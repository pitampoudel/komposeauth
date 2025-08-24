package com.vardansoft.authx.core.utils

import com.google.i18n.phonenumbers.PhoneNumberUtil

data class PhoneNumber(
    val nationalNumber: Long,
    val countryNameCode: String?,
    val fullNumberInInternationalFormat: String
)

fun parsePhoneNumber(
    countryNameCode: String?,
    phoneNumber: String
): PhoneNumber? {
    val phoneUtil = PhoneNumberUtil.getInstance()
    return try {
        val num = phoneUtil.parse(phoneNumber, countryNameCode)
        if (phoneUtil.isValidNumber(num)) PhoneNumber(
            nationalNumber = num.nationalNumber,
            countryNameCode = phoneUtil.getRegionCodeForCountryCode(num.countryCode),
            fullNumberInInternationalFormat = phoneUtil.format(
                num, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
            )
        )
        else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}