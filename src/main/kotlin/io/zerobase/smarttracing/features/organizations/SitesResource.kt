package io.zerobase.smarttracing.features.organizations

import io.zerobase.smarttracing.models.*
import io.zerobase.smarttracing.resources.Creator
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class SitesResource(
    private val orgId: OrganizationId,
    private val id: SiteId,
    private val dao: OrganizationsDao,
    private val scanTypes: Set<String>
) {

    @Path("/name")
    @PUT
    fun updateName(name: String) {
        dao.updateEntityName(id, "Site", name)
    }

    @Path("/scannables")
    @POST
    @Creator
    fun createScannable(request: CreateScannableRequest): IdWrapper {
        val type = request.type
        val singleUse = request.singleUse

        if (!scanTypes.contains(type)) {
            val res = Response.status(Response.Status.BAD_REQUEST)
                .entity("Not a valid type please check /models/scannable-types")
                .build()
            throw WebApplicationException(res)
        }

        return dao.createScannable(orgId, id, type, singleUse).let(::IdWrapper)
    }

    @Path("/scannables")
    @GET
    fun getScannables(): List<Scannable> {
        return dao.getScannables(id)
    }

    @Path("/scannables/{id}")
    fun delegateScannableRequest(@PathParam("id") scannableId: String): ScannablesResource {
        return ScannablesResource(orgId, id, ScannableId(scannableId), dao)
    }
}
