package pitampoudel.komposeauth.app_config.service

import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.repository.AppConfigRepository
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Service
class AppConfigProvider(
    private val repo: AppConfigRepository,
    private val crypto: CryptoService,
    /** Absent only where a test builds this by hand; then nothing retired is kept across a save. */
    private val mongoTemplate: MongoTemplate? = null
) {
    private data class Cached(val value: AppConfig, val loadedAt: Instant)

    private val cache = AtomicReference<Cached?>(null)

    fun get(): AppConfig {
        val cached = cache.get()
        if (cached != null && !isStale(cached)) return cached.value
        val loadedEncrypted = repo.findById(AppConfig.SINGLETON_ID).orElse(AppConfig()).clean()
        val loaded = decrypt(loadedEncrypted)
        cache.set(Cached(loaded, Instant.now()))
        return loaded
    }

    fun save(appConfig: AppConfig): AppConfig {
        // Persist encrypted, cache decrypted
        val toSave = encrypt(appConfig.copy(id = AppConfig.SINGLETON_ID).clean())
        val retired = retiredFields()
        val savedEncrypted = repo.save(toSave)
        restore(retired)
        val saved = decrypt(savedEncrypted)
        cache.set(Cached(saved, Instant.now()))
        return saved
    }

    /**
     * A save replaces the whole document, which would drop the fields this release no longer maps.
     * They are read first and put back, so the release before can still be rolled back to.
     */
    private fun retiredFields(): Document? {
        val template = mongoTemplate ?: return null
        val stored = template.getCollection(template.getCollectionName(AppConfig::class.java))
            .find(Document("_id", AppConfig.SINGLETON_ID)).first() ?: return null
        return Document(AppConfig.RETIRED_STORAGE_FIELDS.filter(stored::containsKey).associateWith(stored::get))
    }

    private fun restore(retired: Document?) {
        if (retired.isNullOrEmpty()) return
        val update = Update()
        retired.forEach { (field, value) -> update.set(field, value) }
        mongoTemplate!!.updateFirst(Query(Criteria.where("_id").`is`(AppConfig.SINGLETON_ID)), update, AppConfig::class.java)
    }

    fun clearCache() {
        cache.set(null)
    }

    /**
     * The cache lives in one process, so a save only refreshes the instance that handled it. Held
     * forever, every other instance would keep serving the old configuration until it happened to
     * restart — which matters here because this config carries the CORS origin list and the
     * provider secrets, so narrowing access or rotating a leaked key would silently not take effect
     * fleet-wide. A short expiry bounds that to [CACHE_TTL] without giving up the read caching.
     */
    private fun isStale(cached: Cached): Boolean =
        Duration.between(cached.loadedAt, Instant.now()) >= CACHE_TTL

    private companion object {
        val CACHE_TTL: Duration = Duration.ofSeconds(60)
    }

    private fun encrypt(src: AppConfig): AppConfig = src.mapSecrets(crypto::encrypt)

    private fun decrypt(src: AppConfig): AppConfig = src.mapSecrets(crypto::decrypt)

    /** Every credential the config holds, listed once so encrypting and decrypting cannot drift apart. */
    private fun AppConfig.mapSecrets(transform: (String) -> String): AppConfig = copy(
        googleAuthClientSecret = googleAuthClientSecret?.let(transform),
        googleAuthDesktopClientSecret = googleAuthDesktopClientSecret?.let(transform),
        twilioAuthToken = twilioAuthToken?.let(transform),
        smtpPassword = smtpPassword?.let(transform),
        samayeApiKey = samayeApiKey?.let(transform),
        sparrowApiToken = sparrowApiToken?.let(transform),
        whatsappAccessToken = whatsappAccessToken?.let(transform),
        alertWebhookUrl = alertWebhookUrl?.let(transform),
        slackBotToken = slackBotToken?.let(transform),
        thirdFactorSecretKey = thirdFactorSecretKey?.let(transform),
        thirdFactorToken = thirdFactorToken?.let(transform),
    )
}