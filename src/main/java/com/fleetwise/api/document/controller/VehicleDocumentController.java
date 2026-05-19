package com.fleetwise.api.document.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.document.dto.VehicleDocumentResponse;
import com.fleetwise.api.document.entity.VehicleDocument;
import com.fleetwise.api.document.entity.VehicleDocumentType;
import com.fleetwise.api.document.service.VehicleDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VehicleDocumentController {

    private final VehicleDocumentService vehicleDocumentService;

    @Operation(
            summary = "Upload a vehicle document",
            description = "Uploads a document file for a vehicle. Max size is 10 MB.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Document uploaded"),
                    @ApiResponse(responseCode = "400", description = "Invalid file or document type"),
                    @ApiResponse(responseCode = "404", description = "Vehicle not found")
            }
    )
    @PostMapping(
            value = "/api/vehicles/{vehicleId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public VehicleDocumentResponse uploadDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId,
            @Parameter(
                    description = "File to upload",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false)
            VehicleDocumentType documentType
    ) {
        return vehicleDocumentService.upload(
                vehicleId,
                principal.getId(),
                file,
                documentType
        );
    }

    @GetMapping("/api/vehicles/{vehicleId}/documents")
    public List<VehicleDocumentResponse> listDocuments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId
    ) {
        return vehicleDocumentService.list(vehicleId, principal.getId());
    }

    @Operation(
            summary = "Download a document",
            description = "Downloads a vehicle or maintenance document by document ID."
    )
    @GetMapping("/api/vehicle-documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID documentId
    ) {
        VehicleDocument document =
                vehicleDocumentService.getDocument(documentId, principal.getId());

        Resource resource =
                vehicleDocumentService.download(documentId, principal.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        document.getContentType() != null
                                ? document.getContentType()
                                : MediaType.APPLICATION_OCTET_STREAM_VALUE
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getOriginalFileName() + "\""
                )
                .body(resource);
    }

    @Operation(
            summary = "Delete a document",
            description = "Deletes a vehicle or maintenance document and removes the stored file."
    )
    @DeleteMapping("/api/vehicle-documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID documentId
    ) {
        vehicleDocumentService.delete(documentId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Upload a maintenance document",
            description = "Uploads an invoice, report, photo, or related document for a maintenance record.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Document uploaded"),
                    @ApiResponse(responseCode = "400", description = "Invalid file or document type"),
                    @ApiResponse(responseCode = "404", description = "Maintenance record not found")
            }
    )
    @PostMapping(
            value = "/api/maintenance/{maintenanceId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public VehicleDocumentResponse uploadMaintenanceDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID maintenanceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false)
            VehicleDocumentType documentType
    ) {
        return vehicleDocumentService.uploadMaintenanceDocument(
                maintenanceId,
                principal.getId(),
                file,
                documentType
        );
    }

    @GetMapping("/api/maintenance/{maintenanceId}/documents")
    public List<VehicleDocumentResponse> listMaintenanceDocuments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID maintenanceId
    ) {
        return vehicleDocumentService.listMaintenanceDocuments(
                maintenanceId,
                principal.getId()
        );
    }
}