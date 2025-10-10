package com.vardansoft.core.data

import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.metadata.defaultMetadataLoader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhoneNumber(
    @SerialName("nationalNumber")
    val nationalNumber: Long,
    @SerialName("countryNameCode")
    val countryNameCode: String?,
    @SerialName("fullNumberInInternationalFormat")
    val fullNumberInInternationalFormat: String
)

fun parsePhoneNumber(countryNameCode: String?, phoneNumber: String): PhoneNumber? {
    val phoneUtil = PhoneNumberUtil.createInstance(metadataLoader = defaultMetadataLoader())
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