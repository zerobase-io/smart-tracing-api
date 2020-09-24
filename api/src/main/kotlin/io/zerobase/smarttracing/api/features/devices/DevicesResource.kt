package io.zerobase.smarttracing.api.features.devices

import io.zerobase.smarttracing.common.models.*
import io.zerobase.smarttracing.api.resources.Creator
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
    fun createDevice(request: CreateDeviceRequest): Id? {
        val newDeviceId = dao.createDevice(request.fingerprint)

        return Id(newDeviceId)
    }

    @Path("/{id}/check-ins")
    @POST
    @Creator
    fun createCheckIn(@PathParam("id") deviceId: String, request: CreateCheckInRequest): Id {
        val scannedId = request.scannedId
        val type = request.type
        val loc = request.location

        val newCheckInId = if (type == ScanType.DEVICE_TO_SCANNABLE) {
            dao.createCheckIn(deviceId, scannedId, loc)
        } else if (type == ScanType.DEVICE_TO_DEVICE) {
            dao.recordPeerToPeerScan(deviceId, scannedId, loc)
        } else {
            throw BadRequestException("Incorrect type")
        }

        return Id(newCheckInId)
    }

    @Path("/{deviceId}/check-ins/{checkInId}/location")
    @PUT
    fun updateCheckInLocation(
            @PathParam("deviceId") deviceId: String,
            @PathParam("checkInId") checkInId: String,
            loc: Location
    ) {
        dao.updateCheckInLocation(deviceId, checkInId, loc)
    }

    @Path("/{id}/reports/tests")
    @POST
    @Creator
    fun selfReportTestResult(@PathParam("id") id: String, report: SelfReportedTestResult): Id {
        val deviceId = id
        return dao.recordTestResult(TestResult(
            reportedBy = deviceId,
            testedParty = deviceId,
            result = report.result,
            testDate = report.testDate,
            verified = false,
            timestamp = report.timestamp
        )).let(::Id)
    }

    @Path("/{id}/reports/symptoms")
    @POST
    @Creator
    fun selfReportSymtoms(@PathParam("id") id: String, report: SelfReportedSymptoms): Id {
        val deviceId = id
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
        )).let(::Id)
    }
}
