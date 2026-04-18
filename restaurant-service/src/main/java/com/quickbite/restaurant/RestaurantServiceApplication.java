package com.quickbite.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * QuickBite Restaurant Service
 *
 * Responsibilities:
 * - Restaurant registration and profile management
 * - Geo-proximity search using Haversine formula
 * - Admin approval workflow
 * - Open/close toggle
 * - Average rating aggregation from Review-Service
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class RestaurantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantServiceApplication.class, args);
    }
}
