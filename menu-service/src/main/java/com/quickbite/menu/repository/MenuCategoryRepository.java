package com.quickbite.menu.repository;

import com.quickbite.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Integer> {
    
    List<MenuCategory> findByRestaurantIdOrderByDisplayOrderAsc(Integer restaurantId);
    
}
