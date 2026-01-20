---- src/main/resources/db/migration/V4__Add_indexes_and_constraints.sql

  -- Create indexes for performance
  CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
  CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
  CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

  CREATE INDEX IF NOT EXISTS idx_destinations_name ON destinations(name);
  CREATE INDEX IF NOT EXISTS idx_destinations_district ON destinations(district);
  CREATE INDEX IF NOT EXISTS idx_destinations_type ON destinations(type);
  CREATE INDEX IF NOT EXISTS idx_destinations_coordinates ON destinations(latitude, longitude);
  CREATE INDEX IF NOT EXISTS idx_destinations_popularity ON destinations(popularity_score DESC);
  CREATE INDEX IF NOT EXISTS idx_destinations_rating ON destinations(average_rating DESC);

  -- Create function to update updated_at timestamp
  CREATE OR REPLACE FUNCTION update_updated_at_column()
  RETURNS TRIGGER AS $$
  BEGIN
      NEW.updated_at = CURRENT_TIMESTAMP;
      RETURN NEW;
  END;
  $$ language 'plpgsql';

  -- Create triggers for auto-updating updated_at
  CREATE TRIGGER update_users_updated_at
      BEFORE UPDATE ON users
      FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

  CREATE TRIGGER update_destinations_updated_at
      BEFORE UPDATE ON destinations
      FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

  CREATE TRIGGER update_user_preferences_updated_at
      BEFORE UPDATE ON user_preferences
      FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- New constraint required for V5's ON CONFLICT clause
  ALTER TABLE destinations
      ADD CONSTRAINT destinations_name_district_key UNIQUE (name, district);