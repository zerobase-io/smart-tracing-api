package io.zerobase.smarttracing.common.models

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

enum class Symptom {
    FEVER, BREATHING, NEW_COUGH, SORE_THROAT, ACHING, VOMITING_DIARRHEA, MIGRAINES, LOSS_OF_TASTE
}

enum class HouseholdSize {
    SINGLE, PARTNER,SMALL, MEDIUM, LARGE
}

enum class PublicInteractionScale {
    NONE, SINGLE, PARTNER,SMALL, MEDIUM, LARGE
}

enum class AgeCategory {
    MINOR, GENERAL, ELDERLY
}

enum class TemperatureUnit {
    Celsius, Fahrenheit, Kelvin;
}

data class Temperature (val value: Float, val unit: TemperatureUnit) {
    fun toCelsius(): Float = when (unit) {
        TemperatureUnit.Celsius -> value
        TemperatureUnit.Fahrenheit -> (value - 32)/1.8f
        TemperatureUnit.Kelvin -> value - - 273.15f
    }
}

data class SymptomSummary(
    val reportedBy: Id,
    val testedParty: DeviceId,
    val age: AgeCategory?,
    val symptoms: Set<Symptom>,
    val householdSize: HouseholdSize?,
    val publicInteractionScale: PublicInteractionScale?,
    val temperature: Temperature?,
    val verified: Boolean,
    val timestamp: Instant
)
