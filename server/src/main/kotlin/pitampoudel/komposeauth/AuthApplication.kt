package pitampoudel.komposeauth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.TimeZone
import javax.annotation.PostConstruct

@SpringBootApplication
class AuthApplication {

    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
}

fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}
