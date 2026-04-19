package com.quickbite.menu.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menu_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer itemId;
    
    private Integer restaurantId;
    
    private Integer categoryId;
    
    private String name;
    
    private String description;
    
    private Double price;
    
    private Double discountedPrice;
    
    private String imageUrl;
    
    private Boolean isVeg;
    
    private Boolean isAvailable;
    
    private Double rating;
    
    private Integer calories;
    
    private String tags;
}
