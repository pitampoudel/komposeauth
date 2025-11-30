package pitampoudel.komposeauth.app_config.service

import org.springframework.stereotype.Component
import pitampoudel.komposeauth.StaticAppProperties

@Component("masterKeyValidator")
class MasterKeyValidator(val staticAppProperties: StaticAppProperties) {
    fun isValid(masterKey: String): Boolean {
        return masterKey == staticAppProperties.base64EncryptionKey
    }
}
