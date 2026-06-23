package com.health.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.health.admin.AdminAuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:uploads;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class ImageUploadControllerTest {
    private static final Path uploadDirectory = createUploadDirectory();

    @Autowired
    MockMvc mockMvc;

    @DynamicPropertySource
    static void uploadProperties(DynamicPropertyRegistry registry) {
        registry.add("health.upload.directory", () -> uploadDirectory.toString());
    }

    @Test
    void uploadsAndServesValidPng() throws Exception {
        String authorization = AdminAuthTestSupport.adminBearerToken(mockMvc);
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", pngBytes());

        String response = mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(file)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url", startsWith("/uploads/")))
                .andReturn().getResponse().getContentAsString();

        String url = response.replaceFirst(".*\"url\"\s*:\s*\"([^\"]+)\".*", "$1");
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void rejectsUnsupportedOrForgedImages() throws Exception {
        String authorization = AdminAuthTestSupport.adminBearerToken(mockMvc);

        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes()))
                        .header("Authorization", authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("仅支持 JPEG、PNG 或 WebP 图片"));

        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(new MockMultipartFile("file", "fake.png", "image/png", "not-an-image".getBytes()))
                        .header("Authorization", authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("图片内容与格式不匹配"));
    }

    @Test
    void rejectsUnauthenticatedUpload() throws Exception {
        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(new MockMultipartFile("file", "avatar.png", "image/png", pngBytes())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsImagesLargerThanFiveMegabytes() throws Exception {
        String authorization = AdminAuthTestSupport.adminBearerToken(mockMvc);
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(pngBytes(), 0, oversized, 0, pngBytes().length);

        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(new MockMultipartFile("file", "large.png", "image/png", oversized))
                        .header("Authorization", authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("图片大小不能超过5MB"));
    }

    private byte[] pngBytes() {
        return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
    }

    private static Path createUploadDirectory() {
        try {
            return Files.createTempDirectory("health-upload-test-");
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建测试上传目录", exception);
        }
    }
}
