package pitampoudel.komposeauth.storage

import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.storage.repository.StorageBucketRepository
import pitampoudel.komposeauth.storage.service.StorageBuckets
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Both admin tiers see the Storage page; only a SUPER_ADMIN changes the list, since a bucket holds a
 * cloud credential and the list decides where every file is written and read.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class StorageControllerSecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var bucketRepository: StorageBucketRepository

    @Autowired
    private lateinit var buckets: StorageBuckets

    private val someId = ObjectId().toHexString()

    private val newBucket = """{"name": "Files", "kind": "S3", "bucketName": "security-test-files", "region": "ap-south-1"}"""

    /** Every write the Storage page can make, with a body each would accept. */
    private val writes = listOf(
        Triple(HttpMethod.POST, "/admin/storage/buckets", newBucket),
        Triple(HttpMethod.PATCH, "/admin/storage/buckets/$someId", """{"name": "Renamed"}"""),
        Triple(HttpMethod.POST, "/admin/storage/buckets/$someId/test", null),
        Triple(HttpMethod.POST, "/admin/storage/buckets/$someId/primary", null),
        Triple(HttpMethod.PUT, "/admin/storage/buckets/$someId/read-from", """{"bucketId": "$someId"}"""),
        Triple(HttpMethod.DELETE, "/admin/storage/buckets/$someId/read-from", null),
        Triple(HttpMethod.DELETE, "/admin/storage/buckets/$someId", null)
    )

    private fun call(method: HttpMethod, path: String, body: String?, cookie: Cookie?): ResultActionsDsl =
        mockMvc.request(method, path) {
            accept = MediaType.APPLICATION_JSON
            cookie?.let { cookie(it) }
            body?.let {
                contentType = MediaType.APPLICATION_JSON
                content = it
            }
        }

    @BeforeEach
    @AfterEach
    fun clean() {
        bucketRepository.deleteAll()
        buckets.forget()
    }

    @Test
    fun `an anonymous caller is turned away from every storage endpoint`() {
        call(HttpMethod.GET, "/admin/storage/state", null, null).andExpect { status { isUnauthorized() } }
        writes.forEach { (method, path, body) ->
            call(method, path, body, null).andExpect { status { isUnauthorized() } }
        }
    }

    @Test
    fun `a signed-in user who is not an admin sees none of it`() {
        TestAuthHelpers.createUser(mockMvc, json, "storage-user@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "storage-user@example.com")

        call(HttpMethod.GET, "/admin/storage/state", null, cookie).andExpect { status { isForbidden() } }
        mockMvc.get("/admin/storage") {
            accept = MediaType.TEXT_HTML
            cookie(cookie)
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `a plain admin reads the buckets but can change nothing`() {
        val (_, cookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, "storage-admin@example.com")

        val state = call(HttpMethod.GET, "/admin/storage/state", null, cookie)
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
        assertEquals("false", json.parseToJsonElement(state).jsonObject.getValue("canManage").jsonPrimitive.content)

        writes.forEach { (method, path, body) ->
            call(method, path, body, cookie).andExpect { status { isForbidden() } }
        }
        assertEquals(0, bucketRepository.count(), "nothing was written")

        val page = mockMvc.get("/admin/storage") {
            accept = MediaType.TEXT_HTML
            cookie(cookie)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertFalse(page.contains("id=\"newBucketBtn\""), "a plain admin's add would answer 403")
        assertTrue(page.contains("Only a SUPER_ADMIN can add or change a bucket."))
    }

    @Test
    fun `a super admin adds buckets and removes one, and the one taking new files can't be removed`() {
        val (_, cookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc, json, userRepository, "storage-super@example.com", role = Roles.SUPER_ADMIN
        )

        val first = call(HttpMethod.POST, "/admin/storage/buckets", newBucket, cookie)
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString.let { json.parseToJsonElement(it).jsonObject }
        assertEquals("true", first.getValue("primary").jsonPrimitive.content, "the first bucket takes new files")
        assertEquals("New files go here", first.getValue("role").jsonPrimitive.content)

        val second = call(
            HttpMethod.POST, "/admin/storage/buckets",
            """{"name": "R2", "kind": "S3", "bucketName": "security-test-r2", "region": "auto",
               "endpoint": "https://acc.r2.cloudflarestorage.com", "accessKeyId": "key", "secretAccessKey": "secret"}""",
            cookie
        ).andExpect { status { isOk() } }
            .andReturn().response.contentAsString.let { json.parseToJsonElement(it).jsonObject }
        assertEquals("Holds older files", second.getValue("role").jsonPrimitive.content)
        assertFalse(second.toString().contains("\"secret\""), "a secret is never sent back")

        call(HttpMethod.POST, "/admin/storage/buckets", newBucket, cookie).andExpect { status { isConflict() } }
        call(HttpMethod.POST, "/admin/storage/buckets", """{"name": "S3", "kind": "S3", "bucketName": "no-region"}""", cookie)
            .andExpect { status { isBadRequest() } }

        val firstId = first.getValue("id").jsonPrimitive.content
        val secondId = second.getValue("id").jsonPrimitive.content
        call(HttpMethod.DELETE, "/admin/storage/buckets/$firstId", null, cookie).andExpect { status { isConflict() } }
        call(HttpMethod.DELETE, "/admin/storage/buckets/$secondId", null, cookie).andExpect { status { isOk() } }
        assertEquals(1, bucketRepository.count())

        val page = mockMvc.get("/admin/storage") {
            accept = MediaType.TEXT_HTML
            cookie(cookie)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertTrue(page.contains("id=\"newBucketBtn\""))
        assertTrue(page.contains("const CAN_MANAGE = true"))
    }

    @Test
    fun `once an admin has copied a bucket, its files open from the copy until they say otherwise`() {
        val (_, cookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc, json, userRepository, "storage-copy@example.com", role = Roles.SUPER_ADMIN
        )
        fun add(body: String) = call(HttpMethod.POST, "/admin/storage/buckets", body, cookie)
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString.let { json.parseToJsonElement(it).jsonObject.getValue("id").jsonPrimitive.content }
        val old = add(newBucket)
        val copy = add("""{"name": "R2", "kind": "S3", "bucketName": "security-test-copy", "region": "auto"}""")
        val third = add("""{"name": "Third", "kind": "S3", "bucketName": "security-test-third", "region": "auto"}""")

        // New files still go to the old bucket, and a file written there after the copy would be missed.
        call(HttpMethod.PUT, "/admin/storage/buckets/$old/read-from", """{"bucketId": "$copy"}""", cookie)
            .andExpect { status { isConflict() } }
        call(HttpMethod.POST, "/admin/storage/buckets/$copy/primary", null, cookie).andExpect { status { isOk() } }
        val state = call(HttpMethod.PUT, "/admin/storage/buckets/$old/read-from", """{"bucketId": "$copy"}""", cookie)
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
        val oldRow = json.parseToJsonElement(state).jsonObject.getValue("buckets").toString()
        assertTrue(oldRow.contains("Copied to R2"), oldRow)

        // The old row is how its saved addresses are recognised, and new files can't go where links open elsewhere.
        call(HttpMethod.DELETE, "/admin/storage/buckets/$old", null, cookie).andExpect { status { isConflict() } }
        call(HttpMethod.DELETE, "/admin/storage/buckets/$copy", null, cookie).andExpect { status { isConflict() } }
        call(HttpMethod.POST, "/admin/storage/buckets/$old/primary", null, cookie).andExpect { status { isConflict() } }
        // Only a bucket that holds its own files is a copy.
        call(HttpMethod.PUT, "/admin/storage/buckets/$third/read-from", """{"bucketId": "$old"}""", cookie)
            .andExpect { status { isConflict() } }

        call(HttpMethod.DELETE, "/admin/storage/buckets/$old/read-from", null, cookie).andExpect { status { isOk() } }
        call(HttpMethod.POST, "/admin/storage/buckets/$old/primary", null, cookie).andExpect { status { isOk() } }
        call(HttpMethod.DELETE, "/admin/storage/buckets/$third", null, cookie).andExpect { status { isOk() } }
    }
}
