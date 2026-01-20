-- V6__Add_updated_at_to_destination_images.sql

-- 1. Add the updated_at column
ALTER TABLE destination_images
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;

-- 2. Create the trigger to auto-update the timestamp
CREATE TRIGGER update_destination_images_updated_at
    BEFORE UPDATE ON destination_images
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();