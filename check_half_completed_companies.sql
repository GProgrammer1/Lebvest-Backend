-- SQL Query to get HALF COMPLETED companies - EMAILS and NAMES ONLY
-- These companies have been approved for signup but haven't completed full verification
-- Status can be: 'APPROVED' (approved signup, no docs submitted) or 'PENDING_DOCS' (docs submitted, awaiting approval)

USE lebvest;

-- Get all half-completed companies with company name, user name, user email, and status
SELECT 
    c.name AS company_name,
    u.name AS user_name,
    u.email AS user_email,
    c.status
FROM companies c
INNER JOIN users u ON c.user_id = u.id
WHERE c.status IN ('APPROVED', 'PENDING_DOCS')
ORDER BY c.status, c.name;
