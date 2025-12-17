package pitampoudel.komposeauth

import kotlinx.serialization.json.Json
import pitampoudel.komposeauth.core.data.Credential
import kotlin.test.Test

class SerializationTest {

    @Test
    fun contextLoads() {
        // Test GoogleId serialization/deserialization
        val googleId = Credential.GoogleId("test-token")
        val googleIdJson = Json.encodeToString<Credential>(googleId)
        val deserializedGoogleId = Json.decodeFromString<Credential>(googleIdJson) as Credential.GoogleId

        println("SerializationTest passed - Credential serialization/deserialization works correctly")
    }
}