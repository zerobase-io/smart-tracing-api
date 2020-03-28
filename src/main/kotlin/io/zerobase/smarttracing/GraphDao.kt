package io.zerobase.smarttracing

import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.zerobase.smarttracing.models.*
import io.zerobase.smarttracing.utils.LoggerDelegate
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import java.util.*


class GraphDao(private val graph: GraphTraversalSource, private val phoneUtil: PhoneNumberUtil) {
    companion object {
        private val log by LoggerDelegate()
    }

    /**
     * Creates a new Device and returns its ID
     */
    fun createDevice(fingerprint: Fingerprint?, ip: String?): DeviceId {
        val id = UUID.randomUUID().toString()
        try {
            val vertex = graph.addV("Device")
                    .property(T.id, id)
                    .property("fingerprint", fingerprint?.value ?: "none")
                    .next()
            return vertex?.run { DeviceId(id) } ?: throw EntityCreationException("Failed to save device")
        } catch (ex: Exception) {
            log.error("error creating device. fingerprint={}", fingerprint, ex)
            throw EntityCreationException("Error creating device", ex)
        }
    }

    /**
     * Creates a new CheckIn and returns its ID
     */
    @Deprecated(message = "Legacy API endpoint", replaceWith = ReplaceWith("recordPeerToPerScan"))
    fun createCheckIn(deviceId: DeviceId, scannedId: ScannableId, loc: Location?): ScanId {
        val scanId = UUID.randomUUID().toString()
        try {
            graph.addE("SCAN")
                    .from(graph.V(deviceId.value))
                    .to(graph.V(scannedId.value))
                    .property(T.id, scanId)
                    .property("timestamp", System.currentTimeMillis())
                    .property("latitude", loc?.latitude)
                    .property("longitude", loc?.longitude)
                    .next()
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
        graph.V(T.id, checkInId.value).inE("SCAN")
                .from(graph.V(T.id, deviceId.value))
                .property("latitude", loc.latitude)
                .property("longitude", loc.longitude)
                .next()
    }

    fun recordPeerToPeerScan(scanner: DeviceId, scanned: DeviceId, loc: Location?): ScanId? {
        val scanId = UUID.randomUUID().toString()
        try {
            graph.addE("SCAN")
                    .from(graph.V(scanner.value))
                    .to(graph.V(scanned.value))
                    .property(T.id, scanId)
                    .property("timestamp", System.currentTimeMillis())
                    .property("latitude", loc?.latitude)
                    .property("longitude", loc?.longitude)
                    .next()
            return ScanId(scanId)
        } catch (ex: Exception) {
            log.error("error creating p2p scan. scanner={} scanned={}", scanner, scanned, ex)
            throw EntityCreationException("Error creating scan relationship between devices.", ex)
        }
    }

    fun recordDeviceCheckIn(device: DeviceId, site: SiteId): ScanId? {
        val id = UUID.randomUUID().toString()
        try {
            graph.addE("SCAN")
                    .from(graph.V(T.id, device.value))
                    .to(graph.V(T.id, site.value))
                    .property(T.id, id)
                    .property("timestamp", System.currentTimeMillis())
                    .next()
        } catch (ex: Exception) {
            log.error("error creating site scan. device={} site={}", device, site, ex)
            throw EntityCreationException("Error creating scan between device and site.", ex);
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
    fun createOrganization(name: String, phone: String, email: String, contactName: String, address: String,
                           hasTestingFacilities: Boolean, multiSite: Boolean): OrganizationId {

        // ZZ as the region code forces E.164
        if (!phoneUtil.isValidNumber(phoneUtil.parseAndKeepRawInput(phone, "ZZ"))) {
            throw InvalidPhoneNumberException("Invalid phone number")
        }

        val id = UUID.randomUUID().toString()
        try {
            graph.addV("Organization")
                .property(T.id, id)
                .property("name", name)
                .property("address", address)
                .property("contactName", contactName)
                .property("phone", phone)
                .property("email", email)
                .property("verified", false)
                .property("hasTestingFacilities", hasTestingFacilities)
                .property("multisite", multiSite)
                .next()
            return OrganizationId(id)
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
        graph.V(T.id, id.value).property("multisite", state).next()
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
    fun createSite(id: OrganizationId, name: String, category: String, subcategory: String, lat: Float, long: Float,
                   testing: Boolean, phone: String?, email: String?, contactName: String?): SiteId {
        val id = UUID.randomUUID().toString()
        try {
            graph.addV("Site")
                .property(T.id, id)
                .property("latitude", lat)
                .property("longitude", long)
                .property("")
        }
//        val query = """
//            MATCH (o:Organization { id: '${id.value}' })
//            CREATE (s:Site {
//                        id: '${UUID.randomUUID()}',
//                        phone: '${phone}',
//                        email: '${email}',
//                        contactName: '${contactName}',
//                        name: '${name}',
//                        category: '${category}',
//                        subcategory: '${subcategory}',
//                        latitude: '${lat}',
//                        longitude: '${long}',
//                        testing: '${testing}'
//            })
//            CREATE (o)-[r:OWNS]->(s)
//            RETURN s.id as id
//            """.trimIndent()
//        return driver.session().use {
//            it.writeTransaction { txn ->
//                val result = txn.run(query).single()["id"].asString()
//                return@writeTransaction result?.let { SiteId(it) }
//            }
//        }
    }
//
//    /**
//     * Gets all the sites list
//     *
//     * @param id id of the organization
//     *
//     * @return list of all the sites.
//     */
    fun getSites(id: OrganizationId): List<Pair<String, String>> {
        return listOf()
//        return driver.session().use {
//            it.writeTransaction { txn ->
//                val result = txn.run(
//                        """
//                        MATCH (o:Organization { id: '${id.value}' }) - [:OWNS] -> (s:Site)
//                        RETURN s.id AS id, s.name AS name
//                        """.trimIndent()
//                ).list { x -> Pair(x["id"].asString(), x["name"].asString()) }
//                return@writeTransaction result?.let { it }
//            }
//        }!!
    }
//
//    /**
//     * Creates a scannable for a site. A scannable is either QR Code or BT
//     * receivers.
//     *
//     * @param oid organization id
//     * @param sid site id
//     * @param type type of scannable
//     * @param singleUse if it is a single use scannable or not
//     *
//     * @return id of the scannable.
//     */
    fun createScannable(oid: OrganizationId, sid: SiteId, type: String, singleUse: Boolean): ScannableId? {
    return null
//        return driver.session().use {
//            it.writeTransaction { txn ->
//                val result = txn.run(
//                        """
//                        MATCH (o:Organization { id: '${oid.value}' }) - [:OWNS] -> (s:Site { id: '${sid.value}' })
//                        CREATE (scan:Scannable {
//                                    id: '${UUID.randomUUID()}',
//                                    type: '${type}',
//                                    singleUse: '${singleUse}',
//                                    active: 'true'
//                        })
//                        CREATE (s)-[r:OWNS]->(scan)
//                        RETURN scan.id as id
//                        """.trimIndent()
//                ).single()["id"].asString()
//                return@writeTransaction result?.let { ScannableId(it) }
//            }
//        }
    }
//
//    /**
//     * Creates a user node and links to device id.
//     *
//     * @param name name of the user.
//     * @param phone phone of the user.
//     * @param email email of the user.
//     * @param id id of the device used to create it.
//     *
//     * @returns id of the user.
//     */
    fun createUser(name: String?, phone: String?, email: String?, id: DeviceId): UserId? {
        return null
//        if (phone?.let { !phoneUtil.isValidNumber(phoneUtil.parseAndKeepRawInput(phone, "US")) } ?: true) {
//            throw InvalidPhoneNumberException("Invalid phone number")
//        }
//
//        // Can cause issues as it throws an exception when device id was used
//        // to create another device
//        return driver.session().use {
//            it.writeTransaction { txn ->
//                val result = txn.run(
//                        """
//                            MATCH (d:Device { id: '${id.value}' })
//                            WHERE NOT () - [:OWNS] -> (d)
//                            CREATE (u: User {
//                                        id: '${UUID.randomUUID()}',
//                                        name: '${name}',
//                                        phone: '${phone}',
//                                        email: '${email}',
//                                        deleted: 'false'
//                            }) - [r:OWNS] -> (d)
//                            RETURN u.id as id
//                        """.trimIndent()
//                ).single()["id"].asString()
//                return@writeTransaction result?.let { UserId(it) }
//            }
//        }
    }
//
//    /**
//     * "Deletes" the user
//     *
//     * @param id id of the user to delete
//     */
    fun deleteUser(id: UserId) {
//        driver.session().use {
//            it.writeTransaction { txn ->
//                txn.run(
//                        """
//                        MATCH (u:User { id: '${id.value}' })
//                        SET u.deleted = 'true'
//                        """
//                )
//            }
//        }
    }
//
//    /**
//     * Gets the user
//     *
//     * @param id the id of the user
//     *
//     * @return User struct
//     */
    fun getUser(id: UserId): User? {
        return null
//        return driver.session().use {
//            it.writeTransaction { txn ->
//                val result = txn.run(
//                        """
//                        MATCH (u:User { id: '${id.value}', deleted: 'false' })
//                        RETURN u.name AS name, u.phone AS phone, u.email AS email, u.id AS id
//                        """.trimIndent()
//                ).single()
//                return@writeTransaction result?.let { User(it["name"].asString(), it["phone"].asString(), it["email"].asString(), it["id"].asString()) }
//            }
//        }!!
    }
}
