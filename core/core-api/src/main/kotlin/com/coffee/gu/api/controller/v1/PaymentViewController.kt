package com.coffee.gu.api.controller.v1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class PaymentViewController {
    @GetMapping("/payment-test")
    fun paymentTest(): String {
        return "forward:/payment-test.html"
    }

    @GetMapping("/payment-success")
    fun paymentSuccess(): String {
        return "forward:/payment-success.html"
    }

    @GetMapping("/payment-fail")
    fun paymentFail(): String {
        return "forward:/payment-fail.html"
    }
}
