package com.vardansoft.core.data

import com.google.i18n.phonenumbers.PhoneNumberUtil


actual fun parsePhoneNumber(
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
        null
    }
}