-- Performance indexes for admin dashboard queries
-- Run this migration to improve query performance

-- Indexes for users table
-- Composite index for common filter combinations
CREATE INDEX IF NOT EXISTS idx_users_enabled_locked ON users(enabled, locked);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users(enabled);
CREATE INDEX IF NOT EXISTS idx_users_locked ON users(locked);

-- Index for user_roles join table (for role filtering)
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role);

-- Note: MySQL doesn't support functional indexes directly, but we can use generated columns
-- For case-insensitive search, consider using COLLATE utf8mb4_general_ci on the columns
-- or use full-text search for better performance

-- Indexes for investments table (for project queries)
CREATE INDEX IF NOT EXISTS idx_investments_status ON investments(status);
CREATE INDEX IF NOT EXISTS idx_investments_category ON investments(category);
CREATE INDEX IF NOT EXISTS idx_investments_status_category ON investments(status, category);
CREATE INDEX IF NOT EXISTS idx_investments_company_id ON investments(company_id);
CREATE INDEX IF NOT EXISTS idx_investments_created_at ON investments(created_at DESC);

-- Index for company table (for joins)
CREATE INDEX IF NOT EXISTS idx_company_user_id ON companies(user_id);
CREATE INDEX IF NOT EXISTS idx_company_status ON companies(status);

-- Index for company_verification_documents (for batch loading)
CREATE INDEX IF NOT EXISTS idx_verification_docs_company_id ON company_verification_documents(company_id);

-- Full-text search indexes (MySQL 5.6+)
-- These enable fast text search without full table scans
-- Note: Run these only if your MySQL version supports FULLTEXT on InnoDB (5.6.4+)
-- ALTER TABLE users ADD FULLTEXT INDEX ft_users_name_email (name, email);
-- ALTER TABLE investments ADD FULLTEXT INDEX ft_investments_title (title);

-- For prefix search optimization, ensure columns use case-insensitive collation
-- ALTER TABLE users MODIFY name VARCHAR(255) COLLATE utf8mb4_general_ci;
-- ALTER TABLE users MODIFY email VARCHAR(255) COLLATE utf8mb4_general_ci;
-- ALTER TABLE investments MODIFY title VARCHAR(255) COLLATE utf8mb4_general_ci;

