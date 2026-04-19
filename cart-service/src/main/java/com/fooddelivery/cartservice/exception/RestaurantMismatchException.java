package com.fooddelivery.cartservice.exception;

public class RestaurantMismatchException extends RuntimeException {
    public RestaurantMismatchException(String message) {
        super(message);
    }
}
