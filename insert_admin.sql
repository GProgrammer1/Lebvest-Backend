-- SQL Script to Insert an Admin User
-- Replace the password hash with your own BCrypt hash if needed
-- Default password: admin123

-- Step 0: Fix the id column to be AUTO_INCREMENT if it's not already
-- NOTE: If you imported from dump.sql, this is NOT needed and will fail due to foreign keys
-- Only run this if you get "Field 'id' doesn't have a default value" error AND there are no foreign keys
-- ALTER TABLE users MODIFY COLUMN id BIGINT AUTO_INCREMENT;

-- Step 1: Insert the user into the users table
-- Omit the id column to let AUTO_INCREMENT handle it (works if table was created from dump.sql)
INSERT INTO users (name, email, password, locked, enabled, created_at)
VALUES (
    'Admin User',                    -- Change to your admin name
    'bousleimengeorgio139@gmail.com', -- Admin email
    '$2a$10$rYU86.5TBx8bD1amb0smUOTIkcdf7dbLYCa7upu6ZRHpVdWlQXL.a',  -- BCrypt hash for "admin123" (VERIFIED)
    false,                           -- Account not locked
    true,                            -- Account enabled
    NOW()                            -- Current timestamp
);

-- Option B: If the above doesn't work, omit the id column entirely:
-- INSERT INTO users (name, email, password, locked, enabled, created_at)
-- VALUES (
--     'Admin User',
--     'bousleimengeorgio139@gmail.com',
--     '$2a$10$rYU86.5TBx8bD1amb0smUOTIkcdf7dbLYCa7upu6ZRHpVdWlQXL.a',
--     false,
--     true,
--     NOW()
-- );

-- If you already inserted the admin with the WRONG hash, run this UPDATE:
-- UPDATE users 
-- SET password = '$2a$10$rYU86.5TBx8bD1amb0smUOTIkcdf7dbLYCa7upu6ZRHpVdWlQXL.a'
-- WHERE email = 'bousleimengeorgio139@gmail.com';

-- Step 2: Insert the ADMIN role into user_roles table
-- Using subquery to get the user ID (more reliable than LAST_INSERT_ID in some cases)
INSERT INTO user_roles (user_id, role)
VALUES (
    (SELECT id FROM users WHERE email = 'bousleimengeorgio139@gmail.com'),
    'ADMIN'
);

-- Alternative: If you know the user ID, you can combine steps 2 and 3:
-- INSERT INTO user_roles (user_id, role)
-- VALUES (
--     (SELECT id FROM users WHERE email = 'bousleimengeorgio139@gmail.com'),
--     'ADMIN'
-- );

-- Verify the admin was created:
-- SELECT u.id, u.name, u.email, u.enabled, u.locked, ur.role
-- FROM users u
-- LEFT JOIN user_roles ur ON u.id = ur.user_id
-- WHERE u.email = 'bousleimengeorgio139@gmail.com';

