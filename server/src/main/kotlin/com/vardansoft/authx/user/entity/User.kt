package com.vardansoft.authx.user.entity

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.hibernate.validator.constraints.URL
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.Instant

@Document(collection = "users")
@TypeAlias("user")
data class User(
    @Id
    val id: ObjectId,
    @field:NotBlank(message = "First name is required")
    val firstName: String,
    @field:NotBlank(message = "Last name is required")
    val lastName: String?,
    @field:Email(message = "Invalid email address")
    @field:Indexed(unique = true, partialFilter = "{ 'email': { '\$exists': true, '\$ne': null } }")
    val email: String?,
    val emailVerified: Boolean = false,
    @field:Indexed(unique = true, partialFilter = "{ 'phoneNumber': { '\$exists': true, '\$ne': null } }")
    val phoneNumber: String?,
    val phoneNumberVerified: Boolean = false,
    @field:URL(message = "Picture must be a valid URL")
    val picture: String? = null,
    val socialLinks: List<String> = listOf(),
    val passwordHash: String? = null,
    @CreatedDate
    val createdAt: Instant = Instant.now(),
    @LastModifiedDate
    val updatedAt: Instant = Instant.now()
) {
    fun asAuthToken() = UsernamePasswordAuthenticationToken(
        id.toHexString(),
        null,
        listOf(
            SimpleGrantedAuthority(roleAuthority()),
        )
    )

    // TODO remove hardcode
    fun roleAuthority() = "ROLE_${if (email == "pitampoudelsaipu@gmail.com") "ADMIN" else "USER"}"
    val fullName: String
        get() = "$firstName $lastName"

}