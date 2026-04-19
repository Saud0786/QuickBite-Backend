package com.quickbite.menu.repository;

import com.quickbite.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    
    List<MenuItem> findByRestaurantId(Integer restaurantId);
    
    List<MenuItem> findByCategoryId(Integer categoryId);
    
    List<MenuItem> findByIsVeg(Boolean isVeg);
    
    List<MenuItem> findByPriceLessThanEqual(Double price);
    
    List<MenuItem> findByIsAvailable(Boolean isAvailable);
    
    long countByRestaurantId(Integer restaurantId);

    @Query("SELECT m FROM MenuItem m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(m.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<MenuItem> searchByName(@Param("keyword") String keyword);
}
