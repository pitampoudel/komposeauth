package com.vardansoft.komposeauth.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController {

    @GetMapping("/test/unsupported-operation")
    fun unsupportedOperation() {
        throw UnsupportedOperationException("This is a test exception")
    }
}
