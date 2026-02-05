package com.korpay.billpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BillPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillPayApplication.class, args);
    }
}
