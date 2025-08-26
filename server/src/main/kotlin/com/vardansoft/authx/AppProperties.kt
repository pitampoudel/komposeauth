package com.vardansoft.authx

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app")
class AppProperties {
    lateinit var baseUrl: String
    lateinit var gcpBucketName: String
    lateinit var name: String
    lateinit var expectedGcpProjectId: String
    lateinit var logoUrl: String

    var smsApiKey: String? = null
    var twilioAccountSid: String? = null
    var twilioAuthToken: String? = null
    var twilioFromNumber: String? = null
}