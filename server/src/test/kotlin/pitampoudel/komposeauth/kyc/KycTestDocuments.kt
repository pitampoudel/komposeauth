package pitampoudel.komposeauth.kyc

import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import pitampoudel.komposeauth.StaticAppProperties
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.core.utils.ServerUrl
import pitampoudel.komposeauth.kyc.service.KycDocuments
import java.util.Base64

const val TEST_ISSUER = "https://auth.example.test"

/** A [KycDocuments] over [storage] with a throwaway key, for tests that build the KYC service by hand. */
fun kycDocumentsOver(storage: StorageService) = KycDocuments(
    storage,
    CryptoService(StaticAppProperties().apply {
        base64EncryptionKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    }),
    ServerUrl(AuthorizationServerSettings.builder().issuer(TEST_ISSUER).build())
)
