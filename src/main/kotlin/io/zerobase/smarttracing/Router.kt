package io.zerobase.smarttracing

import com.fasterxml.jackson.annotation.JsonProperty
import io.zerobase.smarttracing.models.DeviceId
import io.zerobase.smarttracing.models.Fingerprint
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

open class ApiResponse(val success: Boolean,
                       val session: Boolean = false,
                       val message: String? = null
)
data class DeviceCreatedResponse(@JsonProperty("dvid") val id: String): ApiResponse(success = true)
data class ScanRecordedResponse(@JsonProperty("scan") val id: String): ApiResponse(success = true)

data class LegacyCreateDeviceRequest(val fingerprint: String?, val ip: String?)
data class PeerToPeerCheckIn(@JsonProperty("dvid") val scanningDevice: String, @JsonProperty("sdvid") val scannedDevice: String)

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class Router(val dao: GraphDao) {

    @Path("/c/{unused: .+?}")
    @POST
    fun createDevice(req: LegacyCreateDeviceRequest): ApiResponse {
        try {
            return dao.createDevice(req.fingerprint?.let(::Fingerprint)).let { DeviceCreatedResponse(it.value) }
        } catch (_: Exception) {
            return ApiResponse(success = false, message = "Sorry, there was an error processing your account, please try again.")
        }
    }

    @Path("/s-id/{deviceId}")
    @POST
    fun recordCheckIn(req: PeerToPeerCheckIn): ApiResponse {
        val id = dao.recordPeerToPeerScan(DeviceId(req.scanningDevice), DeviceId(req.scannedDevice), null)
        return when (id) {
            null -> ApiResponse(success = false, message = "At least one provided ID is not valid.")
            else -> ScanRecordedResponse(id.value)
        }
    }
}
