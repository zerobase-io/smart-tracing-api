package io.zerobase.smarttracing.api.features.organizations

import com.google.common.collect.Multimap
import com.google.common.eventbus.EventBus
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.api.resources.Creator
import io.zerobase.smarttracing.common.LoggerDelegate
import io.zerobase.smarttracing.common.models.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

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
    val address: Address,
    val hasTestingFacilities: Boolean?,
    val hasMultipleSites: Boolean?
)

data class CreateSiteRequest(
    val name: String?,
    val category: String,
    val subcategory: String,
    val address: Address?,
    val location: Location?,
    val isTesting: Boolean = false,
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
class OrganizationsResource(
    private val dao: OrganizationsDao,
    private val siteTypes: Multimap<String, String>,
    private val scanTypes: Set<String>,
    private val eventBus: EventBus
) {
    companion object {
        val log by LoggerDelegate()
    }

    @POST
    @Creator
    fun createOrganization(request: CreateOrganizationRequest): Id {
        val name = request.name
        val phone = request.contactInfo.phone
        val email = request.contactInfo.email
        val contactName = request.contactInfo.contactName
        val address = request.address
        val hasTestingFacilities = request.hasTestingFacilities ?: false
        val hasMultipleSites = request.hasMultipleSites ?: true

        val organization = dao.createOrganization(name, phone, email, contactName, address, hasTestingFacilities, hasMultipleSites)

        if (!hasTestingFacilities && !hasMultipleSites) {
            log.debug("Simple business organization detected. Auto-creating site and default qr code. organization={}", organization)
            val siteId = dao.createSite(organization.id, category = "BUSINESS", subcategory = "OTHER", address = request.address)
            val scannableId = dao.createScannable(organization.id, siteId, "QR_CODE", false)
            log.info("Created site and default ")
            eventBus.post(SimpleOrganizationCreated(organization, scannableId))
        }

        return Id(organization.id)
    }

    @Path("/{id}/sites")
    @POST
    @Creator
    fun createSite(@PathParam("id") organizationId: String, request: CreateSiteRequest): Id {
        val name = request.name ?: ""
        val category = request.category
        val subcategory = request.subcategory
        val latitude = request.location?.latitude
        val longitude = request.location?.longitude
        val isTesting = request.isTesting

        if (!siteTypes.containsKey(category)) {
            throw BadRequestException("Not a valid category please check /models/site-types")
        }

        if (siteTypes[category]?.contains(subcategory) != true) {
            throw BadRequestException("Not a valid subcategory please check /models/site-types")
        }

        return dao.createSite(
            organizationId, name, category, subcategory, request.address,
            latitude, longitude, isTesting, request.siteManagerContactInfo?.phone,
            request.siteManagerContactInfo?.email, request.siteManagerContactInfo?.contactName
        ).let(::Id)
    }

    @Path("/{id}/sites")
    @GET
    @SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION", justification = "false positive")
    fun getSites(@PathParam("id") id: String): List<SiteResponse> {
        return dao.getSites(id).map { (id, name) -> SiteResponse(id, name) }
    }

    @Path("/{id}/multiple-sites-setting")
    @PUT
    fun updateMultipleSitesSetting(@PathParam("id") id: String, hasMultipleSites: Boolean) {
        dao.setMultiSite(id, hasMultipleSites)
    }

    @Path("/{orgId}/sites/{siteId}")
    fun delegateSiteRequest(@PathParam("orgId") orgId: String, @PathParam("siteId") siteId: String): SitesResource {
        return SitesResource(orgId, siteId, dao, scanTypes)
    }
}
