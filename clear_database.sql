-- ============================================
-- DANGER: This script will DELETE ALL DATA from ALL tables!
-- Use with caution - this cannot be undone!
-- ============================================

USE lebvest;

-- Disable foreign key checks to allow deletion
SET FOREIGN_KEY_CHECKS = 0;

-- Delete all data from all tables (in reverse dependency order)
-- This handles foreign key constraints by disabling checks

-- Investment-related tables (depend on companies and investors)
TRUNCATE TABLE investment_updates;
TRUNCATE TABLE investment_ai_predictions;
TRUNCATE TABLE investment_financials;
TRUNCATE TABLE investment_highlights;
TRUNCATE TABLE investment_documents;
TRUNCATE TABLE investment_team_members;
TRUNCATE TABLE investor_investments;
TRUNCATE TABLE investments;

-- Company-related tables
TRUNCATE TABLE company_investors;
TRUNCATE TABLE company_financials;
TRUNCATE TABLE company_social_media;
TRUNCATE TABLE company_team_members;
TRUNCATE TABLE company_documents;
TRUNCATE TABLE company_notifications;
TRUNCATE TABLE funding_history;
TRUNCATE TABLE companies;
TRUNCATE TABLE company_signup_requests;

-- Investor-related tables
TRUNCATE TABLE investor_goals;
TRUNCATE TABLE investor_notifications;
TRUNCATE TABLE investor_watchlist;
TRUNCATE TABLE investor_preferences;
TRUNCATE TABLE investors;

-- User and auth tables
TRUNCATE TABLE user_roles;
TRUNCATE TABLE forgot_pass_tokens;
TRUNCATE TABLE admin_notifications;
TRUNCATE TABLE users;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Verify tables are empty (optional - uncomment to check)
-- SELECT 'users' as table_name, COUNT(*) as row_count FROM users
-- UNION ALL
-- SELECT 'companies', COUNT(*) FROM companies
-- UNION ALL
-- SELECT 'investors', COUNT(*) FROM investors
-- UNION ALL
-- SELECT 'investments', COUNT(*) FROM investments
-- UNION ALL
-- SELECT 'company_signup_requests', COUNT(*) FROM company_signup_requests;

SELECT 'Database cleared successfully!' as status;

