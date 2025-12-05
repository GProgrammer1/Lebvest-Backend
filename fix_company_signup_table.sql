-- SQL Script to add missing columns to company_signup_requests table
-- Run this to fix the database schema

USE lebvest;

-- Add missing columns
-- Note: If column already exists, you'll get an error - that's okay, just skip that line

ALTER TABLE company_signup_requests ADD COLUMN city VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE company_signup_requests ADD COLUMN custom_sector VARCHAR(255) NULL;
ALTER TABLE company_signup_requests ADD COLUMN governorate VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE company_signup_requests ADD COLUMN phone_number VARCHAR(50) NOT NULL DEFAULT '';
ALTER TABLE company_signup_requests ADD COLUMN website VARCHAR(512) NOT NULL DEFAULT '';

-- Fix location column (make it nullable - it already exists but is NOT NULL)
ALTER TABLE company_signup_requests MODIFY COLUMN location VARCHAR(512) NULL;

-- If you get "Duplicate column name" errors, those columns already exist - that's fine!
-- Just remove the DEFAULT '' after adding if you want to allow NULLs later

-- Verify the columns were added
DESCRIBE company_signup_requests;

SELECT 'Columns added successfully! Check the table structure above.' as status;

