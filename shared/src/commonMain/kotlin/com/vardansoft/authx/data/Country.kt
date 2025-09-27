package com.vardansoft.authx.data

import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val name: String,
    val demonym: String
)