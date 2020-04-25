package io.zerobase.smarttracing

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.zerobase.smarttracing.models.InvalidPhoneNumberException
import java.util.*

private val phoneUtil = PhoneNumberUtil.getInstance()

fun validatePhoneNumber(phone: String?) {
    if (phone == null) {
        return
    }
    try {
        // ZZ as the region code forces E.164
        val parsedPhoneNumber = phoneUtil.parse(phone, "ZZ")
        val validityResult = phoneUtil.isPossibleNumberWithReason(parsedPhoneNumber)
        if (validityResult != PhoneNumberUtil.ValidationResult.IS_POSSIBLE) {
            throw InvalidPhoneNumberException("Unable to validate phone number: $validityResult")
        }
    } catch (ex: NumberParseException) {
        throw InvalidPhoneNumberException("Phone number could not be parsed: ${ex.message}")
    }
}

fun now(): Date = Date()
