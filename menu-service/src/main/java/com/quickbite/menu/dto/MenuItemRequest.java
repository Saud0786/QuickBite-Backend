package com.quickbite.menu.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class MenuItemRequest {
    private Integer restaurantId;
    private Integer categoryId;
    private String name;
    private String description;
    private Double price;
    private Boolean isVeg;
    private Boolean isAvailable;
    private MultipartFile image;
}
