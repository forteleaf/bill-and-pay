-- Bill&Pay NixOS PostgreSQL 초기화 스크립트
-- 실행: sudo -u postgres psql < scripts/init-nixos-db.sql

-- 데이터베이스 생성
CREATE DATABASE billpay;

-- 사용자 생성
CREATE USER billpay WITH PASSWORD 'dbShuria40!';

-- billpay 데이터베이스로 연결
\c billpay

-- ltree 확장 설치
CREATE EXTENSION IF NOT EXISTS ltree;

-- 권한 부여
GRANT ALL PRIVILEGES ON DATABASE billpay TO billpay;
GRANT ALL PRIVILEGES ON SCHEMA public TO billpay;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO billpay;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO billpay;

-- 스키마 생성 권한 (테넌트 스키마용)
GRANT CREATE ON DATABASE billpay TO billpay;
