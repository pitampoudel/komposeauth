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
import pitampoudel.komposeauth.storage.repository.StorageMoveRepository
import pitampoudel.komposeauth.storage.repository.StorageStoreRepository
import pitampoudel.komposeauth.storage.service.StorageStores
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Both admin tiers see the Storage page; only a SUPER_ADMIN changes a store or advances a move, since
 * a store holds a cloud credential and a move rewrites every saved address.
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
    private lateinit var storeRepository: StorageStoreRepository

    @Autowired
    private lateinit var moveRepository: StorageMoveRepository

    @Autowired
    private lateinit var stores: StorageStores

    private val someId = ObjectId().toHexString()

    private val newStore = """{"name": "Files", "kind": "S3", "bucket": "security-test-files", "region": "ap-south-1"}"""

    /** Every write the Storage page can make, with a body each would accept. */
    private val writes = listOf(
        Triple(HttpMethod.POST, "/admin/storage/stores", newStore),
        Triple(HttpMethod.PATCH, "/admin/storage/stores/$someId", """{"name": "Renamed"}"""),
        Triple(HttpMethod.POST, "/admin/storage/stores/$someId/test", null),
        Triple(HttpMethod.POST, "/admin/storage/stores/$someId/primary", null),
        Triple(HttpMethod.DELETE, "/admin/storage/stores/$someId", null),
        Triple(HttpMethod.POST, "/admin/storage/moves", """{"fromStoreId": "$someId", "toStoreId": "$someId"}"""),
        Triple(HttpMethod.POST, "/admin/storage/moves/$someId/step", null),
        Triple(HttpMethod.POST, "/admin/storage/moves/$someId/cancel", null)
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
        storeRepository.deleteAll()
        moveRepository.deleteAll()
        stores.forget()
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
    fun `a plain admin reads the stores and the move but can change nothing, nor step a move`() {
        val (_, cookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, "storage-admin@example.com")

        val state = call(HttpMethod.GET, "/admin/storage/state", null, cookie)
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
        assertEquals("false", json.parseToJsonElement(state).jsonObject.getValue("canManage").jsonPrimitive.content)

        writes.forEach { (method, path, body) ->
            call(method, path, body, cookie).andExpect { status { isForbidden() } }
        }
        assertEquals(0, storeRepository.count(), "nothing was written")

        val page = mockMvc.get("/admin/storage") {
            accept = MediaType.TEXT_HTML
            cookie(cookie)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertFalse(page.contains("id=\"newStoreBtn\""), "a plain admin's add would answer 403")
        assertTrue(page.contains("Only a SUPER_ADMIN can add, change or move a store."))
    }

    @Test
    fun `a super admin adds stores and removes one, and the one taking new files can't be removed`() {
        val (_, cookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc, json, userRepository, "storage-super@example.com", role = Roles.SUPER_ADMIN
        )

        val first = call(HttpMethod.POST, "/admin/storage/stores", newStore, cookie)
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString.let { json.parseToJsonElement(it).jsonObject }
        assertEquals("true", first.getValue("primary").jsonPrimitive.content, "the first store takes new files")
        assertEquals("New files go here", first.getValue("role").jsonPrimitive.content)

        val second = call(
            HttpMethod.POST, "/admin/storage/stores",
            """{"name": "R2", "kind": "S3", "bucket": "security-test-r2", "region": "auto",
               "endpoint": "https://acc.r2.cloudflarestorage.com", "accessKeyId": "key", "secretAccessKey": "secret"}""",
            cookie
        ).andExpect { status { isOk() } }
            .andReturn().response.contentAsString.let { json.parseToJsonElement(it).jsonObject }
        assertEquals("Holds older files", second.getValue("role").jsonPrimitive.content)
        assertFalse(second.toString().contains("\"secret\""), "a secret is never sent back")

        call(HttpMethod.POST, "/admin/storage/stores", newStore, cookie).andExpect { status { isConflict() } }
        call(HttpMethod.POST, "/admin/storage/stores", """{"name": "S3", "kind": "S3", "bucket": "no-region"}""", cookie)
            .andExpect { status { isBadRequest() } }

        val firstId = first.getValue("id").jsonPrimitive.content
        val secondId = second.getValue("id").jsonPrimitive.content
        call(HttpMethod.DELETE, "/admin/storage/stores/$firstId", null, cookie).andExpect { status { isConflict() } }
        call(HttpMethod.DELETE, "/admin/storage/stores/$secondId", null, cookie).andExpect { status { isOk() } }
        assertEquals(1, storeRepository.count())

        val page = mockMvc.get("/admin/storage") {
            accept = MediaType.TEXT_HTML
            cookie(cookie)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertTrue(page.contains("id=\"newStoreBtn\""))
        assertTrue(page.contains("const CAN_MANAGE = true"))
    }
}
