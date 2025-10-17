package com.vardansoft.komposeauth

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component


@Component
@Profile("!test") // Don't run in test profile
class MongoDataMigration(
    private val mongoTemplate: MongoTemplate
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
    }


}
