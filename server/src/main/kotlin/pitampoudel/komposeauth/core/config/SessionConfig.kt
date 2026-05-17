package pitampoudel.komposeauth.core.config

import org.mongodb.spring.session.config.annotation.web.http.EnableMongoHttpSession
import org.springframework.context.annotation.Configuration

@Configuration
@EnableMongoHttpSession
class SessionConfig