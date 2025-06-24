-- 사용자 (비밀번호: 1234)
INSERT INTO USERS (id, username, password, name, mfa_enabled, enabled) VALUES
                                                                           (1, 'admin@example.com', '{bcrypt}$2a$10$22n9G82e9Y7jC/qXjW1.0O.Z/l.X.1K.0F/l.X.1K', '총괄 관리자', true, true),
                                                                           (2, 'manager@example.com', '{bcrypt}$2a$10$22n9G82e9Y7jC/qXjW1.0O.Z/l.X.1K.0F/l.X.1K', '김매니저', true, true),
                                                                           (3, 'developer@example.com', '{bcrypt}$2a$10$22n9G82e9Y7jC/qXjW1.0O.Z/l.X.1K.0F/l.X.1K', '박개발', false, true),
                                                                           (4, 'user@example.com', '{bcrypt}$2a$10$22n9G82e9Y7jC/qXjW1.0O.Z/l.X.1K.0F/l.X.1K', '이사용', false, true),
                                                                           (5, 'finance@example.com', '{bcrypt}$2a$10$22n9G82e9Y7jC/qXjW1.0O.Z/l.X.1K.0F/l.X.1K', '최재무', true, true)
ON CONFLICT (id) DO NOTHING;

-- 그룹
INSERT INTO APP_GROUP (group_id, group_name, description) VALUES
                                                              (1, 'ADMIN_GROUP', '시스템 관리자 그룹'),
                                                              (2, 'DEVELOPER_GROUP', '개발자 그룹'),
                                                              (3, 'USER_GROUP', '일반 사용자 그룹'),
                                                              (4, 'FINANCE_GROUP', '재무팀 그룹')
ON CONFLICT (group_id) DO NOTHING;

-- 역할
INSERT INTO ROLE (role_id, role_name, role_desc) VALUES
                                                     (1, 'ROLE_ADMIN', '시스템 전체 관리자 역할'),
                                                     (2, 'ROLE_MANAGER', '팀 관리자 역할'),
                                                     (3, 'ROLE_USER', '일반 사용자 역할'),
                                                     (4, 'ROLE_FINANCE_VIEWER', '재무 정보 조회 역할')
ON CONFLICT (role_id) DO NOTHING;

-- 사용자-그룹 관계
INSERT INTO USER_GROUPS (user_id, group_id) VALUES (1, 1), (2, 2), (3, 2), (4, 3), (5, 4);

-- 그룹-역할 관계
INSERT INTO GROUP_ROLES (group_id, role_id) VALUES (1, 1), (2, 2), (3, 3), (4, 4);

-- 역할 계층 (ADMIN > MANAGER > USER)
INSERT INTO ROLE_HIERARCHY_CONFIG (id, description, hierarchy_string, is_active) VALUES
    (1, '표준 3단계 역할 계층', 'ROLE_ADMIN > ROLE_MANAGER\nROLE_MANAGER > ROLE_USER', true)
ON CONFLICT (id) DO NOTHING;

-- 리소스 워크벤치 테스트용 데이터 --
-- 1. 스캔 후 초기 상태 (정의 필요)
INSERT INTO MANAGED_RESOURCE (id, resource_identifier, resource_type, friendly_name, status) VALUES
    (101, '/api/documents', 'URL', 'getDocumentList', 'NEEDS_DEFINITION')
ON CONFLICT (id) DO NOTHING;

-- 2. 권한은 생성되었으나 정책은 없는 상태
INSERT INTO MANAGED_RESOURCE (id, resource_identifier, resource_type, friendly_name, description, status) VALUES
    (102, 'io.spring.identityadmin.admin.iam.service.impl.DocumentService.getDocumentById(java.lang.Long)', 'METHOD', '특정 문서 조회', 'ID로 특정 문서를 조회하는 핵심 서비스 메서드', 'PERMISSION_CREATED')
ON CONFLICT (id) DO NOTHING;
INSERT INTO PERMISSION (permission_id, permission_name, friendly_name, description, target_type, action_type, managed_resource_id) VALUES
    (102, 'METHOD_IOSPRINGIDENTITYADMIN_COMMON_DOCUMENTSERVICE_GETDOCUMENTBYID_JAVALANGLONG', '특정 문서 조회', 'ID로 특정 문서를 조회하는 핵심 서비스 메서드', 'METHOD', 'EXECUTE', 102)
ON CONFLICT (permission_id) DO NOTHING;

-- 3. 정책까지 모두 연결된 상태
INSERT INTO MANAGED_RESOURCE (id, resource_identifier, resource_type, friendly_name, description, status) VALUES
    (103, '/admin/**', 'URL', '관리자 페이지 접근', '모든 관리자 페이지 접근', 'POLICY_CONNECTED')
ON CONFLICT (id) DO NOTHING;
INSERT INTO PERMISSION (permission_id, permission_name, friendly_name, description, target_type, action_type, managed_resource_id) VALUES
    (103, 'URL_ADMIN', '관리자 페이지 접근', '모든 관리자 페이지 접근', 'URL', 'ANY', 103)
ON CONFLICT (permission_id) DO NOTHING;
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id) VALUES (1, 103);
-- (실제 Policy는 RoleServiceImpl의 이벤트 핸들러가 자동으로 생성해 줄 것이므로 data.sql에서 직접 생성하지 않음)

-- 4. 관리 제외 상태
INSERT INTO MANAGED_RESOURCE (id, resource_identifier, resource_type, friendly_name, status) VALUES
    (104, '/api/internal/health', 'URL', 'Health Check API', 'EXCLUDED')
ON CONFLICT (id) DO NOTHING;

-- Pre/Post 인가 테스트용 정책 데이터 --
INSERT INTO POLICY (id, name, description, effect, priority, friendly_description) VALUES
    (201, 'FINANCE_REPORT_POLICY', '재무팀 문서 접근 정책', 'ALLOW', 500, '(역할(재무팀) 보유) 그리고 (반환된 문서의 소유자가 본인임)')
ON CONFLICT (id) DO NOTHING;

INSERT INTO POLICY_TARGET (id, policy_id, target_type, target_identifier) VALUES
    (201, 201, 'METHOD', 'io.spring.identityadmin.admin.iam.service.impl.DocumentService.getDocumentById(java.lang.Long)')
ON CONFLICT (id) DO NOTHING;

INSERT INTO POLICY_RULE (id, policy_id, description) VALUES
    (201, 201, '재무팀 역할 및 본인 소유 문서 확인 규칙')
ON CONFLICT (id) DO NOTHING;

-- PreAuthorize 조건
INSERT INTO POLICY_CONDITION (id, rule_id, condition_expression, authorization_phase) VALUES
    (201, 201, 'hasAuthority(''ROLE_FINANCE_VIEWER'')', 'PRE_AUTHORIZE')
ON CONFLICT (id) DO NOTHING;
-- PostAuthorize 조건
INSERT INTO POLICY_CONDITION (id, rule_id, condition_expression, authorization_phase) VALUES
    (202, 201, 'returnObject.ownerUsername == authentication.name', 'POST_AUTHORIZE')
ON CONFLICT (id) DO NOTHING;


-- 비즈니스 정책 생성 워크벤치용 메타데이터 --
INSERT INTO BUSINESS_RESOURCE (id, name, resource_type, description) VALUES
                                                                         (1, '인사 정보', 'HR_DATA', '임직원의 개인 정보 및 급여 정보를 포함합니다.'),
                                                                         (2, '재무 보고서', 'FINANCE_REPORT', '회사의 월별, 분기별 재무 상태 보고서입니다.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO BUSINESS_ACTION (id, name, action_type, description) VALUES
                                                                     (1, '조회하기', 'READ', '데이터나 리소스를 읽을 수 있습니다.'),
                                                                     (2, '수정하기', 'WRITE', '데이터나 리소스를 생성하거나 수정할 수 있습니다.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO BUSINESS_RESOURCE_ACTION (business_resource_id, business_action_id, mapped_permission_name) VALUES
                                                                                                            (1, 1, 'HR_DATA_READ'), (1, 2, 'HR_DATA_WRITE'),
                                                                                                            (2, 1, 'FINANCE_REPORT_READ'), (2, 2, 'FINANCE_REPORT_WRITE');

-- 조건 템플릿 (정책 빌더용) --
INSERT INTO CONDITION_TEMPLATE (id, name, spel_template, category, parameter_count, description) VALUES
                                                                                                     (1, '업무 시간 제약', '#isBusinessHours()', '시간 기반', 0, '오전 9시부터 오후 6시 사이에만 접근을 허용합니다.'),
                                                                                                     (2, '사내 IP 대역 접근', 'hasIpAddress(''192.168.1.0/24'')', '위치 기반', 0, '사내 네트워크 IP 주소에서의 접근만 허용합니다.')
ON CONFLICT (id) DO NOTHING;


-- 테스트용 문서 데이터 --
INSERT INTO DOCUMENT (document_id, title, content, owner_username) VALUES
                                                                       (1, '2025년 1분기 영업 비밀 보고서', '1분기 매출은 전년 대비 15% 상승했습니다...', 'manager@example.com'),
                                                                       (2, '개인 연말정산 자료', '2024년 귀속 연말정산 내역입니다.', 'user@example.com'),
                                                                       (3, '재무팀 전용 감사 보고서', '외부 감사법인 최종 보고서입니다.', 'finance@example.com')
ON CONFLICT (document_id) DO NOTHING;

-- status='PERMISSION_CREATED'인 리소스(ID: 102)에 대한 권한
INSERT INTO PERMISSION (permission_id, permission_name, friendly_name, description, target_type, action_type, managed_resource_id) VALUES
    (102, 'METHOD_IOSPRINGIDENTITYADMIN_COMMON_DOCUMENTSERVICE_GETDOCUMENTBYID_JAVALANGLONG', '특정 문서 조회', 'ID로 특정 문서를 조회하는 핵심 서비스 메서드', 'METHOD', 'EXECUTE', 102)
ON CONFLICT (permission_id) DO UPDATE SET
                                          permission_name = EXCLUDED.permission_name,
                                          friendly_name = EXCLUDED.friendly_name,
                                          description = EXCLUDED.description,
                                          target_type = EXCLUDED.target_type,
                                          action_type = EXCLUDED.action_type,
                                          managed_resource_id = EXCLUDED.managed_resource_id;

-- status='POLICY_CONNECTED'인 리소스(ID: 103)에 대한 권한
INSERT INTO PERMISSION (permission_id, permission_name, friendly_name, description, target_type, action_type, managed_resource_id) VALUES
    (103, 'URL_ADMIN', '관리자 페이지 접근', '모든 관리자 페이지 접근', 'URL', 'ANY', 103)
ON CONFLICT (permission_id) DO UPDATE SET
                                          permission_name = EXCLUDED.permission_name,
                                          friendly_name = EXCLUDED.friendly_name,
                                          description = EXCLUDED.description,
                                          target_type = EXCLUDED.target_type,
                                          action_type = EXCLUDED.action_type,
                                          managed_resource_id = EXCLUDED.managed_resource_id;

-- Pre/Post 인가 테스트용 메서드에 대한 권한 (리소스 ID: 201, 가상)
INSERT INTO MANAGED_RESOURCE (id, resource_identifier, resource_type, friendly_name, description, status) VALUES
    (201, 'io.spring.identityadmin.admin.iam.service.impl.DocumentService.updateDocument(java.lang.Long,java.lang.String)', 'METHOD', '문서 업데이트', 'ID와 새로운 내용으로 문서를 업데이트하는 기능', 'POLICY_CONNECTED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO PERMISSION (permission_id, permission_name, friendly_name, description, target_type, action_type, managed_resource_id) VALUES
    (201, 'METHOD_DOCUMENTSERVICE_UPDATEDOCUMENT', '문서 업데이트', '문서 내용을 수정하는 권한', 'METHOD', 'EXECUTE', 201)
ON CONFLICT (permission_id) DO UPDATE SET
                                          permission_name = EXCLUDED.permission_name,
                                          friendly_name = EXCLUDED.friendly_name,
                                          description = EXCLUDED.description,
                                          target_type = EXCLUDED.target_type,
                                          action_type = EXCLUDED.action_type,
                                          managed_resource_id = EXCLUDED.managed_resource_id;


-- ID 시퀀스 수동 업데이트 (PostgreSQL 기준)
SELECT setval('users_id_seq', (SELECT MAX(id) FROM USERS), true);
SELECT setval('app_group_group_id_seq', (SELECT MAX(group_id) FROM APP_GROUP), true);
SELECT setval('role_role_id_seq', (SELECT MAX(role_id) FROM ROLE), true);
SELECT setval('managed_resource_id_seq', (SELECT MAX(id) FROM MANAGED_RESOURCE), true);
SELECT setval('permission_permission_id_seq', (SELECT MAX(permission_id) FROM PERMISSION), true);
SELECT setval('policy_id_seq', (SELECT MAX(id) FROM POLICY), true);
SELECT setval('policy_target_id_seq', (SELECT MAX(id) FROM POLICY_TARGET), true);
SELECT setval('policy_rule_id_seq', (SELECT MAX(id) FROM POLICY_RULE), true);
SELECT setval('policy_condition_id_seq', (SELECT MAX(id) FROM POLICY_CONDITION), true);
SELECT setval('business_resource_id_seq', (SELECT MAX(id) FROM BUSINESS_RESOURCE), true);
SELECT setval('business_action_id_seq', (SELECT MAX(id) FROM BUSINESS_ACTION), true);
SELECT setval('condition_template_id_seq', (SELECT MAX(id) FROM CONDITION_TEMPLATE), true);
SELECT setval('document_document_id_seq', (SELECT MAX(document_id) FROM DOCUMENT), true);

