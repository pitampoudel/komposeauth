package pitampoudel.komposeauth.app_config

import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.app_config.controller.ConfigContext
import pitampoudel.komposeauth.app_config.controller.ConfigField
import pitampoudel.komposeauth.app_config.controller.ConfigSections
import pitampoudel.komposeauth.app_config.controller.ConnectionStatus
import pitampoudel.komposeauth.app_config.entity.AppConfig
import kotlin.reflect.full.memberProperties
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigSectionsTest {

    private fun section(id: String) = ConfigSections.byId(id)!!

    private fun saved(result: ConfigSections.Result) = assertIs<ConfigSections.Result.Saved>(result).config

    /** A setting missing from every section can't be changed from the console at all. */
    @Test
    fun `every stored setting is on the page exactly once`() {
        val stored = AppConfig::class.memberProperties.map { it.name }.toSet() - setOf("id", "createdAt", "updatedAt")
        val shown = ConfigSections.all.flatMap { it.fields }.map { it.name }

        assertEquals(stored, shown.toSet())
        assertEquals(shown.size, shown.toSet().size, "a field appears in two sections")
    }

    /** The same list `AppConfigProvider` encrypts: each is write-only on the page. */
    @Test
    fun `every encrypted setting is a secret on the page`() {
        val encrypted = listOf(
            "googleAuthClientSecret", "googleAuthDesktopClientSecret", "twilioAuthToken", "smtpPassword",
            "samayeApiKey", "sparrowApiToken", "whatsappAccessToken", "slackBotToken",
            "thirdFactorSecretKey", "thirdFactorToken"
        )
        encrypted.forEach { assertTrue(ConfigSections.isSecret(it), "$it is shown back to the browser") }
    }

    @Test
    fun `a saved secret is never sent back to the page`() {
        val view = ConfigSections.view(section("email"), AppConfig(smtpPassword = "hunter2"), ConfigContext("https://auth.example.com"))
        val password = view.fields.first { it.name == "smtpPassword" }

        assertEquals("", password.value)
        assertTrue(password.saved)
    }

    @Test
    fun `an empty secret keeps the stored one and an empty setting clears its own`() {
        val before = AppConfig(smtpHost = "smtp.old.example", smtpPassword = "hunter2", smtpFromName = "Old")

        val after = saved(
            ConfigSections.apply(
                section("email"), before,
                mapOf("smtpHost" to "smtp.new.example", "smtpPassword" to "", "smtpFromName" to "")
            )
        )

        assertEquals("smtp.new.example", after.smtpHost)
        assertEquals("hunter2", after.smtpPassword)
        assertNull(after.smtpFromName)
    }

    @Test
    fun `a section touches nothing outside itself or anything it was not sent`() {
        val before = AppConfig(name = "Example", smtpHost = "smtp.example.com", smtpUsername = "mailer")

        val after = saved(ConfigSections.apply(section("email"), before, mapOf("smtpHost" to "smtp.example.org")))

        assertEquals("Example", after.name)
        assertEquals("mailer", after.smtpUsername)
        assertEquals("smtp.example.org", after.smtpHost)
    }

    @Test
    fun `values that cannot work are refused with their label`() {
        val result = ConfigSections.apply(
            section("branding"), AppConfig(),
            mapOf("logoUrl" to "logo.png", "brandColor" to "blue", "supportEmail" to "support")
        )

        val problems = assertIs<ConfigSections.Result.Refused>(result).problems
        assertEquals(3, problems.size)
        assertTrue(problems.any { it.startsWith("Logo address") })
        assertTrue(problems.any { it.startsWith("Brand colour") })
        assertTrue(problems.any { it.startsWith("Support email") })
    }

    @Test
    fun `numbers are stored as numbers and countries in upper case`() {
        val network = saved(ConfigSections.apply(section("network"), AppConfig(), mapOf("trustedProxyCount" to "1")))
        assertEquals(1, network.trustedProxyCount)

        val signin = saved(ConfigSections.apply(section("signin"), AppConfig(), mapOf("defaultPhoneRegion" to "in")))
        assertEquals("IN", signin.defaultPhoneRegion)

        assertIs<ConfigSections.Result.Refused>(
            ConfigSections.apply(section("signin"), AppConfig(), mapOf("defaultPhoneRegion" to "ZZ"))
        )
    }

    @Test
    fun `disconnecting clears every field of that connection and nothing else`() {
        val before = AppConfig(name = "Example", twilioAuthToken = "t", twilioAccountSid = "AC1", smsProvider = "twilio")

        val after = section("sms").disconnect(before)

        assertNull(after.smsProvider)
        assertNull(after.twilioAuthToken)
        assertNull(after.twilioAccountSid)
        assertEquals("Example", after.name)
    }

    @Test
    fun `a connection says what it still needs`() {
        val status = section("sms").status!!(AppConfig(smsProvider = "twilio", twilioAccountSid = "AC1"))

        assertEquals(ConnectionStatus.State.INCOMPLETE, status.state)
        assertTrue(status.detail.contains("auth token"))
        assertEquals(ConnectionStatus.State.OFF, section("sms").status!!(AppConfig()).state)
        assertEquals(
            ConnectionStatus.State.CONNECTED,
            section("email").status!!(AppConfig(smtpHost = "smtp.example.com", smtpFromEmail = "a@example.com")).state
        )
    }

    @Test
    fun `every select's stored value is one of its choices`() {
        ConfigSections.all.flatMap { it.fields }.filter { it.type == ConfigField.Type.SELECT }.forEach { field ->
            assertTrue(field.options.any { it.value == "" }, "${field.name} can't be left unset")
        }
    }
}
