package io.zerobase.smarttracing.resources

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.GraphDao
import io.zerobase.smarttracing.models.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

//region Request Models
data class CreateDeviceRequest(val fingerprint: String)

data class CreateCheckInRequest(
        val scannedId: String,
        val type: String,
        val location: Location?
)

//endregion

@Path("/devices")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class DevicesResource(val dao: GraphDao) {

    @POST
    @Creator
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive")
    fun createDevice(request: CreateDeviceRequest): IdWrapper? {
        val fingerprint = Fingerprint(request.fingerprint)

        val newDeviceId = dao.createDevice(fingerprint, null)

        return newDeviceId?.let { IdWrapper(newDeviceId) }
    }

    @Path("/{id}/check-ins")
    @POST
    @Creator
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive")
    fun createCheckIn(@PathParam("id") deviceId: String, request: CreateCheckInRequest): IdWrapper? {
        val scannedId = request.scannedId
        val type = request.type
        val loc = request.location

        val newCheckInId = if (type == "DEVICE_TO_SCANNABLE") {
            dao.createCheckIn(DeviceId(deviceId), ScannableId(scannedId), loc)
        } else if (type == "DEVICE_TO_DEVICE") {
            dao.recordPeerToPeerScan(DeviceId(deviceId), DeviceId(scannedId), loc)
        } else {
            throw BadRequestException("Incorrect type")
        }

        return newCheckInId?.let { IdWrapper(newCheckInId) }
    }

    @Path("/{deviceId}/check-ins/{checkInId}/location")
    @PUT
    fun updateCheckInLocation(
            @PathParam("deviceId") deviceId: String,
            @PathParam("checkInId") checkInId: String,
            loc: Location
    ) {
        dao.updateCheckInLocation(DeviceId(deviceId), ScanId(checkInId), loc)
    }
}
