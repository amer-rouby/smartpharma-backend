package com.smartpharma.einvoice.dto.response;

import com.smartpharma.einvoice.entity.EtaPosDevice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtaPosDeviceResponse {
    private Long id;
    private String serialNumber;
    private String osVersion;
    private String modelFramework;
    private boolean active;
    private boolean presharedKeySet;
    // Once a device has issued receipts its serial is locked - the chain is tied to it.
    private boolean hasIssuedReceipts;

    public static EtaPosDeviceResponse fromEntity(EtaPosDevice d) {
        return EtaPosDeviceResponse.builder()
                .id(d.getId())
                .serialNumber(d.getSerialNumber())
                .osVersion(d.getOsVersion())
                .modelFramework(d.getModelFramework())
                .active(Boolean.TRUE.equals(d.getActive()))
                .presharedKeySet(d.getPresharedKeyEncrypted() != null)
                .hasIssuedReceipts(d.getLastReceiptUuid() != null)
                .build();
    }
}
