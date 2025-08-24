package com.vardansoft.authx

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("production")
class ProductionConfigTest {

    @Test
    fun testMissingBaseUrlThrowsException() {
        // This test verifies that the application fails to start if BASE_URL is not provided in production
        assertThrows(Exception::class.java) {
            val app = SpringApplication(AuthApplication::class.java)
            app.setAdditionalProfiles("production")
            // We're not setting the BASE_URL environment variable, so this should fail
            app.run()
        }
    }
}