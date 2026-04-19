package com.quickbite.menu.controller;

import com.quickbite.menu.dto.ApiResponse;
import com.quickbite.menu.dto.CategoryRequest;
import com.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.menu.entity.MenuCategory;
import com.quickbite.menu.entity.MenuItem;
import com.quickbite.menu.service.ImageUploadService;
import com.quickbite.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuResource {

    private final MenuService menuService;
    private final ImageUploadService imageUploadService;

    @PostMapping(value = "/category", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuCategory>> addCategory(@ModelAttribute CategoryRequest request) {
        String uploadedUrl = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            uploadedUrl = imageUploadService.uploadImage(request.getImage());
        }

        MenuCategory category = MenuCategory.builder()
                .restaurantId(request.getRestaurantId())
                .name(request.getName())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .imageUrl(uploadedUrl)
                .build();

        return ResponseEntity.ok(ApiResponse.success(menuService.addCategory(category), "Category added"));
    }

    @PutMapping(value = "/category/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuCategory>> updateCategory(@PathVariable Integer categoryId, @ModelAttribute CategoryRequest request) {
        String uploadedUrl = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            uploadedUrl = imageUploadService.uploadImage(request.getImage());
        }

        MenuCategory category = MenuCategory.builder()
                .restaurantId(request.getRestaurantId())
                .name(request.getName())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder())
                .imageUrl(uploadedUrl)
                .build();

        return ResponseEntity.ok(ApiResponse.success(menuService.updateCategory(categoryId, category), "Category updated"));
    }

    @PutMapping(value = "/category/{categoryId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuCategory>> updateCategoryJson(@PathVariable Integer categoryId, @RequestBody MenuCategory category) {
        return ResponseEntity.ok(ApiResponse.success(menuService.updateCategory(categoryId, category), "Category updated"));
    }

    @PostMapping(value = "/item", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItem>> addMenuItem(@ModelAttribute MenuItemRequest request) {
        String uploadedUrl = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            uploadedUrl = imageUploadService.uploadImage(request.getImage());
        }

        MenuItem item = MenuItem.builder()
                .restaurantId(request.getRestaurantId())
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .isVeg(request.getIsVeg())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .imageUrl(uploadedUrl)
                .build();

        return ResponseEntity.ok(ApiResponse.success(menuService.addMenuItem(item), "Item added"));
    }

    @PutMapping(value = "/item/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItem>> updateMenuItem(@PathVariable Integer itemId, @ModelAttribute MenuItemRequest request) {
        String uploadedUrl = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            uploadedUrl = imageUploadService.uploadImage(request.getImage());
        }

        MenuItem item = MenuItem.builder()
                .restaurantId(request.getRestaurantId())
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .isVeg(request.getIsVeg())
                .isAvailable(request.getIsAvailable())
                .imageUrl(uploadedUrl)
                .build();

        return ResponseEntity.ok(ApiResponse.success(menuService.updateMenuItem(itemId, item), "Item updated"));
    }

    @PutMapping(value = "/item/{itemId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItem>> updateMenuItemJson(@PathVariable Integer itemId, @RequestBody MenuItem item) {
        return ResponseEntity.ok(ApiResponse.success(menuService.updateMenuItem(itemId, item), "Item updated"));
    }

    @GetMapping("/restaurant/{restaurantId}/items")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getMenuByRestaurant(@PathVariable Integer restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenuByRestaurant(restaurantId), "Menu items fetched"));
    }

    @GetMapping("/restaurant/{restaurantId}/categories")
    public ResponseEntity<ApiResponse<List<MenuCategory>>> getCategoriesByRestaurant(@PathVariable Integer restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getCategoriesByRestaurant(restaurantId), "Categories fetched"));
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<MenuItem>> getItemById(@PathVariable Integer itemId) {
        return menuService.getItemById(itemId)
                .map(item -> ResponseEntity.ok(ApiResponse.success(item, "Item fetched")))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/item/{itemId}/toggle")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleAvailability(@PathVariable Integer itemId) {
        menuService.toggleAvailability(itemId);
        return ResponseEntity.ok(ApiResponse.success(null, "Availability toggled"));
    }

    @DeleteMapping("/item/{itemId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable Integer itemId) {
        menuService.deleteMenuItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(null, "Item deleted"));
    }

    @DeleteMapping("/category/{categoryId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Integer categoryId) {
        menuService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MenuItem>>> searchMenuItems(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(menuService.searchMenuItems(keyword), "Search results"));
    }

    @GetMapping("/restaurant/{restaurantId}/veg")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getVegItems(@PathVariable Integer restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getVegItems(restaurantId), "Veg items fetched"));
    }
}
