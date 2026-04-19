package com.quickbite.menu.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CategoryRequest {
    private Integer restaurantId;
    private String name;
    private String description;
    private Integer displayOrder;
    private MultipartFile image;
}
