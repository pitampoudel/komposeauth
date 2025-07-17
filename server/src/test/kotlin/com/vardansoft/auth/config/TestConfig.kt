package com.vardansoft.auth.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.TestPropertySource

/**
 * Test configuration for setting up the test environment.
 * Provides test-specific beans and properties.
 */
@TestConfiguration
@TestPropertySource(properties = ["app.gcpBucketName=mock-bucket", "app.baseUrl=http://localhost:8080"])
class TestConfig
