-- V8: Member lifecycle stages
ALTER TABLE members ADD COLUMN IF NOT EXISTS stage VARCHAR(30) DEFAULT 'ACTIVE';
