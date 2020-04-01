package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.GraphDao
import io.zerobase.smarttracing.MultiMap
import io.zerobase.smarttracing.models.IdWrapper
import io.zerobase.smarttracing.models.Location
import io.zerobase.smarttracing.models.OrganizationId
import io.zerobase.smarttracing.models.SiteId
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Requests from clients.
 */
data class Contact(
    val phone: String,
    val email: String,
    val contactName: String
)

data class CreateOrganizationRequest(
    val name: String,
    val contactInfo: Contact,
    val address: String,
    val hasTestingFacilities: Boolean?,
    val hasMultipleSites: Boolean?
)

data class CreateSiteRequest(
    val name: String?,
    val category: String,
    val subcategory: String,
    val location: Location,
    val isTesting: Boolean,
    val siteManagerContactInfo: Contact?
)

data class CreateScannableRequest(
    val type: String,
    val singleUse: Boolean
)

data class SiteResponse(val id: String, val name: String)

@Path("/organizations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class OrganizationsResource(val dao: GraphDao, private val siteTypes: MultiMap<String, String>, private val scanTypes: List<String>) {

    @POST
    @Creator
    fun createOrganization(request: CreateOrganizationRequest): IdWrapper {
        val name = request.name
        val phone = request.contactInfo.phone
        val email = request.contactInfo.email
        val contactName = request.contactInfo.contactName
        val address = request.address
        val hasTestingFacilities = request.hasTestingFacilities ?: false
        val hasMultipleSites = request.hasMultipleSites ?: true

        return dao.createOrganization(name, phone, email, contactName, address, hasTestingFacilities, hasMultipleSites).let(::IdWrapper)
    }

    @Path("/{id}/sites")
    @POST
    @Creator
    fun createSite(@PathParam("id") organizationId: String, request: CreateSiteRequest): IdWrapper {
        val name = request.name ?: ""
        val category = request.category
        val subcategory = request.subcategory
        val latitude = request.location.latitude
        val longitude = request.location.longitude
        val isTesting = request.isTesting

        if (!siteTypes.containsKey(category)) {
            throw BadRequestException("Not a valid category please check /models/site-types")
        }

        if (siteTypes[category]?.contains(subcategory) != true) {
            throw BadRequestException("Not a valid subcategory please check /models/site-types")
        }

        return dao.createSite(
            OrganizationId(organizationId), name, category, subcategory,
            latitude, longitude, isTesting, request.siteManagerContactInfo?.phone,
            request.siteManagerContactInfo?.email, request.siteManagerContactInfo?.contactName
        ).let(::IdWrapper)
    }

    @Path("/{id}/sites")
    @GET
    fun getSites(@PathParam("id") id: String): List<SiteResponse> {
        val (x, y) = dao.getSites(OrganizationId(id)).unzip()
        return x.zip(y) { a:String, b:String -> SiteResponse(a, b) }
    }

    @Path("/{id}/multiple-sites-setting")
    @PUT
    fun updateMultipleSitesSetting(@PathParam("id") id: String, hasMultipleSites: Boolean) {
        dao.setMultiSite(OrganizationId(id), hasMultipleSites)
    }

    @Path("/{orgId}/sites/{siteId}/scannables")
    @POST
    @Creator
    fun createScannable(@PathParam("orgId") orgId: String, @PathParam("siteId") siteId: String,
                        request: CreateScannableRequest): IdWrapper {
        val type = request.type
        val singleUse = request.singleUse

        if (!scanTypes.contains(type)) {
            val res = Response.status(Response.Status.BAD_REQUEST)
                .entity("Not a valid type please check /models/scannable-types")
                .build()
            throw WebApplicationException(res)
        }

        return dao.createScannable(OrganizationId(orgId), SiteId(siteId), type, singleUse).let(::IdWrapper)
    }
}
