-- List all tables in the database
SHOW TABLES;

-- Or get more detailed information
SELECT 
    TABLE_NAME as 'Table Name',
    TABLE_ROWS as 'Row Count',
    ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) AS 'Size (MB)',
    TABLE_TYPE as 'Type'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'lebvest'
ORDER BY TABLE_NAME;

-- Or get table names with row counts
SELECT 
    TABLE_NAME,
    TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'lebvest'
ORDER BY TABLE_NAME;
