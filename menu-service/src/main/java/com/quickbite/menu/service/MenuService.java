package com.quickbite.menu.service;

import com.quickbite.menu.entity.MenuCategory;
import com.quickbite.menu.entity.MenuItem;

import java.util.List;
import java.util.Optional;

public interface MenuService {
    
    MenuCategory addCategory(MenuCategory category);
    
    MenuItem addMenuItem(MenuItem item);
    
    List<MenuItem> getMenuByRestaurant(Integer restaurantId);
    
    List<MenuCategory> getCategoriesByRestaurant(Integer restaurantId);
    
    Optional<MenuItem> getItemById(Integer itemId);
    
    MenuItem updateMenuItem(Integer itemId, MenuItem item);

    MenuCategory updateCategory(Integer categoryId, MenuCategory category);
    
    void toggleAvailability(Integer itemId);
    
    void deleteMenuItem(Integer itemId);
    
    void deleteCategory(Integer categoryId);
    
    List<MenuItem> searchMenuItems(String keyword);
    
    List<MenuItem> getVegItems(Integer restaurantId);
}
