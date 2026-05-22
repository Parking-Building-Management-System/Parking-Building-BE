package com.smartpark.swp391.modules.tenant.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", unique = true, nullable = false, length = 20)
    private String tenantCode; // Mã khách hàng

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName; // Tên khách hàng

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "apartment_number", length = 20)
    private String apartmentNumber; // Số căn hộ/văn phòng thuê

    @Column(name = "status", length = 20)
    private String status; // Trạng thái: ACTIVE, INACTIVE

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); // Tự động lưu ngày giờ tạo khi thêm mới
    }
}