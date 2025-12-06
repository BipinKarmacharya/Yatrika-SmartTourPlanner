---- src/main/resources/db/migration/V3__Add_destination_tables.sql

  -- Add more columns to destinations table
  ALTER TABLE destinations
  ADD COLUMN IF NOT EXISTS short_description VARCHAR(500),
  ADD COLUMN IF NOT EXISTS municipality VARCHAR(100),
  ADD COLUMN IF NOT EXISTS ward_number INTEGER,
  ADD COLUMN IF NOT EXISTS full_address TEXT,
  ADD COLUMN IF NOT EXISTS altitude_meters INTEGER,
  ADD COLUMN IF NOT EXISTS sub_category VARCHAR(100),
  ADD COLUMN IF NOT EXISTS best_season VARCHAR(50),
  ADD COLUMN IF NOT EXISTS best_time_of_day VARCHAR(50),
  ADD COLUMN IF NOT EXISTS difficulty_level difficulty_level DEFAULT 'MODERATE',
  ADD COLUMN IF NOT EXISTS average_duration_hours INTEGER,
  ADD COLUMN IF NOT EXISTS entrance_fee_local DECIMAL(10, 2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS entrance_fee_foreign DECIMAL(10, 2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS average_rating DECIMAL(3, 2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS total_reviews INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS total_visits INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS popularity_score INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS tags TEXT[],
  ADD COLUMN IF NOT EXISTS google_place_id VARCHAR(255),
  ADD COLUMN IF NOT EXISTS tripadvisor_id VARCHAR(100),
  ADD COLUMN IF NOT EXISTS wikipedia_url VARCHAR(500),
  ADD COLUMN IF NOT EXISTS safety_level INTEGER,
  ADD COLUMN IF NOT EXISTS has_parking BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS has_restrooms BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS has_drinking_water BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS has_wifi BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS has_guide_services BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS last_verified_at TIMESTAMP WITH TIME ZONE;

  -- Add constraints
  ALTER TABLE destinations
  ADD CONSTRAINT chk_rating CHECK (average_rating BETWEEN 0 AND 5),
  ADD CONSTRAINT chk_safety_level CHECK (safety_level BETWEEN 1 AND 5),
  ADD CONSTRAINT chk_duration CHECK (average_duration_hours > 0);

  -- Create destination_images table
  CREATE TABLE IF NOT EXISTS destination_images (
      id BIGSERIAL PRIMARY KEY,
      destination_id BIGINT NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
      image_url VARCHAR(500) NOT NULL,
      thumbnail_url VARCHAR(500),
      caption VARCHAR(255),
      credits VARCHAR(255),
      is_primary BOOLEAN DEFAULT FALSE,
      display_order INTEGER DEFAULT 0,
      uploaded_by BIGINT REFERENCES users(id),
      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
  );

  -- Create operating_hours table
  CREATE TABLE IF NOT EXISTS operating_hours (
      id BIGSERIAL PRIMARY KEY,
      destination_id BIGINT NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
      day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
      opens_at TIME,
      closes_at TIME,
      is_closed BOOLEAN DEFAULT FALSE,
      notes VARCHAR(255),

      UNIQUE(destination_id, day_of_week)
  );