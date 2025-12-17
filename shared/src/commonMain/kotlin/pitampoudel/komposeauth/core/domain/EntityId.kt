package pitampoudel.komposeauth.core.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias UserId = String
typealias OrgId = String

@Serializable(with = EntityIdSerializer::class)
sealed interface EntityId {
    @SerialName(value = "id")
    val id: String

    @SerialName("user")
    data class User(
        override val id: UserId
    ) : EntityId

    @Serializable
    @SerialName("organization")
    data class Organization(
        override val id: OrgId
    ) : EntityId
}

object EntityIdSerializer : KSerializer<EntityId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EntityId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EntityId) {
        val prefix = when (value) {
            is EntityId.User -> "user-"
            is EntityId.Organization -> "org-"
        }
        encoder.encodeString(prefix + value.id)
    }

    override fun deserialize(decoder: Decoder): EntityId {
        val value = decoder.decodeString()
        return when {
            value.startsWith("user") -> EntityId.User(value.removePrefix("user-"))
            value.startsWith("org") -> EntityId.Organization(value.removePrefix("org-"))
            else -> throw SerializationException("entity id not understandable")
        }
    }
}

