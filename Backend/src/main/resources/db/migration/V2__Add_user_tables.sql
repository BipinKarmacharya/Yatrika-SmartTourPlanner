-- src/main/resources/db/migration/V2__Add_user_tables.sql

-- Add more columns to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITH TIME ZONE;

-- Create user_preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Travel preferences
    budget_range budget_range DEFAULT 'MEDIUM',
    travel_pace VARCHAR(20) DEFAULT 'MODERATE',
    preferred_group_size INTEGER DEFAULT 1,

    -- Activity preferences
    preferred_activities TEXT[],
    accommodation_types TEXT[],
    food_preferences TEXT[],

    -- Boolean preferences
    is_adventure_seeker BOOLEAN DEFAULT FALSE,
    is_culture_enthusiast BOOLEAN DEFAULT FALSE,
    is_nature_lover BOOLEAN DEFAULT FALSE,
    is_food_explorer BOOLEAN DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_travel_pace CHECK (travel_pace IN ('RELAXED', 'MODERATE', 'FAST_PACED')),
    CONSTRAINT chk_group_size CHECK (preferred_group_size BETWEEN 1 AND 20)
);

-- Create refresh_tokens table for JWT refresh
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(token_hash)
);