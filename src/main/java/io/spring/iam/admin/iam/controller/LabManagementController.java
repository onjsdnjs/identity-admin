/*
package io.spring.iam.admin.iam.controller;

import io.spring.iam.aiam.labs.LabAccessor;
import io.spring.iam.aiam.labs.condition.ConditionTemplateGenerationLab;
import io.spring.iam.aiam.labs.policy.AdvancedPolicyGenerationLab;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

*/
/**
 * 🏛️ Lab 관리 컨트롤러
 * 
 * 동적 Lab 시스템의 활용법을 보여주는 관리 API
 * - Lab 목록 조회
 * - Lab 상태 확인
 * - Lab 통계 정보
 * - 동적 Lab 접근 예제
 *//*

@Slf4j
@RestController
@RequestMapping("/api/admin/labs")
@RequiredArgsConstructor
public class LabManagementController {
    
    private final LabAccessor labAccessor;

    */
/**
     * 모든 등록된 Lab 목록 조회
     * @return Lab 목록
     *//*

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getLabList() {
        log.info("🔍 Lab 목록 조회 요청");
        
        Map<String, Object> response = new HashMap<>();
        response.put("labNames", labAccessor.getLabRegistry().getAllLabNames());
        response.put("labCount", labAccessor.getLabRegistry().getLabCount());
        response.put("labStatus", labAccessor.getLabRegistry().getLabStatus());
        
        return ResponseEntity.ok(response);
    }
    
    */
/**
     * Lab 통계 정보 조회
     * @return Lab 통계
     *//*

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLabStatistics() {
        log.info("📊 Lab 통계 정보 조회 요청");
        return ResponseEntity.ok(labAccessor.getLabRegistry().getLabStatistics());
    }
    
    */
/**
     * 특정 Lab 존재 여부 확인 (클래스 이름으로)
     * @param className Lab 클래스 이름
     * @return 존재 여부
     *//*

    @GetMapping("/check/{className}")
    public ResponseEntity<Map<String, Object>> checkLabExistence(@PathVariable String className) {
        log.info("🔍 Lab 존재 여부 확인: {}", className);
        
        boolean exists = labAccessor.hasLab(className);
        
        Map<String, Object> response = new HashMap<>();
        response.put("className", className);
        response.put("exists", exists);
        response.put("message", exists ? "Lab이 등록되어 있습니다" : "Lab을 찾을 수 없습니다");
        
        return ResponseEntity.ok(response);
    }
    
    */
/**
     * 동적 Lab 접근 예제 - 조건 템플릿 생성
     * @return 조건 템플릿 생성 결과
     *//*

    @PostMapping("/example/condition-template")
    public ResponseEntity<Map<String, Object>> exampleConditionTemplate() {
        log.info("🔬 동적 Lab 접근 예제 - 조건 템플릿 생성");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 완전 제네릭 방식으로 Lab 접근
            Optional<ConditionTemplateGenerationLab> labOpt = labAccessor.getLab(ConditionTemplateGenerationLab.class);
            
            if (labOpt.isPresent()) {
                ConditionTemplateGenerationLab lab = labOpt.get();
                String templates = lab.generateUniversalConditionTemplates();
                
                response.put("success", true);
                response.put("method", "완전 제네릭 LabAccessor");
                response.put("templates", templates);
                response.put("message", "조건 템플릿 생성 성공");
            } else {
                response.put("success", false);
                response.put("message", "ConditionTemplateGenerationLab을 찾을 수 없습니다");
            }
            
        } catch (Exception e) {
            log.error("🔥 조건 템플릿 생성 실패", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    */
/**
     * 함수형 접근 방식 예제
     * @return 함수형 실행 결과
     *//*

    @PostMapping("/example/functional")
    public ResponseEntity<Map<String, Object>> exampleFunctionalAccess() {
        log.info("🔧 함수형 Lab 접근 예제");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 함수형 방식으로 Lab 실행
            Optional<String> result = labAccessor.withLab(
                ConditionTemplateGenerationLab.class,
                lab -> lab.generateUniversalConditionTemplates()
            );
            
            if (result.isPresent()) {
                response.put("success", true);
                response.put("method", "함수형 Lab 접근");
                response.put("templates", result.get());
                response.put("message", "함수형 실행 성공");
            } else {
                response.put("success", false);
                response.put("message", "Lab을 찾을 수 없거나 실행 실패");
            }
            
        } catch (Exception e) {
            log.error("🔥 함수형 Lab 접근 실패", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    */
/**
     * 동적 Lab 접근 예제 - 클래스 이름으로 접근
     * @param className Lab 클래스 이름
     * @return Lab 정보
     *//*

    @GetMapping("/example/dynamic/{className}")
    public ResponseEntity<Map<String, Object>> exampleDynamicAccess(@PathVariable String className) {
        log.info("🔧 동적 Lab 접근 예제 - 클래스 이름: {}", className);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 방법 2: 클래스 이름을 통한 동적 접근
            Optional<Object> labOpt = labAccessor.getLabRegistry().getLabByClassName(className);
            
            if (labOpt.isPresent()) {
                Object lab = labOpt.get();
                
                response.put("success", true);
                response.put("method", "클래스 이름 기반 동적 접근");
                response.put("className", className);
                response.put("actualClass", lab.getClass().getName());
                response.put("package", lab.getClass().getPackage().getName());
                response.put("message", "Lab 조회 성공");
                
                // 특정 Lab 타입에 따른 추가 작업 예제
                if (lab instanceof ConditionTemplateGenerationLab conditionLab) {
                    response.put("labType", "ConditionTemplateGenerationLab");
                    response.put("capability", "조건 템플릿 생성");
                } else if (lab instanceof AdvancedPolicyGenerationLab policyLab) {
                    response.put("labType", "AdvancedPolicyGenerationLab");
                    response.put("capability", "고급 정책 생성");
                } else {
                    response.put("labType", "Unknown Lab Type");
                    response.put("capability", "알 수 없는 기능");
                }
                
            } else {
                response.put("success", false);
                response.put("className", className);
                response.put("message", "해당 클래스 이름의 Lab을 찾을 수 없습니다");
            }
            
        } catch (Exception e) {
            log.error("🔥 동적 Lab 접근 실패: {}", className, e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    */
/**
     * 모든 Lab 타입별 분류 조회
     * @return Lab 타입별 분류
     *//*

    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getLabsByTypes() {
        log.info("📋 Lab 타입별 분류 조회");
        
        Map<String, Object> response = new HashMap<>();
        
        // 조건 템플릿 Lab들
        var conditionLabs = labAccessor.getLabRegistry().getLabsByType(ConditionTemplateGenerationLab.class);
        response.put("conditionTemplateLabs", conditionLabs.stream()
            .map(lab -> lab.getClass().getSimpleName())
            .toList());
        
        // 정책 생성 Lab들
        var policyLabs = labAccessor.getLabRegistry().getLabsByType(AdvancedPolicyGenerationLab.class);
        response.put("policyGenerationLabs", policyLabs.stream()
            .map(lab -> lab.getClass().getSimpleName())
            .toList());
        
        // 전체 Lab들
        response.put("allLabs", labAccessor.getLabRegistry().getAllLabNames());
        response.put("totalCount", labAccessor.getLabRegistry().getLabCount());
        
        // Lab 존재 여부 확인 예제
        response.put("labExistence", Map.of(
            "ConditionTemplateGenerationLab", labAccessor.hasLab(ConditionTemplateGenerationLab.class),
            "AdvancedPolicyGenerationLab", labAccessor.hasLab(AdvancedPolicyGenerationLab.class),
            "NonExistentLab", labAccessor.hasLab("NonExistentLab")
        ));
        
        return ResponseEntity.ok(response);
    }
} */
