package pitampoudel.komposeauth

import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.types.ObjectId
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.core.data.CreateUserRequest
import pitampoudel.komposeauth.core.data.Credential
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertNotNull

/**
 * Shared helpers for integration tests that need to create a user and log in to obtain the access token cookie.
 * Keeping this in one file avoids copy/paste drift across integration tests.
 */
object TestAuthHelpers {

    /**
     * Creates a user via the public API and returns the created user id.
     *
     * Supports both response shapes used across the codebase:
     * - JSON object: {"id": "..."}
     * - JSON string: "..."
     */
    fun createUser(mockMvc: MockMvc, json: Json, email: String, password: String = "Password1"): String {
        val mvcResult = mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                CreateUserRequest(
                    firstName = "Test",
                    lastName = "User",
                    email = email,
                    password = password,
                    confirmPassword = password
                )
            )
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val body = mvcResult.response.contentAsString
        val id: String? = when (val element = json.parseToJsonElement(body)) {
            is kotlinx.serialization.json.JsonObject -> element.jsonObject["id"]?.jsonPrimitive?.content
            is kotlinx.serialization.json.JsonPrimitive -> if (element.isString) element.content else null
            else -> null
        }

        assertNotNull(id)
        return id
    }

    /** Logs in with username+password and returns the access token cookie. */
    fun loginCookie(mockMvc: MockMvc, json: Json, username: String, password: String = "Password1"): Cookie {
        val mvcResult = mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.COOKIE.name)
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = username, password = password)
            )
        }.andExpect {
            status { isOk() }
            cookie { exists(ACCESS_TOKEN_COOKIE_NAME) }
        }.andReturn()

        return mvcResult.response.getCookie(ACCESS_TOKEN_COOKIE_NAME)
            .also { assertNotNull(it) }!!
    }

    /**
     * Creates an ADMIN user (via API) and grants ADMIN role directly in the DB.
     * Returns the created user id + an authenticated cookie for that user.
     */
    fun createAdminAndLogin(
        mockMvc: MockMvc,
        json: Json,
        userRepository: UserRepository,
        email: String,
        password: String = "Password1"
    ): Pair<String, Cookie> {
        val userId = createUser(mockMvc, json, email, password)

        val objId = ObjectId(userId)
        val user = userRepository.findById(objId).orElseThrow()
        if (!user.roles.contains("ADMIN")) {
            userRepository.save(user.copy(roles = user.roles + "ADMIN"))
        }

        val cookie = loginCookie(mockMvc, json, email, password)
        return userId to cookie
    }
}
