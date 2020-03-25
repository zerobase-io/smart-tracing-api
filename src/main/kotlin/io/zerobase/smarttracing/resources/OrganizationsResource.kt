package io.zerobase.smarttracing.resources

import com.fasterxml.jackson.annotation.JsonProperty
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.GraphDao
import io.zerobase.smarttracing.MultiMap
import io.zerobase.smarttracing.models.OrganizationId
import io.zerobase.smarttracing.models.IdWrapper
import io.zerobase.smarttracing.models.SiteResponse
import io.zerobase.smarttracing.models.ScannableId
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

/**
 * Requests from clients.
 */
data class Contact(
    @JsonProperty("phone") val phone: String,
    @JsonProperty("email") val email: String,
    @JsonProperty("contactName") val contactName: String
)

data class Location(
    @JsonProperty("latitude") val latitude: Float,
    @JsonProperty("longitude") val longitude: Float
)

data class CreateOrganizationRequest(
    @JsonProperty("name") val name: String,
    @JsonProperty("contact") val contact: Contact,
    @JsonProperty("address") val address: String,
    @JsonProperty("hasTestingFacilities") val hasTestingFacilities: Boolean?,
    @JsonProperty("hasMultipleSites") val hasMultipleSites: Boolean?
)

data class CreateSiteRequest(
    @JsonProperty("name") val name: String?,
    @JsonProperty("category") val category: String,
    @JsonProperty("subcategory") val subcategory: String,
    @JsonProperty("location") val location: Location,
    @JsonProperty("isTesting") val isTesting: Boolean,
    @JsonProperty("contact") val contact: Contact?
)

data class CreateScannableRequest(
    @JsonProperty("type") val type: String,
    @JsonProperty("singleUse") val singleUse: Boolean
)

@Path("/organizations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive")
class OrganizationsResource(val dao: GraphDao, private val siteTypes: MultiMap<String, String>, private val scanTypes: List<String>) {

    @POST
    @Creator
    fun createOrganization(request: CreateOrganizationRequest): IdWrapper? {
        val name = request.name
        val phone = request.contact.phone
        val email = request.contact.email
        val contactName = request.contact.contactName
        val address = request.address
        val hasTestingFacilities = request.hasTestingFacilities ?: false
        val hasMultipleSites = request.hasMultipleSites ?: true

        val id = dao.createOrganization(
            name, phone, email,
            contactName, address,
            hasTestingFacilities,
            hasMultipleSites
        )

        return when (id) {
            null -> null
            else -> IdWrapper(id)
        }
    }

    @Path("/{id}/sites")
    @POST
    @Creator
    fun createSite(@PathParam("id") id: String, request: CreateSiteRequest): IdWrapper? {
        val name = request.name ?: ""
        val category = request.category
        val subcategory = request.subcategory
        val latitude = request.location.latitude
        val longitude = request.location.longitude
        val isTesting = request.isTesting
        val isCat = siteTypes.containsKey(category)
        val isSubCat = siteTypes[category]?.contains(subcategory)

        if (!(isCat!!)) {
            return null
        }

        if (!(isSubCat!!)) {
            return null
        }

        val id = if (request.contact == null) {
            dao.createSite(
                id, name, category, subcategory, latitude,
                longitude, isTesting, null, null, null
            )
        } else {
            dao.createSite(
                id, name, category, subcategory, latitude,
                longitude, isTesting, request.contact!!.phone,
                request.contact!!.email, request.contact!!.contactName
            )
        }

        return when (id) {
            null -> null
            else -> IdWrapper(id)
        }
    }

    @Path("/{id}/sites")
    @GET
    fun getSites(@PathParam("id") id: String): List<Any> {
        return dao.getSites(id)!!
    }

    @Path("/{id}/multiple-sites-setting")
    @PUT
    fun updateMultipleSitesSetting(@PathParam("id") id: String, hasMultipleSites: Boolean) {
        dao.setMultiSite(id, hasMultipleSites)
    }

    @Path("/{orgId}/sites/{siteId}/scannables")
    @POST
    @Creator
    fun createScannable(@PathParam("orgId") orgId: String, @PathParam("siteId") siteId: String, request: CreateScannableRequest): IdWrapper? {
        val type = request.type
        val singleUse = request.singleUse

        if (!scanTypes.contains(type)) {
            return null
        }

        val id = dao.createScannable(orgId, siteId, type, singleUse)

        return when (id) {
            null -> null
            else -> IdWrapper(id)
        }
    }
}
