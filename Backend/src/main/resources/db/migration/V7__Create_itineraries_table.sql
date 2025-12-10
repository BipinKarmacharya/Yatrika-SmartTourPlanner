-- V7__Create_itinerary_tables.sql

-- Assumes BaseEntity fields (id, created_at, updated_at) are handled by common table creation logic
-- or are included in the table creation below. Since you didn't show V1, we include them here
-- as they are common requirements for your entities.

-------------------------------------------
-- 1. CREATE ITINERARIES TABLE
-------------------------------------------
CREATE TABLE itineraries (
    -- BaseEntity Fields
                             id BIGSERIAL PRIMARY KEY,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Itinerary Fields
                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             title VARCHAR(200) NOT NULL,
                             description VARCHAR(1000),
                             cover_image_url VARCHAR(255),
                             start_date DATE NOT NULL,
                             end_date DATE NOT NULL,
                             total_days INTEGER,
                             trip_type VARCHAR(50), -- Based on TripType enum
                             budget_range VARCHAR(50),
                             estimated_total_cost NUMERIC(12, 2),
                             actual_total_cost NUMERIC(12, 2),
                             status VARCHAR(50) DEFAULT 'DRAFT' NOT NULL, -- Based on ItineraryStatus enum
                             is_public BOOLEAN DEFAULT FALSE,
                             total_views INTEGER DEFAULT 0,
                             total_likes INTEGER DEFAULT 0,
                             total_bookmarks INTEGER DEFAULT 0
);

-------------------------------------------
-- 2. CREATE ITINERARY_ITEMS TABLE
-------------------------------------------
CREATE TABLE itinerary_items (
    -- BaseEntity Fields
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Item Relationships
    itinerary_id BIGINT NOT NULL REFERENCES itineraries(id) ON DELETE CASCADE,
    destination_id BIGINT REFERENCES destinations(id) ON DELETE SET NULL, -- Assuming destinations table exists

    -- Item Details
    day_number INTEGER NOT NULL,
    order_in_day INTEGER,
    start_time TIME,
    end_time TIME,
    duration_minutes INTEGER,
    activity_type VARCHAR(50),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    notes TEXT,
    location_name VARCHAR(255),
    location_address VARCHAR(255),
    estimated_cost NUMERIC(10, 2),
    actual_cost NUMERIC(10, 2),
    is_completed BOOLEAN DEFAULT FALSE,
    is_cancelled BOOLEAN DEFAULT FALSE,

    -- Constraint to ensure order within a day is unique for an itinerary
    UNIQUE (itinerary_id, day_number, order_in_day)
);

-------------------------------------------
-- 3. CREATE TRIGGERS & INDEXES
-------------------------------------------

-- Create trigger for itineraries table
CREATE TRIGGER update_itineraries_updated_at
    BEFORE UPDATE ON itineraries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create trigger for itinerary_items table
CREATE TRIGGER update_itinerary_items_updated_at
    BEFORE UPDATE ON itinerary_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_itineraries_user_id ON itineraries(user_id);
CREATE INDEX IF NOT EXISTS idx_itinerary_items_itinerary_id ON itinerary_items(itinerary_id);
CREATE INDEX IF NOT EXISTS idx_itinerary_items_destination_id ON itinerary_items(destination_id);