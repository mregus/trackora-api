package com.fleetwise.api.document.service;

import com.fleetwise.api.activity.entity.ActivityAction;
import com.fleetwise.api.activity.service.ActivityLogService;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.document.dto.VehicleDocumentResponse;
import com.fleetwise.api.document.entity.VehicleDocument;
import com.fleetwise.api.document.entity.VehicleDocumentType;
import com.fleetwise.api.document.repository.VehicleDocumentRepository;
import com.fleetwise.api.document.storage.DocumentStorageService;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.maintenance.entity.Maintenance;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleDocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository documentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;
    private final DocumentStorageService  documentStorageService;
    private final FleetAccessService fleetAccessService;

    @Value("${fleetwise.uploads-dir:uploads}")
    private String uploadsDir;

    private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp",
            ".gif"
    );

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "text/plain",
            "text/csv",
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    @Transactional
    public VehicleDocumentResponse upload(
            UUID vehicleId,
            UUID ownerId,
            MultipartFile file,
            VehicleDocumentType documentType
    ) {
        Vehicle vehicle = vehicleRepository.findByIdAndFleetOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateWriteAccess(vehicle.getFleet().getId(), ownerId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 10 MB limit");
        }

        validateAllowedFileType(file);

        if (documentType == VehicleDocumentType.PHOTO) {
            validateImageFile(file);
        }

        VehicleDocumentType resolvedType =
                documentType != null
                        ? documentType
                        : VehicleDocumentType.GENERAL;

        if (resolvedType == VehicleDocumentType.PHOTO) {
            validateImageFile(file);
        }

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return saveDocument(
                user,
                vehicle,
                null,
                file,
                resolvedType
        );
    }

    @Transactional
    public VehicleDocumentResponse uploadMaintenanceDocument(
            UUID maintenanceId,
            UUID ownerId,
            MultipartFile file,
            VehicleDocumentType documentType
    ) {

        Maintenance maintenance =
                maintenanceRepository.findByIdAndVehicleFleetOwnerId(maintenanceId, ownerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Maintenance not found"));

        Vehicle vehicle = maintenance.getVehicle();

        fleetAccessService.validateWriteAccess(vehicle.getFleet().getId(), ownerId);

        if (!vehicle.getFleet().getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Maintenance not found");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        validateAllowedFileType(file);

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 10 MB limit");
        }

        VehicleDocumentType resolvedType =
                documentType != null
                        ? documentType
                        : VehicleDocumentType.GENERAL;

        if (resolvedType == VehicleDocumentType.PHOTO) {
            validateImageFile(file);
        }

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return saveDocument(
                user,
                vehicle,
                maintenance,
                file,
                resolvedType
        );
    }

    @Transactional(readOnly = true)
    public List<VehicleDocumentResponse> list(UUID vehicleId, UUID ownerId) {

        var vehicle = vehicleRepository.findByIdAndFleetOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateAccess(vehicle.getFleet().getId(), ownerId);

        return documentRepository.findByVehicleIdAndMaintenanceIsNullOrderByCreatedAtDesc(vehicleId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleDocumentResponse> listMaintenanceDocuments(
            UUID maintenanceId,
            UUID ownerId
    ) {

        Maintenance maintenance =
                maintenanceRepository.findByIdAndVehicleFleetOwnerId(maintenanceId, ownerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Maintenance not found"));

        if (!maintenance.getVehicle().getFleet().getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Maintenance not found");
        }

        fleetAccessService.validateAccess(maintenance.getVehicle().getFleet().getId(), ownerId);

        return documentRepository
                .findByMaintenanceIdOrderByCreatedAtDesc(maintenanceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleDocument getDocument(UUID documentId, UUID ownerId) {
        return documentRepository.findByIdAndVehicleFleetOwnerId(documentId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    @Transactional(readOnly = true)
    public Resource download(UUID documentId, UUID ownerId) {
        VehicleDocument document = getDocument(documentId, ownerId);

        return new InputStreamResource(
                documentStorageService.download(document.getStoragePath())
        );
    }

    @Transactional
    public void delete(UUID documentId, UUID ownerId) {
        VehicleDocument document = getDocument(documentId, ownerId);

        try {
            documentStorageService.delete(document.getStoragePath());
        } catch (Exception ignored) {
        }

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        activityLogService.log(
                user,
                document.getVehicle(),
                ActivityAction.DOCUMENT_DELETED,
                "DOCUMENT",
                document.getId(),
                "Deleted document %s".formatted(document.getOriginalFileName())
        );

        documentRepository.delete(document);
    }

    private VehicleDocumentResponse toResponse(VehicleDocument document) {
        return new VehicleDocumentResponse(
                document.getId(),
                document.getVehicle().getId(),
                document.getMaintenance() != null
                        ? document.getMaintenance().getId()
                        : null,
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getDocumentType(),
                document.getCreatedAt()
        );
    }

    private void validateImageFile(MultipartFile file) {

        String contentType = file.getContentType();

        String fileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase()
                : "";

        boolean validContentType =
                contentType != null
                        && ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType);

        boolean validExtension =
                ALLOWED_IMAGE_EXTENSIONS.stream()
                        .anyMatch(fileName::endsWith);

        if (!validContentType && !validExtension) {
            throw new IllegalArgumentException(
                    "PHOTO documents must be valid image files"
            );
        }
    }

    private VehicleDocumentResponse saveDocument(
            User user,
            Vehicle vehicle,
            Maintenance maintenance,
            MultipartFile file,
            VehicleDocumentType documentType
    ) {
        try {
            Files.createDirectories(Path.of(uploadsDir));

            String originalFileName = sanitizeFileName(file.getOriginalFilename());

            if (originalFileName.length() > 200) {
                originalFileName = originalFileName.substring(originalFileName.length() - 200);
            }

            String objectKey = buildObjectKey(
                    vehicle,
                    maintenance,
                    originalFileName
            );

            String storagePath = documentStorageService.upload(file, objectKey);

            VehicleDocument document = VehicleDocument.builder()
                    .vehicle(vehicle)
                    .maintenance(maintenance)
                    .originalFileName(originalFileName)
                    .storedFileName(objectKey)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .storagePath(storagePath)
                    .documentType(documentType)
                    .build();

            VehicleDocument saved = documentRepository.save(document);

            activityLogService.log(
                    user,
                    vehicle,
                    ActivityAction.DOCUMENT_UPLOADED,
                    "DOCUMENT",
                    saved.getId(),
                    "Uploaded document %s".formatted(saved.getOriginalFileName())
            );

            return toResponse(saved);

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document", e);
        }
    }

    private String buildObjectKey(
            Vehicle vehicle,
            Maintenance maintenance,
            String originalFileName
    ) {
        String prefix = maintenance != null
                ? "maintenance/%s".formatted(maintenance.getId())
                : "vehicles/%s".formatted(vehicle.getId());

        return "%s/%s-%s".formatted(
                prefix,
                UUID.randomUUID(),
                originalFileName
        );
    }

    private void validateAllowedFileType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type");
        }
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "upload";
        }

        String fileName = Path.of(originalFileName)
                .getFileName()
                .toString();

        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}