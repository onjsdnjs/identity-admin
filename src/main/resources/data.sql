-- 사용자 (비밀번호: 1234)
INSERT INTO USERS (id, username, password, name, mfa_enabled, enabled) VALUES
                                                                           (1, 'admin@example.com', '{bcrypt}$2a$10$22n9G82e9Y7jC/qXjW1.0O.Z/l.X.1K.0F/l.X.1K', '총괄 관리자', true, true),
                                                                           (2, 'user@example.com', '{bcrypt}$2a$10$22n9G82e9Y7jC/qXjW1.0O.Z/l.X.1K.0F/l.X.1K', '일반 사용자', false, true);

-- 그룹
INSERT INTO APP_GROUP (group_id, group_name, description) VALUES
                                                              (1, 'ADMIN_GROUP', '시스템 관리자 그룹'),
                                                              (2, 'DEVELOPER_GROUP', '개발자 그룹'),
                                                              (3, 'USER_GROUP', '일반 사용자 그룹');

-- 역할
INSERT INTO ROLE (role_id, role_name, role_desc) VALUES
                                                     (1, 'ROLE_ADMIN', '시스템 관리자 역할'),
                                                     (2, 'ROLE_MANAGER', '매니저 역할'),
                                                     (3, 'ROLE_USER', '일반 사용자 역할');

-- 사용자-그룹 관계
INSERT INTO USER_GROUPS (user_id, group_id) VALUES (1, 1), (1, 2), (2, 3);

-- 그룹-역할 관계
INSERT INTO GROUP_ROLES (group_id, role_id) VALUES (1, 1), (2, 2), (3, 3);

-- 권한 (Permission) - 자동 생성되므로 초기 데이터는 최소화하거나 생략 가능
-- 이 데이터는 PermissionCatalogService가 동기화할 때 생성/관리됩니다.

-- 역할 계층
INSERT INTO ROLE_HIERARCHY_CONFIG (hierarchy_id, description, hierarchy_string, is_active) VALUES
    (1, '표준 3단계 역할 계층', 'ROLE_ADMIN > ROLE_MANAGER\nROLE_MANAGER > ROLE_USER', true);

-- 정책 템플릿
INSERT INTO POLICY_TEMPLATE (id, template_id, name, description, category, policy_draft_json) VALUES
                                                                                                  (1, 'new-hire-template', '신입사원 기본 권한', '모든 신입사원에게 적용되는 기본적인 읽기 권한을 부여합니다.', 'HR',
                                                                                                   '{
                                                                                                      "name": "신입사원 기본 정책",
                                                                                                      "description": "신입사원 그룹에게 기본적인 읽기 권한을 부여합니다.",
                                                                                                      "effect": "ALLOW",
                                                                                                      "rules": [
                                                                                                          {
                                                                                                              "conditions": ["hasAuthority(''GROUP_3'')"]
                                                                                                          }
                                                                                                      ],
                                                                                                      "targets": []
                                                                                                   }'),
                                                                                                  (2, 'dev-team-template', '개발팀 기본 권한', '개발팀에게 개발 서버 접근 및 관련 리소스에 대한 권한을 부여합니다.', 'Development',
                                                                                                   '{
                                                                                                      "name": "개발팀 기본 정책",
                                                                                                      "description": "개발팀 그룹에게 개발 관련 리소스 접근 권한을 부여합니다.",
                                                                                                      "effect": "ALLOW",
                                                                                                      "rules": [
                                                                                                          {
                                                                                                              "conditions": ["hasAuthority(''GROUP_2'')"]
                                                                                                          }
                                                                                                      ],
                                                                                                      "targets": []
                                                                                                   }');

-- 기본 정책 예시
INSERT INTO POLICY(id, name, description, effect, priority, friendly_description) VALUES
    (1, 'Admin Full Access', '관리자 전체 접근 허용', 'ALLOW', 100, 'ADMIN 역할 보유 시 모든 접근 허용');

INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier, http_method) VALUES
    (1, 1, 'URL', '/admin/**', null);

INSERT INTO POLICY_RULE(id, policy_id, description) VALUES
    (1, 1, 'Admin Role Check');

INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES
    (1, 1, 'hasRole(''ADMIN'')');

-- ID 시퀀스 수동 업데이트 (필요시)
SELECT setval('users_id_seq', (SELECT MAX(id) FROM USERS));
SELECT setval('app_group_group_id_seq', (SELECT MAX(group_id) FROM APP_GROUP));
SELECT setval('role_role_id_seq', (SELECT MAX(role_id) FROM ROLE));
SELECT setval('policy_template_id_seq', (SELECT MAX(id) FROM POLICY_TEMPLATE));
SELECT setval('policy_id_seq', (SELECT MAX(id) FROM POLICY));
SELECT setval('policy_target_id_seq', (SELECT MAX(id) FROM POLICY_TARGET));
SELECT setval('policy_rule_id_seq', (SELECT MAX(id) FROM POLICY_RULE));
SELECT setval('policy_condition_id_seq', (SELECT MAX(id) FROM POLICY_CONDITION));