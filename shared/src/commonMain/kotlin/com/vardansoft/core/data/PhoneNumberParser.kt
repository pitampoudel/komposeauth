package com.vardansoft.core.data

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

expect fun parsePhoneNumber(countryNameCode: String?, phoneNumber: String): PhoneNumber?
