-- PostgreSQL 기준 스키마

-- 기존 테이블이 있다면 삭제하여 초기 상태 보장
DROP TABLE IF EXISTS WIZARD_SESSION CASCADE;
DROP TABLE IF EXISTS POLICY_TEMPLATE CASCADE;
DROP TABLE IF EXISTS BUSINESS_RESOURCE_ACTION CASCADE;
DROP TABLE IF EXISTS BUSINESS_ACTION CASCADE;
DROP TABLE IF EXISTS BUSINESS_RESOURCE CASCADE;
DROP TABLE IF EXISTS CONDITION_TEMPLATE CASCADE;
DROP TABLE IF EXISTS ROLE_HIERARCHY_CONFIG CASCADE;
DROP TABLE IF EXISTS PERMISSION_FUNCTIONS CASCADE;
DROP TABLE IF EXISTS ROLE_PERMISSIONS CASCADE;
DROP TABLE IF EXISTS PERMISSION CASCADE;
DROP TABLE IF EXISTS GROUP_ROLES CASCADE;
DROP TABLE IF EXISTS ROLE CASCADE;
DROP TABLE IF EXISTS USER_GROUPS CASCADE;
DROP TABLE IF EXISTS APP_GROUP CASCADE;
DROP TABLE IF EXISTS USERS CASCADE;
DROP TABLE IF EXISTS FUNCTION_CATALOG CASCADE;
DROP TABLE IF EXISTS FUNCTION_GROUP CASCADE;
DROP TABLE IF EXISTS MANAGED_RESOURCE CASCADE;
DROP TABLE IF EXISTS POLICY_CONDITION CASCADE;
DROP TABLE IF EXISTS POLICY_RULE CASCADE;
DROP TABLE IF EXISTS POLICY_TARGET CASCADE;
DROP TABLE IF EXISTS POLICY CASCADE;
DROP TABLE IF EXISTS DOCUMENT CASCADE;
DROP TABLE IF EXISTS AUDIT_LOG CASCADE;

-- 사용자 테이블
CREATE TABLE USERS (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       name VARCHAR(255) NOT NULL,
                       mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                       last_mfa_used_at TIMESTAMP,
                       enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- 그룹 테이블
CREATE TABLE APP_GROUP (
                           group_id BIGSERIAL PRIMARY KEY,
                           group_name VARCHAR(255) UNIQUE NOT NULL,
                           description VARCHAR(255)
);

-- 역할 테이블
CREATE TABLE ROLE (
                      role_id BIGSERIAL PRIMARY KEY,
                      role_name VARCHAR(255) UNIQUE NOT NULL,
                      role_desc VARCHAR(255),
                      is_expression VARCHAR(1) DEFAULT 'N'
);

-- 권한 테이블
CREATE TABLE PERMISSION (
                            permission_id BIGSERIAL PRIMARY KEY,
                            permission_name VARCHAR(255) UNIQUE NOT NULL,
                            description VARCHAR(255),
                            target_type VARCHAR(255),
                            action_type VARCHAR(255),
                            condition_expression VARCHAR(2048)
);

-- 사용자-그룹 조인 테이블
CREATE TABLE USER_GROUPS (
                             user_id BIGINT NOT NULL REFERENCES USERS(id) ON DELETE CASCADE,
                             group_id BIGINT NOT NULL REFERENCES APP_GROUP(group_id) ON DELETE CASCADE,
                             PRIMARY KEY (user_id, group_id)
);

-- 그룹-역할 조인 테이블
CREATE TABLE GROUP_ROLES (
                             group_id BIGINT NOT NULL REFERENCES APP_GROUP(group_id) ON DELETE CASCADE,
                             role_id BIGINT NOT NULL REFERENCES ROLE(role_id) ON DELETE CASCADE,
                             PRIMARY KEY (group_id, role_id)
);

-- 역할-권한 조인 테이블
CREATE TABLE ROLE_PERMISSIONS (
                                  role_id BIGINT NOT NULL REFERENCES ROLE(role_id) ON DELETE CASCADE,
                                  permission_id BIGINT NOT NULL REFERENCES PERMISSION(permission_id) ON DELETE CASCADE,
                                  PRIMARY KEY (role_id, permission_id)
);

-- 기술 리소스 테이블
CREATE TABLE MANAGED_RESOURCE (
                                  id BIGSERIAL PRIMARY KEY,
                                  resource_identifier VARCHAR(255) UNIQUE NOT NULL,
                                  resource_type VARCHAR(255) NOT NULL,
                                  http_method VARCHAR(255),
                                  friendly_name VARCHAR(255) NOT NULL,
                                  description VARCHAR(1024),
                                  service_owner VARCHAR(255),
                                  parameter_types VARCHAR(255),
                                  return_type VARCHAR(255),
                                  is_managed BOOLEAN NOT NULL DEFAULT TRUE
);

-- 기능 그룹 테이블
CREATE TABLE FUNCTION_GROUP (
                                id BIGSERIAL PRIMARY KEY,
                                name VARCHAR(255) UNIQUE NOT NULL
);

-- 기능 카탈로그 테이블
CREATE TABLE FUNCTION_CATALOG (
                                  id BIGSERIAL PRIMARY KEY,
                                  managed_resource_id BIGINT UNIQUE NOT NULL REFERENCES MANAGED_RESOURCE(id) ON DELETE CASCADE,
                                  friendly_name VARCHAR(255) NOT NULL,
                                  description VARCHAR(1024),
                                  function_group_id BIGINT REFERENCES FUNCTION_GROUP(id),
                                  status VARCHAR(255) NOT NULL DEFAULT 'UNCONFIRMED'
);

-- 권한-기능 조인 테이블
CREATE TABLE PERMISSION_FUNCTIONS (
                                      permission_id BIGINT NOT NULL REFERENCES PERMISSION(permission_id) ON DELETE CASCADE,
                                      function_catalog_id BIGINT NOT NULL REFERENCES FUNCTION_CATALOG(id) ON DELETE CASCADE,
                                      PRIMARY KEY (permission_id, function_catalog_id)
);

-- 정책 테이블
CREATE TABLE POLICY (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255) UNIQUE NOT NULL,
                        description VARCHAR(255),
                        effect VARCHAR(255) NOT NULL,
                        priority INT NOT NULL,
                        friendly_description VARCHAR(2048)
);

-- 정책 대상 테이블
CREATE TABLE POLICY_TARGET (
                               id BIGSERIAL PRIMARY KEY,
                               policy_id BIGINT NOT NULL REFERENCES POLICY(id) ON DELETE CASCADE,
                               target_type VARCHAR(255) NOT NULL,
                               target_identifier VARCHAR(255) NOT NULL,
                               http_method VARCHAR(255)
);

-- 정책 규칙 테이블
CREATE TABLE POLICY_RULE (
                             id BIGSERIAL PRIMARY KEY,
                             policy_id BIGINT NOT NULL REFERENCES POLICY(id) ON DELETE CASCADE,
                             description VARCHAR(255)
);

-- 정책 조건 테이블
CREATE TABLE POLICY_CONDITION (
                                  id BIGSERIAL PRIMARY KEY,
                                  rule_id BIGINT NOT NULL REFERENCES POLICY_RULE(id) ON DELETE CASCADE,
                                  condition_expression VARCHAR(2048) NOT NULL,
                                  description VARCHAR(255)
);

-- 역할 계층 테이블
CREATE TABLE ROLE_HIERARCHY_CONFIG (
                                       hierarchy_id BIGSERIAL PRIMARY KEY,
                                       description VARCHAR(255),
                                       hierarchy_string TEXT NOT NULL,
                                       is_active BOOLEAN NOT NULL DEFAULT FALSE
);

-- 감사 로그 테이블
CREATE TABLE AUDIT_LOG (
                           id BIGSERIAL PRIMARY KEY,
                           timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           principal_name VARCHAR(255) NOT NULL,
                           resource_identifier VARCHAR(512) NOT NULL,
                           action VARCHAR(255),
                           decision VARCHAR(255) NOT NULL,
                           reason VARCHAR(1024),
                           client_ip VARCHAR(255)
);

-- 정책 템플릿 테이블
CREATE TABLE POLICY_TEMPLATE (
                                 id BIGSERIAL PRIMARY KEY,
                                 template_id VARCHAR(255) UNIQUE NOT NULL,
                                 name VARCHAR(255) NOT NULL,
                                 description VARCHAR(1024),
                                 category VARCHAR(255),
    -- [오류 수정 및 개선] TEXT 대신 JSONB 타입을 사용하여 성능 및 기능 최적화
                                 policy_draft_json JSONB NOT NULL
);

-- 마법사 세션 테이블
CREATE TABLE WIZARD_SESSION (
                                id VARCHAR(36) PRIMARY KEY,
                                context_data TEXT NOT NULL,
                                owner_user_id BIGINT NOT NULL,
                                created_at TIMESTAMP NOT NULL,
                                expires_at TIMESTAMP NOT NULL
);