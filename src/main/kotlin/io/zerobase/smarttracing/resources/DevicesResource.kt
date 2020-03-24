package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.models.IdWrapper
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

//region Request Models

//endregion

@Path("/devices")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class DevicesResource {

    @POST
    @Creator
    fun createDevice(request: Any): IdWrapper {
        TODO("not implemented")
    }

    @Path("/{id}/check-ins")
    @POST
    @Creator
    fun createCheckIn(@PathParam("id") id: String, request: Any): IdWrapper {
        TODO("not implemented")
    }

    @Path("/{deviceId}/check-ins/{checkInId}/location")
    @PUT
    fun updateCheckInLocation(@PathParam("deviceId") deviceId: String, @PathParam("checkInId") checkInId: String, request: Any) {
        TODO("not implemented")
    }
}
