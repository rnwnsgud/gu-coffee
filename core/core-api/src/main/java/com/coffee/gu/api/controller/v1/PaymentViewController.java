package com.coffee.gu.api.controller.v1;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaymentViewController {

    @GetMapping("/payment-test")
    public String paymentTest() {
        return "forward:/payment-test.html";
    }

    @GetMapping("/payment-success")
    public String paymentSuccess() {
        return "forward:/payment-success.html";
    }

    @GetMapping("/payment-fail")
    public String paymentFail() {
        return "forward:/payment-fail.html";
    }
}
