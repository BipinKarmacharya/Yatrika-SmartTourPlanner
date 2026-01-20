-- V8__Add_timestamps_to_operating_hours.sql

-- 1. Add the missing 'created_at' column (Required by the error)
ALTER TABLE operating_hours
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;

-- 2. Add the missing 'updated_at' column (Required by BaseEntity convention)
ALTER TABLE operating_hours
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;

-- 3. Create the trigger to auto-update the timestamp
-- NOTE: This assumes the function update_updated_at_column() exists.
CREATE TRIGGER update_operating_hours_updated_at
    BEFORE UPDATE ON operating_hours
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();