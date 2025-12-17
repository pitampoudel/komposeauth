package pitampoudel.komposeauth.organization.entity

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import pitampoudel.komposeauth.domain.EntityId
import kotlin.time.Clock
import kotlin.time.Instant


@Serializable
@Document(collection = "organizations")
@CompoundIndex(def = "{'name': 'text', 'email': 'text'}")
data class Organization(
    @Contextual
    @Id val id: ObjectId = ObjectId(),
    @Contextual val createdAt: Instant = Clock.System.now(),
    @Contextual val updatedAt: Instant = Clock.System.now(),
    @Indexed(unique = true) val name: String,
    @Indexed(unique = true) val email: String,
    val emailVerified: Boolean = false,
    val logoUrl: String?,
    val address: String,
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