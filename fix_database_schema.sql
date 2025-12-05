-- SQL Script to fix database schema issues
-- Run this to fix location column and foreign key constraints

USE lebvest;

-- Fix location column in companies table (make it nullable)
ALTER TABLE companies MODIFY COLUMN location VARCHAR(512) NULL;

-- Fix request_id in admin_notifications (make it nullable to avoid FK constraint issues)
ALTER TABLE admin_notifications MODIFY COLUMN request_id BIGINT NULL;

-- Verify the changes
DESCRIBE companies;
DESCRIBE admin_notifications;

SELECT 'Schema fixes applied successfully!' as status;

