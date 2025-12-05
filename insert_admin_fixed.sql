-- SQL Script to Insert an Admin User (Fixed for RDS with Foreign Keys)
-- Default password: admin123
-- Note: This script assumes the users table already has AUTO_INCREMENT on id column
-- (which it does if imported from dump.sql)

-- Step 1: Insert the user into the users table (omit id to use AUTO_INCREMENT)
INSERT INTO users (name, email, password, locked, enabled, created_at)
VALUES (
    'Admin User',                    -- Change to your admin name
    'bousleimengeorgio139@gmail.com', -- Admin email
    '$2a$10$rYU86.5TBx8bD1amb0smUOTIkcdf7dbLYCa7upu6ZRHpVdWlQXL.a',  -- BCrypt hash for "admin123" (VERIFIED)
    false,                           -- Account not locked
    true,                            -- Account enabled
    NOW()                            -- Current timestamp
);

-- Step 2: Get the user ID and insert ADMIN role
INSERT INTO user_roles (user_id, role)
VALUES (
    (SELECT id FROM users WHERE email = 'bousleimengeorgio139@gmail.com'),
    'ADMIN'
);

-- Verify the admin was created:
SELECT u.id, u.name, u.email, u.enabled, u.locked, ur.role
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.email = 'bousleimengeorgio139@gmail.com';
