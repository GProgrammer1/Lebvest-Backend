-- SQL Script to Insert an Admin User
-- Replace the password hash with your own BCrypt hash if needed
-- Default password: admin123

-- Step 0: Fix the id column to be AUTO_INCREMENT if it's not already
-- Run this first if you get "Field 'id' doesn't have a default value" error
ALTER TABLE users MODIFY COLUMN id BIGINT AUTO_INCREMENT;

-- Step 1: Insert the user into the users table
-- Option A: If id is AUTO_INCREMENT, set it to NULL (or omit it)
INSERT INTO users (id, name, email, password, locked, enabled, created_at)
VALUES (
    NULL,                            -- Let AUTO_INCREMENT handle this
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

-- Step 2: Get the user ID (replace with the actual ID from step 1 if needed)
-- Or use LAST_INSERT_ID() in MySQL
SET @admin_user_id = LAST_INSERT_ID();

-- Step 3: Insert the ADMIN role into user_roles table
INSERT INTO user_roles (user_id, role)
VALUES (
    @admin_user_id,                 -- The user ID from step 1
    'ADMIN'                         -- Role enum value
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

