package pitampoudel.komposeauth.core.init

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

@Component
class MongoDataMigration(
    private val mongoTemplate: MongoTemplate
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
    }


}