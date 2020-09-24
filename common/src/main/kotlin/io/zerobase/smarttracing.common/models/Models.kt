package io.zerobase.smarttracing.common.models

import java.time.Instant
import java.time.LocalDate

data class Id(val id: String)

data class User(val id: String, val name: String?, val contactInfo: ContactInfo)

data class Location(val latitude: Float, val longitude: Float)

data class ContactInfo(val email: String?, val phoneNumber: String?)

data class Address(val premise: String, val thoroughfare: String, val locality: String, val administrativeArea: String, val postalCode: String, val country: String)
data class Organization(val id: String, val name: String, val address: Address, val contactName: String, val contactInfo: ContactInfo)

data class Scannable(val id: String, val name: String, val type: String)

data class TestResult(
    val reportedBy: String,
    val testedParty: String,
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
    val reportedBy: String,
    val testedParty: String,
    val age: AgeCategory?,
    val symptoms: Set<Symptom>,
    val householdSize: HouseholdSize?,
    val publicInteractionScale: PublicInteractionScale?,
    val temperature: Temperature?,
    val verified: Boolean,
    val timestamp: Instant
)
