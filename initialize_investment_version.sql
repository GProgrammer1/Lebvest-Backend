-- SQL Script to initialize version field for existing investments
-- This fixes the issue where existing investments have null version values
-- Run this after adding the @Version field to Investment entity

USE lebvest;

-- Set version to 0 for all existing investments that have null version
UPDATE investments 
SET version = 0 
WHERE version IS NULL;

-- Verify the update
SELECT COUNT(*) as investments_with_version 
FROM investments 
WHERE version IS NOT NULL;

SELECT 'Version field initialized successfully!' as status;

