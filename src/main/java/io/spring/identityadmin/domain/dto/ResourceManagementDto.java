package io.spring.identityadmin.domain.dto;

import lombok.Data;

/**
 * [신규 DTO]
 * 리소스의 관리 상태(예: 워크벤치 표시 여부)만을 업데이트하기 위한 DTO.
 */
@Data
public class ResourceManagementDto {
    private boolean isManaged;
}