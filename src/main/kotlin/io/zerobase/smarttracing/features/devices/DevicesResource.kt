package io.zerobase.smarttracing.features.devices

import io.zerobase.smarttracing.models.*
import io.zerobase.smarttracing.resources.Creator
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

//region Request Models
enum class ScanType {
    DEVICE_TO_DEVICE,
    DEVICE_TO_SCANNABLE
}

data class CreateDeviceRequest(val fingerprint: String?)

data class CreateCheckInRequest(
    val scannedId: String,
    val type: ScanType,
    val location: Location?
)
//endregion

@Path("/devices")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class DevicesResource(val dao: DevicesDao) {

    @POST
    @Creator
    fun createDevice(request: CreateDeviceRequest): IdWrapper? {
        val fingerprint = request.fingerprint?.let { Fingerprint(it) }

        val newDeviceId = dao.createDevice(fingerprint)

        return IdWrapper(newDeviceId)
    }

    @Path("/{id}/check-ins")
    @POST
    @Creator
    fun createCheckIn(@PathParam("id") deviceId: String, request: CreateCheckInRequest): IdWrapper {
        val scannedId = request.scannedId
        val type = request.type
        val loc = request.location

        val newCheckInId = if (type == ScanType.DEVICE_TO_SCANNABLE) {
            dao.createCheckIn(DeviceId(deviceId), ScannableId(scannedId), loc)
        } else if (type == ScanType.DEVICE_TO_DEVICE) {
            dao.recordPeerToPeerScan(DeviceId(deviceId), DeviceId(scannedId), loc)
        } else {
            throw BadRequestException("Incorrect type")
        }

        return IdWrapper(newCheckInId)
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
