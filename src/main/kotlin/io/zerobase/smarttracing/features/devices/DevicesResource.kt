package io.zerobase.smarttracing.features.devices

import io.zerobase.smarttracing.models.*
import io.zerobase.smarttracing.resources.Creator
import java.time.Instant
import java.time.LocalDate
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

data class SelfReportedTestResult(
    val testDate: LocalDate,
    val result: Boolean,
    val timestamp: Instant
)

data class SelfReportedSymptoms(
    val timestamp: Instant,
    val symptoms: Set<Symptom>,
    val age: AgeCategory? = null,
    val householdSize: HouseholdSize? = null,
    val publicInteractionEstimate: PublicInteractionScale? = null,
    val temperature: Temperature? = null
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

    @Path("/{id}/reports/tests")
    @POST
    @Creator
    fun selfReportTestResult(@PathParam("id") id: String, report: SelfReportedTestResult): IdWrapper {
        val deviceId = DeviceId(id)
        return dao.recordTestResult(TestResult(
            reportedBy = deviceId,
            testedParty = deviceId,
            result = report.result,
            testDate = report.testDate,
            verified = false,
            timestamp = report.timestamp
        )).let(::IdWrapper)
    }

    @Path("/{id}/reports/symptoms")
    @POST
    @Creator
    fun selfReportSymtoms(@PathParam("id") id: String, report: SelfReportedSymptoms): IdWrapper {
        val deviceId = DeviceId(id)
        return dao.recordSymptoms(SymptomSummary(
            reportedBy = deviceId,
            testedParty = deviceId,
            age = report.age,
            symptoms = report.symptoms,
            householdSize = report.householdSize,
            publicInteractionScale = report.publicInteractionEstimate,
            temperature = report.temperature,
            verified = false,
            timestamp = report.timestamp
        )).let(::IdWrapper)
    }
}
