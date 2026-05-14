package com.smartpark.swp391.modules.identity.mapper;

import com.smartpark.swp391.modules.identity.dto.tenant.response.TenantResponse;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

// componentModel = "spring" để MapStruct tự biến class này thành @Component, có thể Inject được
// unmappedTargetPolicy = IGNORE để tránh cảnh báo khi entity có nhiều field hơn DTO
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {
  TenantResponse toResponse(Tenant tenant);
}
