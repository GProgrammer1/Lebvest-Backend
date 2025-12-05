-- SQL Script to add missing columns to companies table
-- Run this to fix the database schema

USE lebvest;

-- Add missing columns to companies table
-- Note: If column already exists, you'll get an error - that's okay, just skip that line

ALTER TABLE companies ADD COLUMN custom_sector VARCHAR(255) NULL;
ALTER TABLE companies ADD COLUMN governorate VARCHAR(255) NULL;
ALTER TABLE companies ADD COLUMN city VARCHAR(255) NULL;
ALTER TABLE companies ADD COLUMN phone_number VARCHAR(50) NULL;
ALTER TABLE companies ADD COLUMN website VARCHAR(512) NULL;

-- If you get "Duplicate column name" errors, those columns already exist - that's fine!

-- Verify the columns were added
DESCRIBE companies;

SELECT 'Columns added successfully! Check the table structure above.' as status;

