package pitampoudel.komposeauth.general.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pitampoudel.core.data.MessageResponse

@RestController
class HomeController() {
    @GetMapping("/")
    fun index(): ResponseEntity<MessageResponse> {
        return ResponseEntity.ok(MessageResponse("Server is up"))
    }
}
