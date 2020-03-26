package io.zerobase.smarttracing.resources

import com.fasterxml.jackson.annotation.JsonProperty
import io.zerobase.smarttracing.GraphDao
import io.zerobase.smarttracing.models.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

//region Request Models
data class CreateDeviceRequest(@JsonProperty("fingerprint") val fingerprint: Fingerprint)

data class CreateCheckInRequest(
        @JsonProperty("scannedId") val scannedId: ScannableId,
        @JsonProperty("type") val type: ScanType,
        @JsonProperty("location") val location: Location?
) {
    enum class ScanType {
        DEVICE_TO_DEVICE,
        DEVICE_TO_SCANNABLE
    }
}

data class UpdateCheckInLocationRequest(
        @JsonProperty("lat") val lat: Float,
        @JsonProperty("long") val long: Float
)

//endregion

@Path("/devices")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class DevicesResource(val dao: GraphDao) {

    @POST
    @Creator
    fun createDevice(request: CreateDeviceRequest): IdWrapper? {
        val fingerprint = request.fingerprint

        val newDeviceId = dao.createDevice(fingerprint, null)

        return newDeviceId?.let { IdWrapper(newDeviceId) }
    }

    @Path("/{deviceId}/check-ins")
    @POST
    @Creator
    fun createCheckIn(@PathParam("deviceId") deviceId: String, request: CreateCheckInRequest): IdWrapper? {
        val scannedId = request.scannedId
        val type = request.type
        val location = request.location

        val newCheckInId = dao.createCheckIn(scannedId, type, location)

        return newCheckInId?.let { IdWrapper(newCheckInId) }
    }

    @Path("/{deviceId}/check-ins/{checkInId}/location")
    @PUT
    fun updateCheckInLocation(
            @PathParam("deviceId") deviceId: DeviceId,
            @PathParam("checkInId") checkInId: CheckInId,
            request: UpdateCheckInLocationRequest
    ) {
        val lat = request.lat
        val long = request.long

        dao.updateCheckInLocation(deviceId, checkInId, lat, long)
    }
}
