package com.health.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import com.health.common.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final UploadProperties properties;

    public ImageStorageService(UploadProperties properties) {
        this.properties = properties;
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("图片大小不能超过5MB");
        }
        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new BadRequestException("仅支持 JPEG、PNG 或 WebP 图片");
        }

        try {
            byte[] header = readHeader(file);
            if (!matchesContentType(contentType, header)) {
                throw new BadRequestException("图片内容与格式不匹配");
            }
            Path directory = Path.of(properties.getDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + extension;
            Path target = directory.resolve(filename).normalize();
            if (!target.getParent().equals(directory)) {
                throw new BadRequestException("图片保存路径不合法");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return properties.normalizedPublicPrefix() + filename;
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequestException("图片保存失败");
        }
    }

    private byte[] readHeader(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(12);
        }
    }

    private boolean matchesContentType(String contentType, byte[] header) {
        return switch (contentType) {
            case "image/png" -> header.length >= 8
                    && (header[0] & 0xff) == 0x89 && header[1] == 0x50 && header[2] == 0x4e
                    && header[3] == 0x47 && header[4] == 0x0d && header[5] == 0x0a
                    && header[6] == 0x1a && header[7] == 0x0a;
            case "image/jpeg" -> header.length >= 3
                    && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
            case "image/webp" -> header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }
}
