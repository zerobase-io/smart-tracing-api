package io.zerobase.smarttracing.models

import com.fasterxml.jackson.annotation.JsonValue
import java.time.Instant
import java.time.LocalDate

interface Id {
    val value: String
}

inline class Fingerprint(val value: String)
inline class DeviceId(@JsonValue override val value: String): Id
inline class SiteId(@JsonValue override val value: String): Id
inline class ScanId(@JsonValue override val value: String): Id
inline class OrganizationId(@JsonValue override val value: String): Id
inline class UserId(@JsonValue override val value: String): Id
inline class ScannableId(@JsonValue override val value: String): Id
inline class ReportId(@JsonValue override val value: String): Id

data class IdWrapper(val id: Id)

data class User(val id: UserId, val name: String?, val contactInfo: ContactInfo)

data class Location(val latitude: Float, val longitude: Float)

data class ContactInfo(val email: String?, val phoneNumber: String?)

data class Address(val premise: String, val thoroughfare: String, val locality: String, val administrativeArea: String, val postalCode: String, val country: String)
data class Organization(val id: OrganizationId, val name: String, val address: Address, val contactName: String, val contactInfo: ContactInfo)

data class Scannable(val id: ScannableId, val name: String, val type: String)

data class TestResult(
    val reportedBy: Id,
    val testedParty: DeviceId,
    val verified: Boolean,
    val testDate: LocalDate,
    val result: Boolean,
    val timestamp: Instant
)
