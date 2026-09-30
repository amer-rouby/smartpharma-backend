package com.smartpharma.einvoice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Field limits are ETA's POS authentication header limits.
// presharedKey: required when creating (checked in the service, since the same
// DTO serves updates), null keeps the stored one on update.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtaPosDeviceRequest {

    @NotBlank(message = "Serial number is required")
    @Size(max = 100)
    private String serialNumber;

    @NotBlank(message = "OS version is required")
    @Size(max = 50)
    private String osVersion;

    @NotBlank(message = "Model framework is required")
    @Size(max = 10)
    private String modelFramework;

    @Size(max = 200)
    private String presharedKey;

    private Boolean active;
}
