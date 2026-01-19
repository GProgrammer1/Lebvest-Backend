-- SQL script to create an admin user
-- Password hash for "admin123": $2a$10$YhEx8S8nYKrnvKeo4Vg35.BFPmVjtzTA12LrF8hY8NrfVwjIgvcbC
-- To generate a new hash: ./mvnw exec:java -Dexec.mainClass="com.lebvest.util.PasswordHashGenerator" -Dexec.args="admin123"

-- Check if user exists and update, otherwise insert
SET @user_exists = (SELECT COUNT(*) FROM users WHERE email = 'admin@lebvest.com');

-- If user exists, update it
UPDATE users 
SET 
    password = '$2a$10$YhEx8S8nYKrnvKeo4Vg35.BFPmVjtzTA12LrF8hY8NrfVwjIgvcbC',
    name = 'Admin User',
    locked = false,
    enabled = true
WHERE email = 'admin@lebvest.com'
AND @user_exists > 0;

-- If user doesn't exist, insert it
-- Note: This assumes id has AUTO_INCREMENT. If you get an error, run:
-- ALTER TABLE users MODIFY id BIGINT AUTO_INCREMENT;
INSERT INTO users (name, email, password, locked, enabled, created_at)
SELECT 'Admin User', 'admin@lebvest.com', '$2a$10$YhEx8S8nYKrnvKeo4Vg35.BFPmVjtzTA12LrF8hY8NrfVwjIgvcbC', false, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@lebvest.com');

-- Get the user ID (adjust email if needed)
SET @user_id = (SELECT id FROM users WHERE email = 'admin@lebvest.com');

-- Only proceed with role assignment if user exists
-- Delete existing ADMIN role for this user (if any)
DELETE FROM user_roles WHERE user_id = @user_id AND role = 'ADMIN';

-- Add ADMIN role (only if user_id is not NULL)
INSERT INTO user_roles (user_id, role)
SELECT @user_id, 'ADMIN'
WHERE @user_id IS NOT NULL
ON DUPLICATE KEY UPDATE role = 'ADMIN';

-- Verify the user was created
SELECT 
    u.id,
    u.name,
    u.email,
    u.enabled,
    u.locked,
    GROUP_CONCAT(ur.role) as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.email = 'admin@lebvest.com'
GROUP BY u.id, u.name, u.email, u.enabled, u.locked;

