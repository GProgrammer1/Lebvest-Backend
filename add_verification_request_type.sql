-- Add VERIFICATION_REQUEST to admin_notifications.type ENUM
-- This fixes the "Data truncated for column 'type'" error when saving verification notifications

ALTER TABLE admin_notifications 
MODIFY COLUMN type ENUM('APP_STAT_UPDATE','PROJECT_PROPOSAL','SIGNUP_REQUEST','VERIFICATION_REQUEST') DEFAULT NULL;
