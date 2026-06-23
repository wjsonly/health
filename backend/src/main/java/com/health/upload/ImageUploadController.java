package com.health.upload;

import com.health.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ImageUploadController {
    private final ImageStorageService storageService;

    public ImageUploadController(ImageStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/api/admin/uploads/images")
    public ApiResponse<ImageUploadResponse> uploadImage(@RequestParam MultipartFile file) {
        return ApiResponse.ok(new ImageUploadResponse(storageService.store(file)));
    }

    record ImageUploadResponse(String url) {
    }
}
