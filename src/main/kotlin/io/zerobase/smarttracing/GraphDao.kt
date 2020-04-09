package io.zerobase.smarttracing

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.models.*
import io.zerobase.smarttracing.utils.LoggerDelegate
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import java.util.*

private fun <S, T> Traversal<S, T>.getIfPresent(): T? {
    return tryNext().orElse(null)
}

private fun <S, T> Traversal<S, T>.execute(): T? {
    return next()
}

class GraphDao(
    private val graph: GraphTraversalSource,
    private val phoneUtil: PhoneNumberUtil
) {
    companion object {
        private val log by LoggerDelegate()
    }

    /**
     * Creates a new Device and returns its ID
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive")
    fun createDevice(fingerprint: Fingerprint?): DeviceId {
        val id = UUID.randomUUID().toString()
        try {
            val vertex = graph.addV("Device")
                    .property(T.id, id)
                    .property("fingerprint", fingerprint?.value ?: "none")
                    .property("creationTimestamp", System.currentTimeMillis())
                    .execute()
            return vertex?.run { DeviceId(id) } ?: throw EntityCreationException("Failed to save device")
        } catch (ex: Exception) {
            log.error("error creating device. fingerprint={}", fingerprint, ex)
            throw EntityCreationException("Error creating device", ex)
        }
    }

    /**
     * Creates a new CheckIn and returns its ID
     */
    fun createCheckIn(deviceId: DeviceId, scannedId: ScannableId, loc: Location?): ScanId {
        val scanId = UUID.randomUUID().toString()
        try {
            val deviceNode = graph.V(deviceId.value).getIfPresent() ?: throw InvalidIdException(deviceId)
            val scannableNode = graph.V(scannedId.value).getIfPresent() ?: throw InvalidIdException(scannedId)
            val edge = graph.addE("SCAN")
                .from(deviceNode)
                .to(scannableNode)
                .property(T.id, scanId)
                .property("timestamp", System.currentTimeMillis())
            loc?.also { (lat, long) -> edge.property("latitude", loc?.latitude).property("longitude", loc?.longitude) }
            edge.getIfPresent()
            return ScanId(scanId)
        } catch (ex: Exception) {
            log.error("error creating check-in. device={} scannable={}", deviceId, scannedId, ex)
            throw EntityCreationException("Error creating check-in", ex)
        }
    }

    /**
     * Updates the location attribute of a CheckIn. Throws 404 if CheckIn doesn't exist.
     */
    fun updateCheckInLocation(deviceId: DeviceId, checkInId: ScanId, loc: Location) {
        graph.V(checkInId.value).inE("SCAN")
                .from(graph.V(deviceId.value))
                .property("latitude", loc.latitude)
                .property("longitude", loc.longitude)
                .execute()
    }

    fun recordPeerToPeerScan(scanner: DeviceId, scanned: DeviceId, loc: Location?): ScanId {
        val scanId = UUID.randomUUID().toString()
        try {
            val aNode = graph.V(scanner.value).getIfPresent() ?: throw InvalidIdException(scanner)
            val bNode = graph.V(scanned.value).getIfPresent() ?: throw InvalidIdException(scanned)
            graph.addE("SCAN")
                    .from(aNode)
                    .to(bNode)
                    .property(T.id, scanId)
                    .property("timestamp", System.currentTimeMillis())
                    .property("latitude", loc?.latitude ?: 0)
                    .property("longitude", loc?.longitude ?: 0)
                    .execute()
            return ScanId(scanId)
        } catch (ex: Exception) {
            log.error("error creating p2p scan. scanner={} scanned={}", scanner, scanned, ex)
            throw EntityCreationException("Error creating scan relationship between devices.", ex)
        }
    }

    /**
     * Creates an organization node inside neo4j.
     *
     * @param organization name of the organization that is being created.
     * @param phone phone number for the contact for the organization.
     * @param email email for the contact for the organization.
     * @param contactName name of the contact for the organization.
     * @param address address of the organization.
     * @param hasTestingFacilities if the organization owns sites that can do testing.
     * @param multiSite reporting if the organization has multiple sites.
     *
     * @return organization id.
     *
     * @throws exception if phone number is invalid.
     */
    fun createOrganization(name: String, phone: String, email: String, contactName: String, address: Address,
                           hasTestingFacilities: Boolean, multiSite: Boolean): Organization {

        validatePhoneNumber(phone)

        val id = UUID.randomUUID().toString()
        try {
            val v = graph.addV("Organization")
                .property(T.id, id)
                .property("name", name)
                .property("premise", address.premise)
                .property("thoroughfare", address.thoroughfare)
                .property("locality", address.locality)
                .property("administrativeArea", address.administrativeArea)
                .property("postalCode", address.postalCode)
                .property("country", address.country)
                .property("contactName", contactName)
                .property("email", email)
                .property("phone", phone)
                .property("verified", false)
                .property("hasTestingFacilities", hasTestingFacilities)
                .property("multisite", multiSite)
                .property("creationTimestamp", System.currentTimeMillis())
            v.execute()

            return Organization(OrganizationId(id), name, address, contactName, ContactInfo(email, phone))
        } catch (ex: Exception) {
            log.error("Error creating organization. name={}", name, ex)
            throw EntityCreationException("Error creating organization", ex)
        }
    }

    /**
     * Sets the multi-site flag in an organization.
     *
     * @param id organization uuid.
     * @param state the value for the multi-site flag
     */
    fun setMultiSite(id: OrganizationId, state: Boolean) {
        graph.V(id.value).property("multisite", state).execute()
    }

    /**
     * Creates site.
     *
     * @param id organization id.
     * @param name name of the site.
     * @param category category of the site.
     * @param subcategory subcategory of the site.
     * @param lat latitude of the site.
     * @param long longitude of the site.
     * @param testing whether the site can preform testing.
     * @param phone contact phone of site manager
     * @param email contact email of site manager
     * @param contactName contact name of site manager
     */
    fun createSite(organizationId: OrganizationId, name: String = "Default", category: String, subcategory: String, lat: Float? = null, long: Float? = null,
                   testing: Boolean = false, phone: String? = null, email: String? = null, contactName: String? = null): SiteId {
        val id = UUID.randomUUID().toString()
        try {
            val v = graph.addV("Site")
                .property(T.id, id)
                .property("name", name)
                .property("category", category)
                .property("subcategory", subcategory)
                .property("testing", testing)
                .property("creationTimestamp", System.currentTimeMillis())
            lat?.also { v.property("latitude", it) }
            long?.also { v.property("longitude", it) }
            contactName?.also { v.property("contactName", it) }
            phone?.also { v.property("phone", it) }
            email?.also { v.property("email", it) }
            v.execute()
            graph.addE("OWNS").from(graph.V(organizationId.value)).to(graph.V(id)).execute()
            return SiteId(id)
        } catch (ex: Exception) {
            log.error("error creating site. organization={} name={} category={}-{} testing={}", id, name, category, subcategory, testing, ex)
            throw EntityCreationException("Error creating site.", ex)
        }
    }

    /**
     * Gets all the sites list
     *
     * @param id id of the organization
     *
     * @return list of all the sites.
     */
    @SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION", justification = "false positive")
    fun getSites(id: OrganizationId): List<Pair<String, String>> {
        return graph.V(id.value).out("OWNS").hasLabel("Site")
            .elementMap<String>().toList()
            .map{ it[T.id]!! to it["name"]!! }
    }

    /*
     * Creates a scannable for a site. A scannable is either QR Code or BT
     * receivers.
     *
     * @param oid organization id
     * @param sid site id
     * @param type type of scannable
     * @param singleUse if it is a single use scannable or not
     *
     * @return id of the scannable.
     */
    fun createScannable(oid: OrganizationId, sid: SiteId, type: String, singleUse: Boolean): ScannableId {
        val id = UUID.randomUUID().toString()
        try {
            graph.addV("Scannable").property(T.id, id).property("type", type).property("singleUse", singleUse)
                .property("active", true).addE("OWNS")
                .from(graph.V(sid.value))
                .execute()
            return ScannableId(id)
        } catch (ex: Exception) {
            log.error("error creating scannable. organization={} site={} type={}", oid, sid, type)
            throw EntityCreationException("Error creating scannable.", ex)
        }
    }

    /**
     * Gets the email for the organization
     *
     * @param oid organization id
     *
     * @return email of the organization.
     */
    fun getOrganization(id: OrganizationId): Organization? {
        return graph.V(id.value)
            .propertyMap<String>()
            .getIfPresent()
            ?.let {
                Organization(
                    id=id,
                    name=it["name"]!!,
                    address=Address(
                        it["premise"]!!, it["thoroughfare"]!!,
                        it["locality"]!!, it["administrativeArea"]!!, it["postalCode"]!!, it["country"]!!
                    ),
                    contactName = it["contactName"]!!,
                    contactInfo=ContactInfo(email = it["email"], phoneNumber = it["phoneNumber"])
                )
            }
    }

    /**
     * Creates a user node and links to device id.
     *
     * @param name name of the user.
     * @param phone phone of the user.
     * @param email email of the user.
     * @param id id of the device used to create it.
     *
     * @returns id of the user.
     */
    fun createUser(name: String?, phone: String?, email: String?, deviceId: DeviceId): UserId {
        phone?.apply { validatePhoneNumber(phone) }
        val id = UUID.randomUUID().toString()
        val deviceVertex = graph.V(deviceId.value).getIfPresent() ?: throw InvalidIdException(deviceId)
        try {
            graph.addV("USER")
                .property(T.id, id).property("name", name).property("phone", phone).property("email", email)
                .property("deleted", false)
                .addE("OWNS").to(deviceVertex)
                .execute()
            return UserId(id)
        } catch (ex: Exception) {
            log.error("error creating user for device. device={}", deviceId, ex)
            throw EntityCreationException("Error creating user.", ex)
        }
    }

    /**
     * "Deletes" the user
     *
     * @param id id of the user to delete
     */
    fun deleteUser(id: UserId) {
        try {
            graph.V(id.value).property("deleted", true).execute()
        } catch (ex: Exception) {
            log.error("failed to delete user. id={}", id, ex)
            throw ex
        }
    }

    /**
     * Gets the user
     *
     * @param id the id of the user
     *
     * @return User struct
     */
    fun getUser(id: UserId): User? {
        try {
            val vertex = graph.V(id.value).has("deleted", false).propertyMap<String>().getIfPresent() ?: return null
            return User(
                id = id,
                name = vertex["name"],
                contactInfo = ContactInfo(phoneNumber = vertex["phone"],  email = vertex["email"])
            )
        } catch (ex: Exception) {
            log.error("error getting user. id={}", ex)
            throw ex
        }
    }

    private fun validatePhoneNumber(phone: String?) {
        if (phone == null) {
            return
        }
        try {
            // ZZ as the region code forces E.164
            val parsedPhoneNumber = phoneUtil.parse(phone, "ZZ")
            val validityResult = phoneUtil.isPossibleNumberWithReason(parsedPhoneNumber)
            if (validityResult != PhoneNumberUtil.ValidationResult.IS_POSSIBLE) {
                throw InvalidPhoneNumberException("Unable to validate phone number: $validityResult")
            }
        } catch (ex: NumberParseException) {
            throw InvalidPhoneNumberException("Phone number could not be parsed: ${ex.message}")
        }
    }
}
