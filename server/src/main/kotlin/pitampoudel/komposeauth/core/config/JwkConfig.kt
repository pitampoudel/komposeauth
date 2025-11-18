package pitampoudel.komposeauth.core.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import pitampoudel.komposeauth.jwk.service.PersistentKeyService
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

@Configuration(proxyBeanMethods = false)
class JwkConfig(
    private val persistentKeyService: PersistentKeyService
) {

    @Bean
    @Lazy
    fun serverKeyPair(): KeyPair = persistentKeyService.loadOrCreateKeyPair()

    @Bean
    @Lazy
    fun jwkSet(serverKeyPair: KeyPair): JWKSet {
        val publicKey = serverKeyPair.public as RSAPublicKey
        val privateKey = serverKeyPair.private as RSAPrivateKey

        val rsaKey = RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(persistentKeyService.currentKid())
            .build()

        return JWKSet(rsaKey)
    }

    @Bean
    @Lazy
    fun jwkSource(jwkSet: JWKSet): JWKSource<SecurityContext> {
        return ImmutableJWKSet(jwkSet)
    }

    @Bean
    @Lazy
    fun jwtEncoder(jwkSource: JWKSource<SecurityContext>): JwtEncoder {
        // Use the asymmetric JWK source (RS256)
        return NimbusJwtEncoder(jwkSource)
    }

    @Bean
    @Lazy
    fun jwtDecoder(serverKeyPair: KeyPair): JwtDecoder {
        val publicKey = serverKeyPair.public as RSAPublicKey
        // NimbusJwtDecoder supports RS256 by default when given a public key
        return NimbusJwtDecoder.withPublicKey(publicKey).build()
    }
}
