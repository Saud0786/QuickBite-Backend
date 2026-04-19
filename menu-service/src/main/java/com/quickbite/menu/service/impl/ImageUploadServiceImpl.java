package com.quickbite.menu.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.quickbite.menu.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadServiceImpl implements ImageUploadService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", "quickbite/menu_" + UUID.randomUUID().toString(),
                    "overwrite", true,
                    "resource_type", "image"
            ));

            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.warn("Cloudinary upload failed, saving record without image: {}", e.getMessage());
            return null;
        }
    }
}
