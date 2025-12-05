-- Add status column to investments table
-- This migration adds the investment status field for admin review workflow

ALTER TABLE investments 
ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'PENDING_REVIEW';

-- Create index for status column for faster queries
CREATE INDEX IF NOT EXISTS idx_inv_status ON investments(status);

-- Update existing investments to APPROVED status (assuming they were already approved)
-- If you want to keep them as PENDING_REVIEW, comment out the next line
UPDATE investments SET status = 'APPROVED' WHERE status = 'PENDING_REVIEW' OR status IS NULL;

