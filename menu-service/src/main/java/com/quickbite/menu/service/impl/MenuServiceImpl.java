package com.quickbite.menu.service.impl;

import com.quickbite.menu.entity.MenuCategory;
import com.quickbite.menu.entity.MenuItem;
import com.quickbite.menu.repository.MenuCategoryRepository;
import com.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuServiceImpl implements MenuService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    @Override
    public MenuCategory addCategory(MenuCategory category) {
        return menuCategoryRepository.save(category);
    }

    @Override
    public MenuItem addMenuItem(MenuItem item) {
        if (item.getIsAvailable() == null) item.setIsAvailable(true);
        if (item.getIsVeg() == null) item.setIsVeg(false);
        return menuItemRepository.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItem> getMenuByRestaurant(Integer restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuCategory> getCategoriesByRestaurant(Integer restaurantId) {
        return menuCategoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MenuItem> getItemById(Integer itemId) {
        return menuItemRepository.findById(itemId);
    }

    @Override
    public MenuItem updateMenuItem(Integer itemId, MenuItem item) {
        MenuItem existing = menuItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found"));
        
        if (item.getRestaurantId() != null) existing.setRestaurantId(item.getRestaurantId());
        if (item.getCategoryId() != null) existing.setCategoryId(item.getCategoryId());
        if (item.getName() != null) existing.setName(item.getName());
        if (item.getDescription() != null) existing.setDescription(item.getDescription());
        if (item.getPrice() != null) existing.setPrice(item.getPrice());
        if (item.getDiscountedPrice() != null) existing.setDiscountedPrice(item.getDiscountedPrice());
        if (item.getImageUrl() != null) existing.setImageUrl(item.getImageUrl());
        if (item.getIsVeg() != null) existing.setIsVeg(item.getIsVeg());
        if (item.getIsAvailable() != null) existing.setIsAvailable(item.getIsAvailable());
        if (item.getCalories() != null) existing.setCalories(item.getCalories());
        if (item.getTags() != null) existing.setTags(item.getTags());
        
        return menuItemRepository.save(existing);
    }

    @Override
    public MenuCategory updateCategory(Integer categoryId, MenuCategory category) {
        MenuCategory existing = menuCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.getRestaurantId() != null) existing.setRestaurantId(category.getRestaurantId());
        if (category.getName() != null) existing.setName(category.getName());
        if (category.getDescription() != null) existing.setDescription(category.getDescription());
        if (category.getImageUrl() != null) existing.setImageUrl(category.getImageUrl());
        if (category.getDisplayOrder() != null) existing.setDisplayOrder(category.getDisplayOrder());

        return menuCategoryRepository.save(existing);
    }

    @Override
    public void toggleAvailability(Integer itemId) {
        MenuItem existing = menuItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found"));
        existing.setIsAvailable(!existing.getIsAvailable());
        menuItemRepository.save(existing);
    }

    @Override
    public void deleteMenuItem(Integer itemId) {
        menuItemRepository.deleteById(itemId);
    }

    @Override
    public void deleteCategory(Integer categoryId) {
        menuCategoryRepository.deleteById(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItem> searchMenuItems(String keyword) {
        return menuItemRepository.searchByName(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItem> getVegItems(Integer restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId)
            .stream()
            .filter(item -> Boolean.TRUE.equals(item.getIsVeg()))
            .collect(Collectors.toList());
    }
}
