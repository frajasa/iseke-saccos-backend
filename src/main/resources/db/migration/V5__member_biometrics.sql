-- V5: Add biometric file path columns to members table
ALTER TABLE members ADD COLUMN IF NOT EXISTS photo_path VARCHAR(500);
ALTER TABLE members ADD COLUMN IF NOT EXISTS signature_path VARCHAR(500);
ALTER TABLE members ADD COLUMN IF NOT EXISTS fingerprint_path VARCHAR(500);
