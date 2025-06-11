package io.spring.identityadmin.domain.dto;
import io.spring.identityadmin.domain.entity.FunctionCatalog;
import lombok.Data;

/**
 * '기능 카탈로그 관리' 화면에서 개별 항목을 업데이트할 때 사용하는 DTO
 */
@Data
public class FunctionCatalogUpdateDto {
    private String friendlyName;
    private String description;
    private FunctionCatalog.CatalogStatus status;
    private Long groupId;
}