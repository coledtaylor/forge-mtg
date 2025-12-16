-- ============================================
-- Forge MTG Card Database Schema
-- ============================================
-- This script creates the optimized database schema for storing Magic: The Gathering cards.
-- Run this in PostgreSQL to set up the database manually,
-- or let Hibernate create it automatically with ddl-auto=create/update

-- ============================================
-- CARDS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS cards (
    id BIGSERIAL PRIMARY KEY,

    -- Card Identity
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,

    -- Mana and Colors
    mana_cost VARCHAR(50),
    colors VARCHAR(10),           -- e.g., "U,B" for Blue/Black
    color_identity VARCHAR(10),   -- For Commander format

    -- Card Characteristics
    rarity VARCHAR(20),            -- Common, Uncommon, Rare, Mythic
    power INTEGER,
    toughness INTEGER,
    loyalty INTEGER,               -- For Planeswalkers

    -- Card Text
    text TEXT,
    flavor_text TEXT,

    -- Set Information
    edition VARCHAR(10),
    collector_number VARCHAR(20),
    artist VARCHAR(255),

    -- Image URLs
    image_url VARCHAR(500),
    image_url_back VARCHAR(500),   -- For double-faced cards

    -- Metadata
    is_custom BOOLEAN DEFAULT false,
    is_token BOOLEAN DEFAULT false,
    is_commander_legal BOOLEAN DEFAULT false,
    is_standard_legal BOOLEAN DEFAULT false,
    is_modern_legal BOOLEAN DEFAULT false,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_power CHECK (power IS NULL OR power >= -99),
    CONSTRAINT chk_toughness CHECK (toughness IS NULL OR toughness >= -99)
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================

-- Primary search indexes
CREATE INDEX idx_card_name ON cards(name);
CREATE INDEX idx_card_name_lower ON cards(LOWER(name));
CREATE INDEX idx_card_type ON cards(type);
CREATE INDEX idx_card_colors ON cards(colors);
CREATE INDEX idx_card_edition ON cards(edition);
CREATE INDEX idx_card_rarity ON cards(rarity);

-- Composite index for common searches
CREATE INDEX idx_card_type_colors ON cards(type, colors);
CREATE INDEX idx_card_edition_number ON cards(edition, collector_number);

-- Legality filters
CREATE INDEX idx_card_standard_legal ON cards(is_standard_legal) WHERE is_standard_legal = true;
CREATE INDEX idx_card_commander_legal ON cards(is_commander_legal) WHERE is_commander_legal = true;

-- Full-text search (PostgreSQL specific)
CREATE INDEX idx_card_text_search ON cards
    USING GIN(to_tsvector('english', COALESCE(name, '') || ' ' || COALESCE(text, '')));

-- Creature stats search
CREATE INDEX idx_card_power ON cards(power) WHERE power IS NOT NULL;
CREATE INDEX idx_card_toughness ON cards(toughness) WHERE toughness IS NOT NULL;

-- ============================================
-- SAMPLE DATA
-- ============================================

INSERT INTO cards (name, type, mana_cost, colors, rarity, power, toughness, text, edition) VALUES
    ('Lightning Bolt', 'Instant', '{R}', 'R', 'Common', NULL, NULL, 'Lightning Bolt deals 3 damage to any target.', 'M21'),
    ('Counterspell', 'Instant', '{U}{U}', 'U', 'Common', NULL, NULL, 'Counter target spell.', 'M21'),
    ('Grizzly Bears', 'Creature — Bear', '{1}{G}', 'G', 'Common', 2, 2, NULL, 'M21'),
    ('Black Lotus', 'Artifact', '{0}', NULL, 'Rare', NULL, NULL, '{T}, Sacrifice Black Lotus: Add three mana of any one color.', 'LEA'),
    ('Ancestral Recall', 'Instant', '{U}', 'U', 'Rare', NULL, NULL, 'Target player draws three cards.', 'LEA'),
    ('Serra Angel', 'Creature — Angel', '{3}{W}{W}', 'W', 'Uncommon', 4, 4, 'Flying, vigilance', 'M21'),
    ('Llanowar Elves', 'Creature — Elf Druid', '{G}', 'G', 'Common', 1, 1, '{T}: Add {G}.', 'M21'),
    ('Dark Ritual', 'Instant', '{B}', 'B', 'Common', NULL, NULL, 'Add {B}{B}{B}.', 'LEA')
ON CONFLICT DO NOTHING;

-- ============================================
-- USEFUL QUERIES
-- ============================================

-- Count cards by type
-- SELECT type, COUNT(*) as count FROM cards GROUP BY type ORDER BY count DESC;

-- Find all Blue cards
-- SELECT name, mana_cost FROM cards WHERE colors LIKE '%U%';

-- Search by name (case-insensitive)
-- SELECT * FROM cards WHERE LOWER(name) LIKE LOWER('%bolt%');

-- Find creatures with power >= 4
-- SELECT name, power, toughness FROM cards WHERE power >= 4 ORDER BY power DESC;

-- Full-text search
-- SELECT name, text FROM cards WHERE to_tsvector('english', name || ' ' || COALESCE(text, '')) @@ to_tsquery('english', 'damage');

-- ============================================
-- MAINTENANCE
-- ============================================

-- Update statistics for query optimizer
ANALYZE cards;

-- Rebuild indexes (if needed)
-- REINDEX TABLE cards;

-- ============================================
-- OPTIONAL: CARD SETS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS card_sets (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    release_date DATE,
    set_type VARCHAR(50),  -- Core, Expansion, Masters, etc.
    total_cards INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_set_code ON card_sets(code);
CREATE INDEX idx_set_release_date ON card_sets(release_date);

-- Add foreign key to cards table (optional)
-- ALTER TABLE cards ADD COLUMN set_id BIGINT;
-- ALTER TABLE cards ADD CONSTRAINT fk_card_set FOREIGN KEY (set_id) REFERENCES card_sets(id);

-- ============================================
-- OPTIONAL: CARD RULINGS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS card_rulings (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL,
    ruling_date DATE NOT NULL,
    ruling_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ruling_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
);

CREATE INDEX idx_ruling_card_id ON card_rulings(card_id);
CREATE INDEX idx_ruling_date ON card_rulings(ruling_date);

-- ============================================
-- OPTIONAL: USER COLLECTIONS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS user_collections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    foil BOOLEAN DEFAULT false,
    condition VARCHAR(20),  -- Near Mint, Lightly Played, etc.
    acquired_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_collection_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
    CONSTRAINT chk_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_collection_user ON user_collections(user_id);
CREATE INDEX idx_collection_card ON user_collections(card_id);
CREATE UNIQUE INDEX idx_collection_unique ON user_collections(user_id, card_id, foil, condition);

-- ============================================
-- PERFORMANCE MONITORING
-- ============================================

-- View table size
-- SELECT pg_size_pretty(pg_total_relation_size('cards')) AS total_size;

-- View index usage
-- SELECT
--     schemaname,
--     tablename,
--     indexname,
--     idx_scan,
--     pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public' AND tablename = 'cards'
-- ORDER BY idx_scan DESC;

-- Find slow queries
-- SELECT
--     calls,
--     mean_exec_time,
--     query
-- FROM pg_stat_statements
-- WHERE query LIKE '%cards%'
-- ORDER BY mean_exec_time DESC
-- LIMIT 10;

