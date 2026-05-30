package com.fleetwise.api.document.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface DocumentStorageService {

    String upload(MultipartFile file, String objectKey);

    InputStream download(String objectKey);

    void delete(String objectKey);
}