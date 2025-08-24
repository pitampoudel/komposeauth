package com.vardansoft.authx.core.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.vardansoft.authx.core.service.StorageService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

@Configuration
class JwkConfig(
    @Lazy private val storageService: StorageService
) {
    private val logger = LoggerFactory.getLogger(JwkConfig::class.java)
    private val publicKeyBlobName = "jwk/public.key"
    private val privateKeyBlobName = "jwk/private.key"

    @Bean
    fun jwkSource(): JWKSource<SecurityContext?> {

        val keyPair = when {
            storageService.exists(publicKeyBlobName) && storageService.exists(privateKeyBlobName) -> {
                logger.info("Loading existing key pair from Google Cloud Storage")
                loadKeyPairFromGcs()
            }

            else -> {
                logger.info("Generating new key pair and saving to Google Cloud Storage")
                generateAndSaveKeyPairToGcs()
            }
        }

        val publicKey = keyPair.public as RSAPublicKey
        val privateKey = keyPair.private as RSAPrivateKey

        val rsaKey = RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID("spring-boot-jwk") // static kid is fine unless rotating
            .build()

        return ImmutableJWKSet(JWKSet(rsaKey))
    }
    @Bean
    fun jwtEncoder(jwkSource: JWKSource<SecurityContext>): JwtEncoder {
        return NimbusJwtEncoder(jwkSource)
    }

    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
    }

    private fun generateAndSaveKeyPairToGcs(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val keyPair = generator.generateKeyPair()

        val publicSpec = X509EncodedKeySpec(keyPair.public.encoded)
        val privateSpec = PKCS8EncodedKeySpec(keyPair.private.encoded)

        storageService.upload(publicKeyBlobName, "application/octet-stream", publicSpec.encoded)
        storageService.upload(privateKeyBlobName, "application/octet-stream", privateSpec.encoded)

        return keyPair
    }

    private fun loadKeyPairFromGcs(): KeyPair {
        val keyFactory = KeyFactory.getInstance("RSA")

        val publicBytes = storageService.download(publicKeyBlobName)
            ?: throw IllegalStateException("Public key not found in GCS")
        val privateBytes = storageService.download(privateKeyBlobName)
            ?: throw IllegalStateException("Private key not found in GCS")

        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicBytes))
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes))

        return KeyPair(publicKey, privateKey)
    }
}
