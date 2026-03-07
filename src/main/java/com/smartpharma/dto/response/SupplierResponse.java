package com.smartpharma.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupplierResponse {

    private Long id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String status;
    private String notes;
    private Long pharmacyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupplierResponse fromEntity(com.smartpharma.entity.Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .city(supplier.getCity())
                .status(supplier.getStatus())
                .notes(supplier.getNotes())
                .pharmacyId(supplier.getPharmacy().getId())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}