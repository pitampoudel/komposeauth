package com.vardansoft.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app")
class AppProperties {
    lateinit var baseUrl: String
    lateinit var gcpBucketName: String
    lateinit var smsApiKey: String
}