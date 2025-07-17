package com.vardansoft.auth

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kotlin.test.Test


@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AuthApplicationTests {

    companion object {
        @Container
        @JvmStatic
        var mongo: MongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:5.0"))

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { mongo.replicaSetUrl }
        }
    }


    @Test
    fun contextLoads() {
    }
}
