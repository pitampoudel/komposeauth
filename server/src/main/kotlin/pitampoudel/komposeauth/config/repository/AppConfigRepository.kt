package pitampoudel.komposeauth.config.repository

import org.springframework.data.mongodb.repository.MongoRepository
import pitampoudel.komposeauth.config.entity.AppConfig

interface AppConfigRepository : MongoRepository<AppConfig, String>