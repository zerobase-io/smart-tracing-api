package io.zerobase.smarttracing.api.features.organizations

import com.google.inject.Inject
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.api.EntityCreationException
import io.zerobase.smarttracing.api.QueryFailedException
import io.zerobase.smarttracing.api.UpdateFailedException
import io.zerobase.smarttracing.api.gremlin.execute
import io.zerobase.smarttracing.api.gremlin.getIfPresent
import io.zerobase.smarttracing.api.now
import io.zerobase.smarttracing.common.LoggerDelegate
import io.zerobase.smarttracing.common.models.*
import io.zerobase.smarttracing.api.validatePhoneNumber
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__`.unfold
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality.single
import java.util.*

class OrganizationsDao @Inject constructor(private val graph: GraphTraversalSource) {
    companion object {
        private val log by LoggerDelegate()
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
                .property("creationTimestamp", now())
            v.execute()

            return Organization(id, name, address, contactName, ContactInfo(email, phone))
        } catch (ex: Exception) {
            log.error("Error creating organization. name={}", name, ex)
            throw EntityCreationException("Error creating organization", ex)
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
                    id=id.value,
                    name=it["name"]!!,
                    address= Address(
                        it["premise"]!!, it["thoroughfare"]!!,
                        it["locality"]!!, it["administrativeArea"]!!, it["postalCode"]!!, it["country"]!!
                    ),
                    contactName = it["contactName"]!!,
                    contactInfo= ContactInfo(email = it["email"], phoneNumber = it["phoneNumber"])
                )
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
    fun createSite(
        organizationId: OrganizationId, name: String = "Default", category: String, subcategory: String,
        address: Address? = null, lat: Float? = null, long: Float? = null, testing: Boolean = false,
        phone: String? = null, email: String? = null, contactName: String? = null
    ): SiteId {
        val id = UUID.randomUUID().toString()
        try {
            val v = graph.addV("Site")
                .property(T.id, id)
                .property(single,"organizationId", organizationId.value)
                .property(single,"name", name)
                .property(single,"category", category)
                .property(single,"subcategory", subcategory)
                .property(single,"testing", testing)
                .property(single,"creationTimestamp", now())
            address?.also {
                v.property("premise", it.premise)
                .property("thoroughfare", it.thoroughfare)
                .property("locality", it.locality)
                .property("administrativeArea", it.administrativeArea)
                .property("postalCode", it.postalCode)
                .property("country", it.country)
            }
            lat?.also { v.property("latitude", it) }
            long?.also { v.property("longitude", it) }
            contactName?.also { v.property("contactName", it) }
            phone?.also { v.property("phone", it) }
            email?.also { v.property("email", it) }
            v.addE("OWNS").from(graph.V(organizationId.value)).to(graph.V(id)).execute()
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
            .valueMap<String>().with(WithOptions.tokens).by(unfold<String>()).toList()
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

    fun updateEntityName(id: Id, nodeType: String, name: String) {
        try {
            graph.V(id.value).hasLabel(nodeType).property(single, "name", name).execute()
        } catch (ex: Exception) {
            log.error("error updating name of site. id={} label={}", id, nodeType, ex);
            throw UpdateFailedException("Site Name")
        }
    }

    @SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION", justification = "False positive from kotlin")
    fun getScannables(id: SiteId): List<Scannable> {
        try {
            return graph.V(id.value).hasLabel("Site").outE("OWNS").otherV().hasLabel("Scannable")
                .valueMap<String>("name", "type").with(WithOptions.tokens).by(unfold<String>()).toList()
                .map { Scannable(ScannableId(it[T.id]!!), it["name"] ?: "None", it["type"]!!) }
        } catch (ex: Exception) {
            log.error("error executing query. siteId={}", id, ex)
            throw QueryFailedException("Site Scannables")
        }
    }
}
