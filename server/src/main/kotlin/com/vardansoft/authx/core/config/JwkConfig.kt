package com.vardansoft.authx.core.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.vardansoft.authx.AppProperties
import com.vardansoft.authx.core.service.StorageService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

@Configuration(proxyBeanMethods = false)
class JwkConfig(
    @Lazy private val storageService: StorageService,
    appProperties: AppProperties
) {
    private val logger = LoggerFactory.getLogger(JwkConfig::class.java)
    private val publicKeyBlobName = "jwk/public.key"
    private val privateKeyBlobName = "jwk/private.key"

    private val localDir: Path = Path.of(System.getProperty("user.home"), appProperties.name, "jwk")
    private val localPublicKey: Path = localDir.resolve("public.key")
    private val localPrivateKey: Path = localDir.resolve("private.key")

    @Bean
    @Lazy
    fun serverKeyPair(): KeyPair {
        try {
            val local = tryLoadKeyPairFromLocal()
            if (local != null) {
                logger.info("Loaded RSA key pair from local cache at {}", localDir)
                return local
            }
        } catch (e: Exception) {
            logger.warn("Failed to load RSA key pair from local cache: {}", e.toString())
        }

        val fromGcs = tryLoadKeyPairFromGcs()
        if (fromGcs != null) {
            logger.info("Loaded RSA key pair from Google Cloud Storage; caching locally at {}", localDir)
            try {
                saveKeyPairToLocal(fromGcs)
            } catch (e: Exception) {
                logger.warn("Failed to save RSA key pair to local cache: {}", e.toString())
            }
            return fromGcs
        }

        logger.info("Generating new RSA key pair and saving to Google Cloud Storage and local cache")
        val generated = generateAndSaveKeyPairToGcs()
        try {
            saveKeyPairToLocal(generated)
        } catch (e: Exception) {
            logger.warn("Failed to save generated RSA key pair to local cache: {}", e.toString())
        }
        return generated
    }

    @Bean
    @Lazy
    fun jwkSet(serverKeyPair: KeyPair): JWKSet {
        val publicKey = serverKeyPair.public as RSAPublicKey
        val privateKey = serverKeyPair.private as RSAPrivateKey
        val rsaKey = RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID("spring-boot-jwk") // static kid is fine unless rotating
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
        return NimbusJwtEncoder(jwkSource)
    }

    @Bean
    @Lazy
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

    private fun saveKeyPairToLocal(keyPair: KeyPair) {
        if (!Files.exists(localDir)) {
            Files.createDirectories(localDir)
        }
        Files.write(localPublicKey, keyPair.public.encoded, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
        Files.write(localPrivateKey, keyPair.private.encoded, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    }

    private fun tryLoadKeyPairFromLocal(): KeyPair? {
        if (!Files.exists(localPublicKey) || !Files.exists(localPrivateKey)) return null
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicBytes = Files.readAllBytes(localPublicKey)
        val privateBytes = Files.readAllBytes(localPrivateKey)
        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicBytes))
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes))
        return KeyPair(publicKey, privateKey)
    }

    private fun tryLoadKeyPairFromGcs(): KeyPair? {
        val keyFactory = KeyFactory.getInstance("RSA")

        val publicBytes = storageService.download(publicKeyBlobName)
        val privateBytes = storageService.download(privateKeyBlobName)

        if (publicBytes == null || privateBytes == null) {
            return null
        }

        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicBytes))
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes))

        return KeyPair(publicKey, privateKey)
    }
}
