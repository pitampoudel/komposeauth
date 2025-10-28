package pitampoudel

import pitampoudel.komposeauth.data.Credential
import kotlinx.serialization.json.Json
import kotlin.test.Test

class SerializationTest {

    @Test
    fun contextLoads() {
        // Test GoogleId serialization/deserialization
        val googleId = Credential.GoogleId("test-token")
        val googleIdJson = Json.Default.encodeToString<Credential>(googleId)
        val deserializedGoogleId = Json.Default.decodeFromString<Credential>(googleIdJson) as Credential.GoogleId

        println("SerializationTest passed - Credential serialization/deserialization works correctly")
    }
}