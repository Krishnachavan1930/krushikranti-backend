-- SQL Migration Script for KrushiKranti B2B Features
-- Date: 2026-03-08
-- Description: Schema alterations for WhatsApp-style chat, message status, deal tracking, and bulk payments

-- ==============================================================================
-- Phase 2: Bulk Product Image Support
-- ==============================================================================
-- NOTE: imageUrl column already exists in bulk_products table (verified in entity)

-- ==============================================================================
-- Phase 4: Message Status System (Read Receipts)
-- ==============================================================================
ALTER TABLE negotiation_messages 
ADD COLUMN message_status VARCHAR(20) NOT NULL DEFAULT 'SENT';

-- Add index for efficient status queries
CREATE INDEX idx_negotiation_messages_status ON negotiation_messages(message_status);

-- ==============================================================================
-- Phase 6: Deal Offer Creator Tracking
-- ==============================================================================
-- Step 1: Add column as nullable first
ALTER TABLE deal_offers 
ADD COLUMN created_by BIGINT NULL;

-- Step 2: Populate created_by from conversation data BEFORE adding constraint
UPDATE deal_offers do
JOIN conversations c ON do.conversation_id = c.id
SET do.created_by = c.wholesaler_id
WHERE do.created_by IS NULL;

-- Step 3: Set NOT NULL after data is populated
ALTER TABLE deal_offers 
MODIFY COLUMN created_by BIGINT NOT NULL;

-- Step 4: Now add foreign key constraint (all rows have valid user IDs)
ALTER TABLE deal_offers 
ADD CONSTRAINT fk_deal_offers_created_by 
FOREIGN KEY (created_by) REFERENCES users(id);

-- Add index for created_by queries  
CREATE INDEX idx_deal_offers_created_by ON deal_offers(created_by);

-- ==============================================================================
-- Phase 7 & 8: Bulk Orders Table (for B2B Payments & Shipping)
-- ==============================================================================
CREATE TABLE bulk_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deal_offer_id BIGINT NOT NULL UNIQUE,
    farmer_id BIGINT NOT NULL,
    wholesaler_id BIGINT NOT NULL,
    bulk_product_id BIGINT NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    platform_fee DECIMAL(10, 2) COMMENT '5% KrushiKranti commission',
    farmer_payout DECIMAL(12, 2) COMMENT '95% Farmer payout',
    razorpay_order_id VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    order_status VARCHAR(30) NOT NULL DEFAULT 'AWAITING_PAYMENT',
    shipment_id VARCHAR(100),
    awb_code VARCHAR(50),
    courier_name VARCHAR(100),
    tracking_url VARCHAR(500),
    delivery_status VARCHAR(30) DEFAULT 'NOT_SHIPPED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_bulk_orders_deal_offer FOREIGN KEY (deal_offer_id) REFERENCES deal_offers(id),
    CONSTRAINT fk_bulk_orders_farmer FOREIGN KEY (farmer_id) REFERENCES users(id),
    CONSTRAINT fk_bulk_orders_wholesaler FOREIGN KEY (wholesaler_id) REFERENCES users(id),
    CONSTRAINT fk_bulk_orders_product FOREIGN KEY (bulk_product_id) REFERENCES bulk_products(id)
);

-- Indexes for bulk_orders
CREATE INDEX idx_bulk_orders_farmer ON bulk_orders(farmer_id);
CREATE INDEX idx_bulk_orders_wholesaler ON bulk_orders(wholesaler_id);
CREATE INDEX idx_bulk_orders_payment_status ON bulk_orders(payment_status);
CREATE INDEX idx_bulk_orders_order_status ON bulk_orders(order_status);
CREATE INDEX idx_bulk_orders_razorpay_order ON bulk_orders(razorpay_order_id);

-- ==============================================================================
-- Reviews Table (from previous implementation)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_reviews_product_id (product_id),
    INDEX idx_reviews_user_id (user_id)
);

-- ==============================================================================
-- Rollback Script (KEEP FOR REFERENCE - DO NOT RUN)
-- ==============================================================================
/*
ALTER TABLE negotiation_messages DROP COLUMN message_status;
DROP INDEX idx_negotiation_messages_status ON negotiation_messages;

ALTER TABLE deal_offers DROP CONSTRAINT fk_deal_offers_created_by;
ALTER TABLE deal_offers DROP COLUMN created_by;
DROP INDEX idx_deal_offers_created_by ON deal_offers;

DROP TABLE bulk_orders;

DROP TABLE reviews;
*/
