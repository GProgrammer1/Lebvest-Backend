-- SQL Query to get FULLY VERIFIED companies - EMAILS and NAMES ONLY
-- These companies have completed all verification steps and can post projects

USE lebvest;

-- Get all fully verified companies with company name, user name, and user email
SELECT 
    c.name AS company_name,
    u.name AS user_name,
    u.email AS user_email
FROM companies c
INNER JOIN users u ON c.user_id = u.id
WHERE c.status = 'FULLY_VERIFIED'
ORDER BY c.name;
