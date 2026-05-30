package com.fleetwise.api.document.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "storage.provider",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalDocumentStorageService implements DocumentStorageService {

    @Value("${fleetwise.uploads-dir:uploads}")
    private String uploadsDir;

    @Override
    public String upload(MultipartFile file, String objectKey) {
        try {
            Path storagePath = Path.of(uploadsDir, objectKey);
            Files.createDirectories(storagePath.getParent());

            Files.copy(file.getInputStream(), storagePath);

            return storagePath.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document locally", e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            return Files.newInputStream(Path.of(objectKey));
        } catch (Exception e) {
            throw new RuntimeException("Failed to download document locally", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(Path.of(objectKey));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete document locally", e);
        }
    }
}