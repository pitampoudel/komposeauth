package pitampoudel.komposeauth.setup.repository

import org.springframework.data.mongodb.repository.MongoRepository
import pitampoudel.komposeauth.setup.entity.AppConfig

interface AppConfigRepository : MongoRepository<AppConfig, String>