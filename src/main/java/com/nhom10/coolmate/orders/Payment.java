package com.nhom10.coolmate.orders;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String method; // COD, MOMO, VNPAY
    private String status; // UNPAID, PAID
    private LocalDateTime createdAt;
}