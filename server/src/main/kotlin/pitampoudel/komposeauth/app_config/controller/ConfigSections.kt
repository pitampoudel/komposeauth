package pitampoudel.komposeauth.app_config.controller

import pitampoudel.komposeauth.app_config.entity.AppConfig
import java.net.URI
import java.util.Locale
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

/**
 * What the Configuration page shows, in the words an operator uses.
 *
 * The stored [AppConfig] is one flat document, but nobody thinks of it that way: a person connects
 * a mail server, turns on Google sign-in or names their app. So the page is a list of
 * [ConfigSection]s, each saved on its own, and a connection says whether it is set up rather than
 * leaving the operator to work that out from a column of field names.
 *
 * A secret is never sent back to the browser. Its field is left empty, says that a value is saved,
 * and an empty submission keeps the stored one; [ConfigSection.disconnect] is how it is cleared.
 */
data class ConfigSection(
    val id: String,
    val kind: Kind,
    val title: String,
    val summary: String,
    val fields: List<ConfigField>,
    /** Absent for a settings section, which has no on or off. */
    val status: ((AppConfig) -> ConnectionStatus)? = null,
    /** What the Test button does, if the connection has one. */
    val test: TestKind? = null,
    /** Shown once, above the fields: where the values come from, or what to register elsewhere. */
    val guide: ((ConfigContext) -> String?)? = null,
) {
    enum class Kind { CONNECTION, SETTINGS }

    enum class TestKind { EMAIL, SMS, ALERTS }

    /** Every field this section owns, set to nothing. */
    fun disconnect(config: AppConfig): AppConfig {
        val cleared = config.copy()
        fields.forEach { field -> field.property().set(cleared, null) }
        return cleared
    }
}

data class ConfigField(
    /** The [AppConfig] property, spelled exactly. */
    val name: String,
    val label: String,
    val help: String? = null,
    val type: Type = Type.TEXT,
    val placeholder: String? = null,
    val options: List<Option> = emptyList(),
    /** Shown only while the section's select of this name has one of these values. */
    val showWhen: Pair<String, Set<String>>? = null,
) {
    enum class Type { TEXT, URL, EMAIL, NUMBER, TEXTAREA, SECRET, SELECT, COLOR }

    data class Option(val value: String, val label: String)

    @Suppress("UNCHECKED_CAST")
    fun property(): KMutableProperty1<AppConfig, Any?> =
        AppConfig::class.memberProperties.first { it.name == name } as KMutableProperty1<AppConfig, Any?>
}

data class ConnectionStatus(val state: State, val detail: String) {
    enum class State { CONNECTED, INCOMPLETE, OFF }

    val label: String
        get() = when (state) {
            State.CONNECTED -> "Connected"
            State.INCOMPLETE -> "Incomplete"
            State.OFF -> "Not set up"
        }
}

/** What a guide may need to say: the addresses another service has to be told about. */
data class ConfigContext(val issuer: String)

/** One field as the template draws it. A secret's [value] is always empty; [saved] says whether one is stored. */
data class FieldView(
    val field: ConfigField,
    val value: String,
    val saved: Boolean,
) {
    val name get() = field.name
    val label get() = field.label
    val help get() = field.help
    val type get() = field.type.name.lowercase()
    val placeholder
        get() = when {
            field.type == ConfigField.Type.SECRET && saved -> "Leave empty to keep the saved one"
            else -> field.placeholder.orEmpty()
        }
    val options get() = field.options
    val showWhenField get() = field.showWhen?.first
    val showWhenValues get() = field.showWhen?.second?.joinToString(",")
    val wide get() = field.type == ConfigField.Type.TEXTAREA
}

data class SectionView(
    val section: ConfigSection,
    val fields: List<FieldView>,
    val status: ConnectionStatus?,
    val guide: String?,
) {
    val id get() = section.id
    val title get() = section.title
    val summary get() = section.summary
    val test get() = section.test?.name?.lowercase()
    val hasValues get() = fields.any { it.saved }
}

object ConfigSections {

    private fun set(value: String?) = !value.isNullOrBlank()

    private val SMS_PROVIDERS = listOf(
        ConfigField.Option("", "None"),
        ConfigField.Option("twilio", "Twilio Verify"),
        ConfigField.Option("whatsapp", "WhatsApp Business"),
        ConfigField.Option("sparrow", "Sparrow SMS (Nepal)"),
        ConfigField.Option("samaye", "Samaye SMS (Nepal)"),
    )

    private fun providerLabel(value: String?) =
        SMS_PROVIDERS.firstOrNull { it.value == value?.lowercase() }?.label ?: value.orEmpty()

    private fun required(vararg fields: Pair<String?, String>): List<String> =
        fields.filter { !set(it.first) }.map { it.second }

    private fun missing(names: List<String>) =
        "Needs " + (if (names.size > 1) names.dropLast(1).joinToString(", ") + " and " + names.last() else names.single())

    val all: List<ConfigSection> = listOf(
        ConfigSection(
            id = "google",
            kind = ConfigSection.Kind.CONNECTION,
            title = "Google sign-in",
            summary = "Let people sign in with their Google account.",
            fields = listOf(
                ConfigField("googleAuthClientId", "Web client ID", placeholder = "1234-abc.apps.googleusercontent.com"),
                ConfigField("googleAuthClientSecret", "Web client secret", type = ConfigField.Type.SECRET),
                ConfigField(
                    "googleAuthDesktopClientId", "Desktop client ID",
                    help = "Only for desktop apps that sign in through the system browser."
                ),
                ConfigField("googleAuthDesktopClientSecret", "Desktop client secret", type = ConfigField.Type.SECRET),
            ),
            status = { c ->
                val gaps = required(c.googleAuthClientId to "a client ID", c.googleAuthClientSecret to "a client secret")
                when {
                    gaps.isEmpty() -> ConnectionStatus(ConnectionStatus.State.CONNECTED, "Web sign-in is on")
                    gaps.size == 2 && !set(c.googleAuthDesktopClientId) ->
                        ConnectionStatus(ConnectionStatus.State.OFF, "Needs a client ID from Google Cloud")
                    else -> ConnectionStatus(ConnectionStatus.State.INCOMPLETE, missing(gaps))
                }
            },
            guide = { ctx ->
                "Create an OAuth client of type Web application in Google Cloud Console, under APIs & " +
                    "Services → Credentials, and add this authorized redirect URI: " +
                    "${ctx.issuer}/login/oauth2/code/google"
            }
        ),
        ConfigSection(
            id = "apple",
            kind = ConfigSection.Kind.CONNECTION,
            title = "Sign in with Apple",
            summary = "Let people on iPhone, iPad and Mac sign in with their Apple ID.",
            fields = listOf(
                ConfigField(
                    "appleAuthClientId", "Services ID or bundle ID",
                    help = "The identifier Apple puts in the token's audience, such as com.example.app.",
                    placeholder = "com.example.app"
                ),
            ),
            status = { c ->
                if (set(c.appleAuthClientId)) ConnectionStatus(ConnectionStatus.State.CONNECTED, c.appleAuthClientId!!)
                else ConnectionStatus(ConnectionStatus.State.OFF, "Needs an identifier from Apple")
            }
        ),
        ConfigSection(
            id = "email",
            kind = ConfigSection.Kind.CONNECTION,
            title = "Email",
            summary = "Sends sign-in codes, password resets and account notices.",
            fields = listOf(
                ConfigField("smtpHost", "SMTP server", placeholder = "smtp.example.com"),
                ConfigField("smtpPort", "Port", help = "587 when empty.", type = ConfigField.Type.NUMBER, placeholder = "587"),
                ConfigField("smtpUsername", "Username"),
                ConfigField("smtpPassword", "Password", type = ConfigField.Type.SECRET),
                ConfigField("smtpFromEmail", "Send from", type = ConfigField.Type.EMAIL, placeholder = "no-reply@example.com"),
                ConfigField("smtpFromName", "Sender name", placeholder = "Example"),
                ConfigField(
                    "emailFooterText", "Footer",
                    help = "A line at the bottom of every email, such as your company's address.",
                    type = ConfigField.Type.TEXTAREA
                ),
            ),
            status = { c ->
                when {
                    !set(c.smtpHost) -> ConnectionStatus(ConnectionStatus.State.OFF, "Needs an SMTP server")
                    !set(c.smtpFromEmail) -> ConnectionStatus(ConnectionStatus.State.INCOMPLETE, "Needs an address to send from")
                    else -> ConnectionStatus(ConnectionStatus.State.CONNECTED, "${c.smtpHost} as ${c.smtpFromEmail}")
                }
            },
            test = ConfigSection.TestKind.EMAIL,
            guide = {
                "Any SMTP service works: Amazon SES, SendGrid, Mailgun, Postmark, Resend, Google Workspace " +
                    "or your own server. The connection uses STARTTLS."
            }
        ),
        ConfigSection(
            id = "sms",
            kind = ConfigSection.Kind.CONNECTION,
            title = "Text messages",
            summary = "Sends one-time codes to phone numbers.",
            fields = listOf(
                ConfigField("smsProvider", "Provider", type = ConfigField.Type.SELECT, options = SMS_PROVIDERS),
                ConfigField("twilioAccountSid", "Account SID", showWhen = "smsProvider" to setOf("twilio")),
                ConfigField("twilioAuthToken", "Auth token", type = ConfigField.Type.SECRET, showWhen = "smsProvider" to setOf("twilio")),
                ConfigField("twilioVerifyServiceSid", "Verify service SID", placeholder = "VA…", showWhen = "smsProvider" to setOf("twilio")),
                ConfigField(
                    "twilioFromNumber", "From number",
                    help = "Not needed for Verify; kept for older setups.",
                    showWhen = "smsProvider" to setOf("twilio")
                ),
                ConfigField("whatsappPhoneNumberId", "Phone number ID", showWhen = "smsProvider" to setOf("whatsapp")),
                ConfigField("whatsappAccessToken", "Access token", type = ConfigField.Type.SECRET, showWhen = "smsProvider" to setOf("whatsapp")),
                ConfigField("sparrowApiToken", "API token", type = ConfigField.Type.SECRET, showWhen = "smsProvider" to setOf("sparrow")),
                ConfigField("sparrowFromNumber", "Sender ID", showWhen = "smsProvider" to setOf("sparrow")),
                ConfigField("samayeApiKey", "API key", type = ConfigField.Type.SECRET, showWhen = "smsProvider" to setOf("samaye")),
            ),
            status = { c ->
                val provider = c.smsProvider?.lowercase().orEmpty()
                val gaps = when (provider) {
                    "" -> null
                    "twilio" -> required(c.twilioAccountSid to "an account SID", c.twilioAuthToken to "an auth token", c.twilioVerifyServiceSid to "a Verify service SID")
                    "whatsapp" -> required(c.whatsappPhoneNumberId to "a phone number ID", c.whatsappAccessToken to "an access token")
                    "sparrow" -> required(c.sparrowApiToken to "an API token")
                    "samaye" -> required(c.samayeApiKey to "an API key")
                    else -> emptyList()
                }
                if (gaps == null) ConnectionStatus(ConnectionStatus.State.OFF, "No provider chosen")
                else if (gaps.isEmpty()) ConnectionStatus(ConnectionStatus.State.CONNECTED, providerLabel(provider))
                else ConnectionStatus(ConnectionStatus.State.INCOMPLETE, "${providerLabel(provider)}: ${missing(gaps)}")
            },
            test = ConfigSection.TestKind.SMS
        ),
        ConfigSection(
            id = "errors",
            kind = ConfigSection.Kind.CONNECTION,
            title = "Error reporting",
            summary = "Sends unexpected server errors to Sentry, or anything that accepts a Sentry DSN, such as GlitchTip.",
            fields = listOf(
                ConfigField("sentryDsn", "DSN", type = ConfigField.Type.URL, placeholder = "https://key@o0.ingest.sentry.io/0"),
            ),
            status = { c ->
                if (set(c.sentryDsn)) ConnectionStatus(ConnectionStatus.State.CONNECTED, hostOf(c.sentryDsn))
                else ConnectionStatus(ConnectionStatus.State.OFF, "Errors are only logged")
            }
        ),
        ConfigSection(
            id = "alerts",
            kind = ConfigSection.Kind.CONNECTION,
            title = "Alerts",
            summary = "Posts to your team's chat when an admin role is granted or revoked and when identity documents are submitted.",
            fields = listOf(
                ConfigField(
                    "alertWebhookUrl", "Webhook URL", type = ConfigField.Type.SECRET,
                    placeholder = "https://hooks.slack.com/services/…",
                    help = "An incoming webhook from Slack, Discord, Mattermost, Google Chat or Rocket.Chat."
                ),
                ConfigField(
                    "slackBotToken", "Slack bot token", type = ConfigField.Type.SECRET, placeholder = "xoxb-…",
                    help = "Only for a Slack app instead of a webhook. Alerts go to both when both are set."
                ),
                ConfigField("slackChannelId", "Slack channel ID", placeholder = "C0123456789"),
            ),
            status = { c ->
                val slackGaps = required(c.slackBotToken to "a bot token", c.slackChannelId to "a channel ID")
                val places = listOfNotNull(
                    c.alertWebhookUrl?.takeIf(::set)?.let { url -> "Webhook" + (runCatching { URI(url).host }.getOrNull()?.let { " to $it" } ?: "") },
                    c.slackChannelId.takeIf { slackGaps.isEmpty() }?.let { "Slack channel $it" }
                )
                when {
                    slackGaps.size == 1 -> ConnectionStatus(ConnectionStatus.State.INCOMPLETE, "Slack app: ${missing(slackGaps)}")
                    places.isNotEmpty() -> ConnectionStatus(ConnectionStatus.State.CONNECTED, places.joinToString(" and "))
                    else -> ConnectionStatus(ConnectionStatus.State.OFF, "No alerts are sent")
                }
            },
            test = ConfigSection.TestKind.ALERTS,
            guide = {
                "Simplest is a webhook: in Slack, add an Incoming Webhook to the channel; in Discord, open the " +
                    "channel's settings, then Integrations, then Webhooks; in Google Chat, Apps & integrations. " +
                    "Paste its URL here. A Slack app works too: give it the chat:write scope, install it and " +
                    "invite it to the channel."
            }
        ),
        ConfigSection(
            id = "kyc",
            kind = ConfigSection.Kind.CONNECTION,
            title = "Identity verification",
            summary = "Hands identity checks to Third Factor's hosted KYC service instead of manual document review.",
            fields = listOf(
                ConfigField("thirdFactorUrl", "Service address", type = ConfigField.Type.URL),
                ConfigField("thirdFactorToken", "API token", type = ConfigField.Type.SECRET),
                ConfigField("thirdFactorSecretKey", "Signing secret", type = ConfigField.Type.SECRET),
            ),
            status = { c ->
                val gaps = required(c.thirdFactorUrl to "a service address", c.thirdFactorToken to "an API token", c.thirdFactorSecretKey to "a signing secret")
                when (gaps.size) {
                    0 -> ConnectionStatus(ConnectionStatus.State.CONNECTED, hostOf(c.thirdFactorUrl))
                    3 -> ConnectionStatus(ConnectionStatus.State.OFF, "Documents are reviewed by hand")
                    else -> ConnectionStatus(ConnectionStatus.State.INCOMPLETE, missing(gaps))
                }
            }
        ),

        ConfigSection(
            id = "branding",
            kind = ConfigSection.Kind.SETTINGS,
            title = "Branding",
            summary = "How the sign-in page, emails and this console present you.",
            fields = listOf(
                ConfigField("name", "App name", placeholder = "Example"),
                ConfigField("logoUrl", "Logo address", type = ConfigField.Type.URL, placeholder = "https://example.com/logo.png"),
                ConfigField("brandColor", "Brand colour", type = ConfigField.Type.COLOR),
                ConfigField("websiteUrl", "Website", help = "Where people return after identity verification.", type = ConfigField.Type.URL),
                ConfigField("supportEmail", "Support email", help = "Shown in emails as the address to write to.", type = ConfigField.Type.EMAIL),
            )
        ),
        ConfigSection(
            id = "links",
            kind = ConfigSection.Kind.SETTINGS,
            title = "Links in emails",
            summary = "Shown at the foot of every email. Leave any of them empty to leave it out.",
            fields = listOf(
                ConfigField("privacyLink", "Privacy policy", type = ConfigField.Type.URL),
                ConfigField("facebookLink", "Facebook", type = ConfigField.Type.URL),
                ConfigField("instagramLink", "Instagram", type = ConfigField.Type.URL),
                ConfigField("tiktokLink", "TikTok", type = ConfigField.Type.URL),
                ConfigField("linkedinLink", "LinkedIn", type = ConfigField.Type.URL),
                ConfigField("youtubeLink", "YouTube", type = ConfigField.Type.URL),
            )
        ),
        ConfigSection(
            id = "signin",
            kind = ConfigSection.Kind.SETTINGS,
            title = "Sign-in",
            summary = "Passkeys, phone numbers and your own apps.",
            fields = listOf(
                ConfigField(
                    "rpId", "Passkey domain",
                    help = "The domain passkeys belong to, such as example.com. Passkeys work on it and its subdomains, and stop working if it changes.",
                    placeholder = "example.com"
                ),
                ConfigField(
                    "defaultPhoneRegion", "Default country for phone numbers",
                    help = "How a number typed without a + is read.",
                    type = ConfigField.Type.SELECT,
                    options = listOf(ConfigField.Option("", "Nepal (default)")) + countries()
                ),
                ConfigField(
                    "corsAllowedOriginList", "Web apps on other domains",
                    help = "One origin per line, such as https://app.example.com. Only these may call this server from a browser.",
                    type = ConfigField.Type.TEXTAREA
                ),
                ConfigField(
                    "allowedAndroidSha256List", "Android app signing keys",
                    help = "SHA-256 fingerprints, comma-separated, of the apps allowed to use passkeys.",
                    type = ConfigField.Type.TEXTAREA
                ),
            )
        ),
        ConfigSection(
            id = "roles",
            kind = ConfigSection.Kind.SETTINGS,
            title = "Roles",
            summary = "Roles you can give people, on top of USER, ADMIN and SUPER_ADMIN.",
            fields = listOf(
                ConfigField(
                    "rolesCatalog", "Extra roles",
                    help = "One per line, such as EDITOR. They appear in the access token's authorities as ROLE_EDITOR.",
                    type = ConfigField.Type.TEXTAREA
                ),
            )
        ),
        ConfigSection(
            id = "network",
            kind = ConfigSection.Kind.SETTINGS,
            title = "Proxies and rate limits",
            summary = "Rate limits count requests per visitor, so the server has to know where a visitor's address is.",
            fields = listOf(
                ConfigField(
                    "clientIpHeader", "Client address header",
                    help = "A header your platform writes itself: X-Real-IP on Railway, Fly-Client-IP on Fly, CF-Connecting-IP behind Cloudflare. Leave empty otherwise.",
                    placeholder = "CF-Connecting-IP"
                ),
                ConfigField(
                    "trustedProxyCount", "Proxies in front of this server",
                    help = "Used when the header above is empty. 1 on Cloud Run or behind one load balancer, 0 when reached directly.",
                    type = ConfigField.Type.NUMBER,
                    placeholder = "0"
                ),
            )
        ),
    )

    val connections get() = all.filter { it.kind == ConfigSection.Kind.CONNECTION }
    val settings get() = all.filter { it.kind == ConfigSection.Kind.SETTINGS }

    fun byId(id: String): ConfigSection? = all.firstOrNull { it.id == id }

    private val secretNames: Set<String> =
        all.flatMap { it.fields }.filter { it.type == ConfigField.Type.SECRET }.map { it.name }.toSet()

    fun isSecret(name: String) = name in secretNames

    fun view(section: ConfigSection, config: AppConfig, context: ConfigContext) = SectionView(
        section = section,
        fields = section.fields.map { field ->
            val stored = field.property().get(config)?.toString().orEmpty()
            FieldView(
                field = field,
                value = if (field.type == ConfigField.Type.SECRET) "" else stored,
                saved = stored.isNotBlank()
            )
        },
        status = section.status?.invoke(config),
        guide = section.guide?.invoke(context)
    )

    /**
     * [config] with [section]'s fields replaced by what was posted.
     *
     * A field the form did not send is left as it was, and so is a secret sent empty. Everything else
     * sent empty is cleared. Returns the problems instead when a value can't be stored.
     */
    fun apply(section: ConfigSection, config: AppConfig, posted: Map<String, String?>): Result {
        val updated = config.copy()
        val problems = mutableListOf<String>()
        for (field in section.fields) {
            if (!posted.containsKey(field.name)) continue
            val raw = posted[field.name]?.trim().orEmpty()
            if (raw.isEmpty() && field.type == ConfigField.Type.SECRET) continue
            val property = field.property()
            if (raw.isEmpty()) {
                property.set(updated, null)
                continue
            }
            val problem = problemWith(field, raw)
            if (problem != null) {
                problems += problem
                continue
            }
            property.set(updated, if (field.type == ConfigField.Type.NUMBER) raw.toInt() else normalize(field, raw))
        }
        return if (problems.isEmpty()) Result.Saved(updated) else Result.Refused(problems)
    }

    sealed interface Result {
        data class Saved(val config: AppConfig) : Result
        data class Refused(val problems: List<String>) : Result
    }

    private fun normalize(field: ConfigField, raw: String): String = when (field.name) {
        "defaultPhoneRegion" -> raw.uppercase()
        else -> raw
    }

    private fun problemWith(field: ConfigField, raw: String): String? = when {
        field.type == ConfigField.Type.NUMBER ->
            if (raw.toIntOrNull()?.takeIf { it >= 0 } == null) "${field.label} must be a whole number" else null

        field.type == ConfigField.Type.URL ->
            if (!isWebAddress(raw)) "${field.label} must be a full address starting with https://" else null

        field.type == ConfigField.Type.EMAIL ->
            if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(raw)) "${field.label} must be an email address" else null

        field.type == ConfigField.Type.COLOR ->
            if (!Regex("^#[0-9a-fA-F]{6}$").matches(raw)) "${field.label} must be a colour such as #3458d4" else null

        field.type == ConfigField.Type.SELECT && field.options.isNotEmpty() ->
            if (field.options.none { it.value.equals(raw, ignoreCase = true) }) "${field.label} isn't one of the choices" else null

        else -> null
    }

    private fun isWebAddress(raw: String): Boolean = runCatching {
        val uri = URI(raw)
        uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    private fun hostOf(url: String?): String = runCatching { URI(url!!).host }.getOrNull() ?: "Set"

    private fun countries(): List<ConfigField.Option> =
        Locale.getISOCountries()
            .map { code -> ConfigField.Option(code, Locale.of("", code).getDisplayCountry(Locale.ENGLISH)) }
            .sortedBy { it.label }
}
