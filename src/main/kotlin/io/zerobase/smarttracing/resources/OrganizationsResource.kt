package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.models.IdWrapper
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

//region Request Models

//endregion

@Path("/organizations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class OrganizationsResource {

    @POST
    @Creator
    fun createOrganization(request: Any): IdWrapper {
        TODO("not implemented")
    }

    @Path("/{id}/sites")
    @POST
    @Creator
    fun createSite(@PathParam("id") id: String, request: Any): IdWrapper {
        TODO("not implemented")
    }

    @Path("/{id}/sites")
    @GET
    fun getSites(@PathParam("id") id: String): List<Any> {
        TODO("not implemented")
    }

    @Path("/{id}/multiple-sites-setting")
    @PUT
    fun updateMultipleSitesSetting(@PathParam("id") id: String, hasMultipleSites: Boolean) {
        TODO("not implemented")
    }

    @Path("/{orgId}/sites/{siteId}/scannables")
    @POST
    @Creator
    fun createScannable(@PathParam("orgId") orgId: String, @PathParam("siteId") siteId: String, request: Any): IdWrapper {
        TODO("not implemented")
    }
}
