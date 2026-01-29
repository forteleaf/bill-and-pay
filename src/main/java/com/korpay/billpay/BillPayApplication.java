package com.korpay.billpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bill&Pay Settlement Platform
 * 
 * Main application entry point for the multi-level settlement system.
 * Handles complex fee structures and ledger-based settlement calculations.
 */
@SpringBootApplication
public class BillPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillPayApplication.class, args);
    }
}
