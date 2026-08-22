package com.coffee.gu.payment;

import jakarta.persistence.*;

@Entity
@Table(name = "test_payment_pessimistic")
public class PaymentPessimisticEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderKey;
    private String state;

    public PaymentPessimisticEntity() {}
    public PaymentPessimisticEntity(String orderKey, String state) {
        this.orderKey = orderKey;
        this.state = state;
    }

    public Long getId() { return id; }
    public String getOrderKey() { return orderKey; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
