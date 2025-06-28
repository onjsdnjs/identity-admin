package io.spring.iam.aiam.labs;

/**
 * 🚀 완전 제네릭 Lab 시스템 사용법 가이드
 * 
 * ===============================================
 * 🎯 Lab 추가시 더 이상 메서드 추가가 불필요합니다!
 * ===============================================
 * 
 * 📋 1. 새로운 Lab 생성
 * ==================
 * 
 * @Component
 * public class MyAwesomeLab {
 *     // Lab 구현...
 * }
 * 
 * 주의사항:
 * - 클래스명이 "Lab"으로 끝나야 함 (자동 감지)
 * - @Component 어노테이션 필수
 * 
 * 📋 2. Lab 사용 방법들
 * ===================
 * 
 * 🔧 방법 1: 제네릭 타입 기반 접근 (권장)
 * ----------------------------------------
 * 
 * @Autowired
 * private LabAccessor labAccessor;
 * 
 * // 타입 안전한 Lab 조회
 * Optional<MyAwesomeLab> lab = labAccessor.getLab(MyAwesomeLab.class);
 * if (lab.isPresent()) {
 *     String result = lab.get().doSomething();
 * }
 * 
 * 🔧 방법 2: 함수형 접근 (가장 간단)
 * --------------------------------
 * 
 * // Lab 조회 + 실행을 한 번에
 * Optional<String> result = labAccessor.withLab(
 *     MyAwesomeLab.class,
 *     lab -> lab.doSomething()
 * );
 * 
 * 🔧 방법 3: 클래스 이름 기반 동적 접근
 * ------------------------------------
 * 
 * // 타입 안전한 클래스 이름 접근
 * Optional<MyAwesomeLab> lab = labAccessor.getLabByClassName(
 *     "MyAwesomeLab", 
 *     MyAwesomeLab.class
 * );
 * 
 * // 타입 정보 없는 동적 접근
 * Optional<Object> lab = labAccessor.getLabByClassName("MyAwesomeLab");
 * 
 * 📋 3. Lab 존재 여부 확인
 * ======================
 * 
 * // 타입으로 확인
 * boolean exists = labAccessor.hasLab(MyAwesomeLab.class);
 * 
 * // 클래스 이름으로 확인
 * boolean exists = labAccessor.hasLab("MyAwesomeLab");
 * 
 * 📋 4. 실제 사용 예제 (Strategy 패턴)
 * =================================
 * 
 * @Component
 * public class MyDiagnosisStrategy implements DiagnosisStrategy<IAMContext, IAMResponse> {
 *     
 *     private final LabAccessor labAccessor;
 *     
 *     public MyDiagnosisStrategy(LabAccessor labAccessor) {
 *         this.labAccessor = labAccessor;
 *     }
 *     
 *     @Override
 *     public IAMResponse execute(IAMRequest<IAMContext> request, Class<IAMResponse> responseType) {
 *         // 방법 1: 전통적 방식
 *         Optional<MyAwesomeLab> labOpt = labAccessor.getLab(MyAwesomeLab.class);
 *         if (labOpt.isEmpty()) {
 *             throw new DiagnosisException("MY_DIAGNOSIS", "LAB_NOT_FOUND", 
 *                 "MyAwesomeLab을 찾을 수 없습니다");
 *         }
 *         String result = labOpt.get().doSomething();
 *         
 *         // 방법 2: 함수형 방식 (더 간단)
 *         Optional<String> result = labAccessor.withLab(
 *             MyAwesomeLab.class,
 *             lab -> lab.doSomething()
 *         );
 *         
 *         return createResponse(result);
 *     }
 * }
 * 
 * 📋 5. Lab 관리 및 모니터링
 * ========================
 * 
 * @Autowired
 * private IAMLabRegistry labRegistry;
 * 
 * // 모든 Lab 목록 조회
 * List<String> labNames = labRegistry.getAllLabNames();
 * 
 * // Lab 통계 정보
 * Map<String, Object> stats = labRegistry.getLabStatistics();
 * 
 * // 특정 타입의 Lab들 조회
 * List<MyAwesomeLab> myLabs = labRegistry.getLabsByType(MyAwesomeLab.class);
 * 
 * 📋 6. 장점 요약
 * =============
 * 
 * ✅ Lab 추가시 메서드 추가 불필요
 * ✅ 타입 안전성 보장
 * ✅ 컴파일 타임 타입 체크
 * ✅ 런타임 동적 조회 지원
 * ✅ 함수형 프로그래밍 지원
 * ✅ 느슨한 결합 (Loose Coupling)
 * ✅ 확장성 (Scalability)
 * ✅ 유지보수성 (Maintainability)
 * 
 * 📋 7. 마이그레이션 가이드
 * =======================
 * 
 * 기존 코드:
 * ----------
 * private final MyAwesomeLab myAwesomeLab;
 * 
 * public MyStrategy(MyAwesomeLab myAwesomeLab) {
 *     this.myAwesomeLab = myAwesomeLab;
 * }
 * 
 * String result = myAwesomeLab.doSomething();
 * 
 * 새로운 코드:
 * -----------
 * private final LabAccessor labAccessor;
 * 
 * public MyStrategy(LabAccessor labAccessor) {
 *     this.labAccessor = labAccessor;
 * }
 * 
 * // 방법 1: 안전한 방식
 * Optional<MyAwesomeLab> labOpt = labAccessor.getLab(MyAwesomeLab.class);
 * if (labOpt.isPresent()) {
 *     String result = labOpt.get().doSomething();
 * }
 * 
 * // 방법 2: 함수형 방식 (권장)
 * Optional<String> result = labAccessor.withLab(
 *     MyAwesomeLab.class,
 *     lab -> lab.doSomething()
 * );
 * 
 * 🎉 이제 Lab을 마음껏 추가하세요! 코드 수정은 더 이상 필요 없습니다! 🎉
 */
public class LabUsageGuide {
    // 이 클래스는 문서화 목적으로만 사용됩니다
} 