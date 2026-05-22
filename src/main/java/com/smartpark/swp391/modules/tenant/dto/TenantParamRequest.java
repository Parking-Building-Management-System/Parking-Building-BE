package com.smartpark.swp391.modules.tenant.dto;

import lombok.Data;

@Data
public class TenantParamRequest {
    // Các tham số để lọc (Filter)
    private String search;          // Thanh tìm kiếm chung (Tên, mã, SĐT)
    private String apartmentNumber; // Bộ lọc riêng theo số phòng
    private String status;          // Bộ lọc theo trạng thái

    // Các tham số để Phân trang (Pagination) và Sắp xếp (Sort)
    private int page = 0;           // Trang hiện tại muốn xem (mặc định trang đầu tiên là số 0)
    private int size = 10;          // Số dòng dữ liệu hiển thị trên 1 trang
    private String sortBy = "createdAt"; // Sắp xếp theo tên trường nào (mặc định sắp xếp theo ngày tạo)
    private String sortDir = "desc";     // Hướng sắp xếp (desc: mới nhất lên đầu, asc: cũ nhất lên đầu)
}