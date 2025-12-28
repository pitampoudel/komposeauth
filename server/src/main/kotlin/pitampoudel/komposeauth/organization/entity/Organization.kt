package pitampoudel.komposeauth.organization.entity

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import pitampoudel.komposeauth.core.domain.EntityId
import java.time.Instant


@Serializable
@Document(collection = "organizations")
@CompoundIndexes(
    CompoundIndex(
        name = "text_search_idx",
        def = "{'name': 'text', 'email': 'text'}"
    ),
    CompoundIndex(
        name = "users_created_idx",
        def = "{'userIds': 1, 'createdAt': -1}"
    )
)
data class Organization(
    @Contextual
    @Id val id: ObjectId = ObjectId(),
    @Contextual 
    @CreatedDate
    val createdAt: Instant = Instant.now(),
    @Contextual 
    @LastModifiedDate
    val updatedAt: Instant = Instant.now(),
    @field:NotBlank(message = "Organization name is required")
    @Indexed(unique = true) val name: String,
    @field:Email(message = "Invalid email address")
    @Indexed(unique = true) val email: String,
    val emailVerified: Boolean = false,
    val logoUrl: String?,
    val country: String?,
    val state: String?,
    val city: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    @Indexed(unique = true, sparse = true)
    val phoneNumber: String?,
    val phoneNumberVerified: Boolean = false,
    @Indexed(unique = true, sparse = true)
    val registrationNo: String?,
    val description: String?,
    val website: String?,
    val socialLinks: List<String> = listOf(),
    val userIds: List<@Contextual ObjectId>
) {
    fun asEntityId(): EntityId = EntityId.Organization(id = id.toHexString())
}