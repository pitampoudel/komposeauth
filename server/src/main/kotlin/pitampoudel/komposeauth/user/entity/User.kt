package pitampoudel.komposeauth.user.entity

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.hibernate.validator.constraints.URL
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
@TypeAlias("user")
@CompoundIndexes(
    CompoundIndex(
        name = "search_fields_idx",
        def = "{'firstName': 1, 'lastName': 1, 'email': 1, 'phoneNumber': 1}"
    ),
    CompoundIndex(
        name = "roles_created_idx",
        def = "{'roles': 1, 'createdAt': -1}"
    ),
    CompoundIndex(
        name = "deactivated_created_idx",
        def = "{'deactivated': 1, 'createdAt': -1}"
    )
)
data class User(
    @Id
    val id: ObjectId,
    @field:NotBlank(message = "First name is required")
    val firstName: String?,
    @field:NotBlank(message = "Last name is required")
    val lastName: String?,
    @field:Email(message = "Invalid email address")
    @field:Indexed(unique = true, partialFilter = $$"{ 'email': { '$type': 'string' } }")
    val email: String?,
    val emailVerified: Boolean = false,
    @field:Indexed(unique = true, partialFilter = $$"{ 'phoneNumber': { '$type': 'string' } }")
    val phoneNumber: String?,
    val phoneNumberVerified: Boolean = false,
    @field:URL(message = "Picture must be a valid URL")
    val picture: String? = null,
    val socialLinks: List<String> = listOf(),
    val passwordHash: String? = null,
    val roles: List<String> = listOf(),
    val deactivated: Boolean = false,
    @CreatedDate
    val createdAt: Instant = Instant.now(),
    @LastModifiedDate
    val updatedAt: Instant = Instant.now()
) {
    fun firstNameOrUser() = firstName ?: "User"

    init {
        require(!email.isNullOrBlank() || !phoneNumber.isNullOrBlank()) {
            "Either email or phone number must be provided"
        }
    }

    fun verifiedEmail() = if (emailVerified) email else null

    val fullName: String
        get() = "$firstName $lastName"

}