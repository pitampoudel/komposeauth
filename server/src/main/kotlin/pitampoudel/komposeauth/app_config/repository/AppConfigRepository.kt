package pitampoudel.komposeauth.app_config.repository

import org.springframework.data.mongodb.repository.MongoRepository
import pitampoudel.komposeauth.app_config.entity.AppConfig

interface AppConfigRepository : MongoRepository<AppConfig, String>