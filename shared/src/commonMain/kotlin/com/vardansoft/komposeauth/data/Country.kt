package com.vardansoft.komposeauth.data

import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val name: String,
    val demonym: String
)