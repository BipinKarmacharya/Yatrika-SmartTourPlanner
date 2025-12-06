-- V1__Create_base_tables.sql

-- Create ENUM types safely (PostgreSQL doesn't support CREATE TYPE IF NOT EXISTS for enums)
DO $$
BEGIN
    -- Create user_role enum if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('USER', 'ADMIN', 'TOUR_GUIDE');
    END IF;

    -- Create budget_range enum if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'budget_range') THEN
        CREATE TYPE budget_range AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'LUXURY');
    END IF;

    -- Create destination_type enum if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'destination_type') THEN
        CREATE TYPE destination_type AS ENUM ('NATURAL', 'CULTURAL', 'ADVENTURE', 'RELIGIOUS');
    END IF;

    -- Create difficulty_level enum if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'difficulty_level') THEN
        CREATE TYPE difficulty_level AS ENUM ('EASY', 'MODERATE', 'DIFFICULT', 'EXTREME');
    END IF;

    -- Create activity_type enum if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'activity_type') THEN
        CREATE TYPE activity_type AS ENUM ('VISIT', 'MEAL', 'TRANSPORT', 'ACCOMMODATION', 'ACTIVITY', 'SHOPPING');
    END IF;

    -- Create season_type enum if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'season_type') THEN
        CREATE TYPE season_type AS ENUM ('SPRING', 'SUMMER', 'AUTUMN', 'WINTER', 'YEAR_ROUND');
    END IF;

    -- Create emergency_type enum if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'emergency_type') THEN
        CREATE TYPE emergency_type AS ENUM ('POLICE', 'HOSPITAL', 'FIRE_STATION', 'AMBULANCE', 'TOURIST_POLICE', 'RESCUE', 'PHARMACY');
    END IF;

    -- Create itinerary_status enum if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'itinerary_status') THEN
        CREATE TYPE itinerary_status AS ENUM ('DRAFT', 'PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
    END IF;

    -- Create trip_type enum if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'trip_type') THEN
        CREATE TYPE trip_type AS ENUM ('SOLO', 'COUPLE', 'FAMILY', 'FRIENDS', 'GROUP', 'BUSINESS');
    END IF;
END $$;

-- Create users table (basic structure)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role user_role DEFAULT 'USER',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Create destinations table (basic structure)
CREATE TABLE IF NOT EXISTS destinations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    district VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    type destination_type NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_coordinates CHECK (
        latitude BETWEEN 26.0 AND 30.5 AND
        longitude BETWEEN 80.0 AND 88.5
    )
);

COMMENT ON TABLE users IS 'Stores user account information';
COMMENT ON TABLE destinations IS 'Stores tourist destinations in Nepal';