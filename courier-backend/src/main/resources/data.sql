-- ============================================================
-- Statuses — parcel lifecycle
-- ============================================================
-- REGISTERED       : paczka przyjęta do systemu, czeka na weryfikację przez Workera
-- PENDING_PICKUP   : zweryfikowana, czeka aż kurier ją odbierze
-- IN_TRANSIT       : kurier odebrał, jedzie do kolejnego hub-u (next_region)
-- AT_HUB           : dotarła do hub-u pośredniego, czeka na kolejny odcinek
-- OUT_FOR_DELIVERY : jest już w regionie docelowym, kurier jedzie do odbiorcy
-- DELIVERED        : dostarczona pomyślnie
-- UNDELIVERED      : próba dostarczenia nieudana (np. nie było nikogo)
-- LOST             : zgłoszona jako zagubiona
-- DAMAGED          : zgłoszona jako zniszczona
-- ============================================================
INSERT INTO statuses (name, created_at) VALUES ('REGISTERED',       CURRENT_TIMESTAMP);
INSERT INTO statuses (name, created_at) VALUES ('PENDING_PICKUP',   CURRENT_TIMESTAMP);
INSERT INTO statuses (name, created_at) VALUES ('IN_TRANSIT',       CURRENT_TIMESTAMP);
INSERT INTO statuses (name, created_at) VALUES ('AT_HUB',           CURRENT_TIMESTAMP);
INSERT INTO statuses (name, created_at) VALUES ('OUT_FOR_DELIVERY', CURRENT_TIMESTAMP);
INSERT INTO statuses (name, created_at) VALUES ('DELIVERED',        CURRENT_TIMESTAMP);
INSERT INTO statuses (name, created_at) VALUES ('UNDELIVERED',      CURRENT_TIMESTAMP);
INSERT INTO statuses (name, created_at) VALUES ('LOST',             CURRENT_TIMESTAMP);
INSERT INTO statuses (name, created_at) VALUES ('DAMAGED',          CURRENT_TIMESTAMP);

-- ============================================================
-- Roles
-- ============================================================
INSERT INTO roles (name, created_at) VALUES ('WORKER',  CURRENT_TIMESTAMP);
INSERT INTO roles (name, created_at) VALUES ('COURIER', CURRENT_TIMESTAMP);
INSERT INTO roles (name, created_at) VALUES ('ADMIN',   CURRENT_TIMESTAMP);
INSERT INTO roles (name, created_at) VALUES ('CLIENT',  CURRENT_TIMESTAMP);

-- ============================================================
-- Delivery modes
-- ============================================================
INSERT INTO delivery_modes (name, created_at) VALUES ('STANDARD', CURRENT_TIMESTAMP);
INSERT INTO delivery_modes (name, created_at) VALUES ('EXPRESS',  CURRENT_TIMESTAMP);
INSERT INTO delivery_modes (name, created_at) VALUES ('ECONOMY',  CURRENT_TIMESTAMP);
