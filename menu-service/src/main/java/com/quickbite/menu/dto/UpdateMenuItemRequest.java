package com.quickbite.menu.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateMenuItemRequest {
    private Integer restaurantId;
    private Integer categoryId;
    private String name;
    private String description;
    private Double price;
    private Double discountedPrice;
    private Boolean isVeg;
    private Boolean isAvailable;
    private Integer calories;
    private String tags;
    private MultipartFile image;
}