-- data.sql (PostgreSQL, auth 스키마 기준)

-- 1. 사용자 (Users)
-- 'admin@example.com': ADMIN 역할 (그룹 통해 부여)
-- 'user@example.com': USER 역할 (그룹 통해 부여)
-- 비밀번호는 '{bcrypt}$2a$10$22n9G82e9Y7jC/qXjW1.0O.Z/l.X.1K.0F/l.X.1K' (즉, '1234'의 bcrypt 인코딩)
INSERT INTO USERS (username, password, name, roles, age, mfa_enabled, registered_mfa_factors) VALUES
                                                                                                       ('admin@example.com', '{bcrypt}$2a$10$22n9G82e9Y7jC/qXjW1.0O.Z/l.X.1K.0F/l.X.1K', '관리자', 'ADMIN', 30, true, 'OTT,PASSKEY'),
                                                                                                       ('user@example.com', '{bcrypt}$2a$10$22n9G82e9Y7jC/qXjW1.0O.Z/l.X.1K.0F/l.X.1K', '일반사용자', 'USER', 25, false, 'OTT');

-- 2. 역할 (Role)
INSERT INTO ROLE (role_id, role_name, role_desc, is_expression) VALUES
                                                                         (1, 'ADMIN', '시스템 관리자 역할', 'N'),
                                                                         (2, 'MANAGER', '매니저 역할', 'N'),
                                                                         (3, 'USER', '일반 사용자 역할', 'N');

-- 3. 권한 (Permission)
INSERT INTO PERMISSION (permission_id, permission_name, description, target_type, action_type) VALUES
                                                                                                        (1, 'PERMISSION_READ', '권한 정보 읽기', 'PERMISSION', 'READ'),
                                                                                                        (2, 'PERMISSION_CREATE', '권한 정보 생성', 'PERMISSION', 'CREATE'),
                                                                                                        (3, 'PERMISSION_UPDATE', '권한 정보 수정', 'PERMISSION', 'UPDATE'),
                                                                                                        (4, 'PERMISSION_DELETE', '권한 정보 삭제', 'PERMISSION', 'DELETE'),
                                                                                                        (5, 'DOCUMENT_READ', '문서 읽기 권한', 'DOCUMENT', 'READ'),
                                                                                                        (6, 'DOCUMENT_WRITE', '문서 쓰기 권한', 'DOCUMENT', 'WRITE'),
                                                                                                        (7, 'DOCUMENT_DELETE', '문서 삭제 권한', 'DOCUMENT', 'DELETE'),
                                                                                                        (8, 'BOARD_CREATE', '게시판 글쓰기 권한', 'BOARD', 'CREATE'),
                                                                                                        (9, 'METHOD_RESOURCE_READ', '메서드 리소스 읽기', 'METHOD_RESOURCE', 'READ'),
                                                                                                        (10, 'METHOD_RESOURCE_CREATE', '메서드 리소스 생성', 'METHOD_RESOURCE', 'CREATE'),
                                                                                                        (11, 'METHOD_RESOURCE_UPDATE', '메서드 리소스 수정', 'METHOD_RESOURCE', 'UPDATE'),
                                                                                                        (12, 'METHOD_RESOURCE_DELETE', '메서드 리소스 삭제', 'METHOD_RESOURCE', 'DELETE'),
                                                                                                        (13, 'USER_READ', '사용자 정보 읽기', 'USER', 'READ'), -- UserManagementController의 getUsers, getUser에 대한 권한
                                                                                                        (14, 'USER_UPDATE', '사용자 정보 수정', 'USER', 'UPDATE'),
                                                                                                        (15, 'USER_DELETE', '사용자 정보 삭제', 'USER', 'DELETE'),
                                                                                                        (16, 'GROUP_READ', '그룹 정보 읽기', 'GROUP', 'READ'),
                                                                                                        (17, 'GROUP_CREATE', '그룹 정보 생성', 'GROUP', 'CREATE'),
                                                                                                        (18, 'GROUP_UPDATE', '그룹 정보 수정', 'GROUP', 'UPDATE'),
                                                                                                        (19, 'GROUP_DELETE', '그룹 정보 삭제', 'GROUP', 'DELETE'),
                                                                                                        (20, 'RESOURCE_READ', '자원 정보 읽기', 'RESOURCE', 'READ'),
                                                                                                        (21, 'RESOURCE_CREATE', '자원 정보 생성', 'RESOURCE', 'CREATE'),
                                                                                                        (22, 'RESOURCE_UPDATE', '자원 정보 수정', 'RESOURCE', 'UPDATE'),
                                                                                                        (23, 'RESOURCE_DELETE', '자원 정보 삭제', 'RESOURCE', 'DELETE'),
                                                                                                        (24, 'ROLE_READ', '역할 정보 읽기', 'ROLE', 'READ'),
                                                                                                        (25, 'ROLE_CREATE', '역할 정보 생성', 'ROLE', 'CREATE'),
                                                                                                        (26, 'ROLE_UPDATE', '역할 정보 수정', 'ROLE', 'UPDATE'),
                                                                                                        (27, 'ROLE_DELETE', '역할 정보 삭제', 'ROLE', 'DELETE'),
                                                                                                        (28, 'ROLE_HIERARCHY_READ', '역할 계층 읽기', 'ROLE_HIERARCHY', 'READ'),
                                                                                                        (29, 'ROLE_HIERARCHY_CREATE', '역할 계층 생성', 'ROLE_HIERARCHY', 'CREATE'),
                                                                                                        (30, 'ROLE_HIERARCHY_UPDATE', '역할 계층 수정', 'ROLE_HIERARCHY', 'UPDATE'),
                                                                                                        (31, 'ROLE_HIERARCHY_DELETE', '역할 계층 삭제', 'ROLE_HIERARCHY', 'DELETE'),
                                                                                                        (32, 'ROLE_HIERARCHY_ACTIVATE', '역할 계층 활성화', 'ROLE_HIERARCHY', 'ACTIVATE');


-- 4. 그룹 (APP_GROUP)
INSERT INTO APP_GROUP (group_id, group_name, description) VALUES
                                                                   (1, 'ADMIN_GROUP', '시스템 관리자 그룹'),
                                                                   (2, 'DEVELOPER_GROUP', '개발자 그룹 (매니저 역할)'),
                                                                   (3, 'USER_GROUP', '일반 사용자 그룹');

-- 5. User-Group 관계 (USER_GROUPS)
INSERT INTO USER_GROUPS (user_id, group_id) VALUES
                                                     ((SELECT id FROM USERS WHERE username = 'admin@example.com'), (SELECT group_id FROM APP_GROUP WHERE group_name = 'ADMIN_GROUP')),
                                                     ((SELECT id FROM USERS WHERE username = 'admin@example.com'), (SELECT group_id FROM APP_GROUP WHERE group_name = 'DEVELOPER_GROUP')), -- admin은 개발자 그룹에도 속함
                                                     ((SELECT id FROM USERS WHERE username = 'user@example.com'), (SELECT group_id FROM APP_GROUP WHERE group_name = 'USER_GROUP'));

-- 6. Group-Role 관계 (GROUP_ROLES)
INSERT INTO GROUP_ROLES (group_id, role_id) VALUES
                                                     ((SELECT group_id FROM APP_GROUP WHERE group_name = 'ADMIN_GROUP'), (SELECT role_id FROM ROLE WHERE role_name = 'ADMIN')),
                                                     ((SELECT group_id FROM APP_GROUP WHERE group_name = 'DEVELOPER_GROUP'), (SELECT role_id FROM ROLE WHERE role_name = 'MANAGER')),
                                                     ((SELECT group_id FROM APP_GROUP WHERE group_name = 'USER_GROUP'), (SELECT role_id FROM ROLE WHERE role_name = 'USER'));


-- 7. Role-Permission 관계 (ROLE_PERMISSIONS)
-- ADMIN 역할에 모든 관리 관련 권한 부여
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id) VALUES
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'PERMISSION_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'PERMISSION_CREATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'PERMISSION_UPDATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'PERMISSION_DELETE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'DOCUMENT_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'DOCUMENT_WRITE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'DOCUMENT_DELETE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'BOARD_CREATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'METHOD_RESOURCE_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'METHOD_RESOURCE_CREATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'METHOD_RESOURCE_UPDATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'METHOD_RESOURCE_DELETE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'USER_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'USER_UPDATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'USER_DELETE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'GROUP_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'GROUP_CREATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'GROUP_UPDATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'GROUP_DELETE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'RESOURCE_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'RESOURCE_CREATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'RESOURCE_UPDATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'RESOURCE_DELETE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'ROLE_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'ROLE_CREATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'ROLE_UPDATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'ROLE_DELETE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'ROLE_HIERARCHY_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'ROLE_HIERARCHY_CREATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'ROLE_HIERARCHY_UPDATE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'ROLE_HIERARCHY_DELETE')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'ROLE_HIERARCHY_ACTIVATE'));


-- MANAGER 역할에 특정 권한 부여 (예시)
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id) VALUES
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'MANAGER'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'USER_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'MANAGER'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'DOCUMENT_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'MANAGER'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'DOCUMENT_WRITE'));

-- USER 역할에 기본 권한 부여 (예시)
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id) VALUES
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'USER'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'DOCUMENT_READ')),
                                                               ((SELECT role_id FROM ROLE WHERE role_name = 'USER'), (SELECT permission_id FROM PERMISSION WHERE permission_name = 'BOARD_CREATE'));


-- 13. 문서 (DOCUMENT)
INSERT INTO DOCUMENT (document_id, title, content, owner_username, created_at) VALUES
                                                                                        (1, '관리자 문서 1', '이 문서는 관리자만 볼 수 있는 기밀 문서입니다.', 'admin@example.com', NOW()),
                                                                                        (2, '사용자 문서 1', '이 문서는 일반 사용자만 수정할 수 있는 문서입니다.', 'user@example.com', NOW()),
                                                                                        (3, '공개 문서', '이 문서는 모든 사용자가 읽을 수 있는 공개 문서입니다.', 'guest@example.com', NOW());


-- 2.1. URL 기반 정책 (기존 RESOURCES 테이블 마이그레이션)
-- 정책 1: 관리자는 모든 admin 경로 접근 허용
INSERT INTO POLICY(id, name, description, effect, priority) VALUES (1, 'Admin URL Access', '관리자는 /admin/** 이하 모든 URL에 접근 가능', 'ALLOW', 100);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (1, 1, 'URL', '/admin/**');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (1, 1, 'Admin Role Check');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (1, 1, 'hasRole(''ADMIN'')');

-- 정책 2: 사용자는 /users 경로 접근 허용
INSERT INTO POLICY(id, name, description, effect, priority) VALUES (2, 'User URL Access', '사용자는 /users URL에 접근 가능', 'ALLOW', 200);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (2, 2, 'URL', '/users');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (2, 2, 'User Role Check');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (2, 2, 'hasRole(''USER'')');

-- 2.2. 메서드 기반 정책 (기존 METHOD_RESOURCES 테이블 마이그레이션)
-- 정책 100번대: UserManagementService 관련 정책
INSERT INTO POLICY(id, name, description, effect, priority) VALUES (100, 'Get Users Method Policy', 'getUsers 메서드 접근 제어', 'ALLOW', 100);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (100, 100, 'METHOD', 'io.spring.identityadmin.admin.service.impl.UserManagementServiceImpl.getUsers');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (100, 100, 'Requires USER_READ authority');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (100, 100, 'hasAuthority(''USER_READ'')');

INSERT INTO POLICY(id, name, description, effect, priority) VALUES (101, 'Get User Method Policy', 'getUser 메서드 접근 제어', 'ALLOW', 100);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (101, 101, 'METHOD', 'io.spring.identityadmin.admin.service.impl.UserManagementServiceImpl.getUser');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (101, 101, 'Requires USER_READ authority');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (101, 101, 'hasAuthority(''USER_READ'')');

INSERT INTO POLICY(id, name, description, effect, priority) VALUES (102, 'Modify User Method Policy', 'modifyUser 메서드 접근 제어', 'ALLOW', 100);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (102, 102, 'METHOD', 'io.spring.identityadmin.admin.service.impl.UserManagementServiceImpl.modifyUser');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (102, 102, 'Requires USER_UPDATE authority');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (102, 102, 'hasAuthority(''USER_UPDATE'')');

INSERT INTO POLICY(id, name, description, effect, priority) VALUES (103, 'Delete User Method Policy', 'deleteUser 메서드 접근 제어', 'ALLOW', 100);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (103, 103, 'METHOD', 'io.spring.identityadmin.admin.service.impl.UserManagementServiceImpl.deleteUser');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (103, 103, 'Requires USER_DELETE authority');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (103, 103, 'hasAuthority(''USER_DELETE'')');

-- 정책 200번대: RoleService 관련 정책
INSERT INTO POLICY(id, name, description, effect, priority) VALUES (200, 'Get Roles Method Policy', 'getRoles 메서드 접근 제어', 'ALLOW', 100);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (200, 200, 'METHOD', 'io.spring.identityadmin.admin.service.impl.RoleServiceImpl.getRoles');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (200, 200, 'Requires ROLE_READ authority');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (200, 200, 'hasAuthority(''ROLE_READ'')');

-- ... (다른 모든 RoleService, GroupService, PermissionService 등의 메서드에 대해서도 동일한 패턴으로 POLICY 데이터를 추가합니다)
-- 예: createRole에 대한 정책
INSERT INTO POLICY(id, name, description, effect, priority) VALUES (201, 'Create Role Method Policy', 'createRole 메서드 접근 제어', 'ALLOW', 100);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (201, 201, 'METHOD', 'io.spring.identityadmin.admin.service.impl.RoleServiceImpl.createRole');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (201, 201, 'Requires ROLE_CREATE authority');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (201, 201, 'hasAuthority(''ROLE_CREATE'')');

-- 정책 300번대: DocumentService 관련 정책 (복합 조건 예시)
INSERT INTO POLICY(id, name, description, effect, priority) VALUES (300, 'Read Document Method Policy', 'readDocument 메서드 접근 제어 (소유권 체크)', 'ALLOW', 100);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (300, 300, 'METHOD', 'io.spring.identityadmin.admin.service.DocumentService.readDocument');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (300, 300, 'Requires DOCUMENT_READ permission and ownership');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (300, 300, 'hasPermission(#id, ''DOCUMENT'', ''READ'')'); -- #id는 메서드 파라미터 이름

INSERT INTO POLICY(id, name, description, effect, priority) VALUES (301, 'Update Document Method Policy', 'updateDocumentContent 메서드 접근 제어', 'ALLOW', 100);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (301, 301, 'METHOD', 'io.spring.identityadmin.admin.service.DocumentService.updateDocumentContent');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (301, 301, 'Requires DOCUMENT_WRITE permission');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (301, 301, 'hasPermission(#id, ''DOCUMENT'', ''WRITE'')');

INSERT INTO POLICY(id, name, description, effect, priority) VALUES (302, 'Delete Document Method Policy', 'deleteDocument 메서드 접근 제어', 'ALLOW', 100);
INSERT INTO POLICY_TARGET(id, policy_id, target_type, target_identifier) VALUES (302, 302, 'METHOD', 'io.spring.identityadmin.admin.service.DocumentService.deleteDocument');
INSERT INTO POLICY_RULE(id, policy_id, description) VALUES (302, 302, 'Requires DOCUMENT_DELETE permission');
INSERT INTO POLICY_CONDITION(id, rule_id, expression) VALUES (302, 302, 'hasPermission(#id, ''DOCUMENT'', ''DELETE'')');