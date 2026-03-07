package com.smartpharma.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupplierRequest {

    @NotBlank(message = "اسم المورد مطلوب")
    @Size(max = 100, message = "الاسم يجب ألا يتجاوز 100 حرف")
    private String name;

    @Size(max = 50, message = "اسم شخص الاتصال يجب ألا يتجاوز 50 حرف")
    private String contactPerson;

    @Size(max = 20, message = "الهاتف يجب ألا يتجاوز 20 حرف")
    private String phone;

    @Size(max = 100, message = "البريد يجب ألا يتجاوز 100 حرف")
    private String email;

    @Size(max = 255, message = "العنوان يجب ألا يتجاوز 255 حرف")
    private String address;

    @Size(max = 50, message = "المدينة يجب ألا تتجاوز 50 حرف")
    private String city;

    @Size(max = 20, message = "حالة المورد غير صحيحة")
    @Builder.Default
    private String status = "ACTIVE";

    @Size(max = 1000, message = "الملاحظات يجب ألا تتجاوز 1000 حرف")
    private String notes;
}