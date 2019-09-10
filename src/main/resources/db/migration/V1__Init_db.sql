CREATE TABLE points (
  user_id UUID PRIMARY KEY,
  total INT NOT NULL CHECK (total >= 0),
  total_expiring INT NOT NULL CHECK (total_expiring >= 0),
  closest_expiring_timestamp TIMESTAMP NULL,
  closest_expiring_amount INT NULL
);

CREATE TABLE transaction(
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES points(user_id),
  amount INT NOT NULL CHECK (amount != 0),
  timestamp TIMESTAMP NOT NULL,
  expires TIMESTAMP NULL,
  total INT NOT NULL,
  comment VARCHAR(1024) NULL
);

CREATE TABLE pending_transaction(
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES points(user_id),
  amount INT NOT NULL CHECK (amount != 0),
  timestamp TIMESTAMP NOT NULL,
  expires TIMESTAMP NULL,
  total INT NOT NULL,
  comment VARCHAR(1024) NULL
);

CREATE TABLE expiring_points(
  transaction_id UUID NOT NULL REFERENCES transaction(id),
  user_id UUID NOT NULL REFERENCES points(user_id),
  amount INT NOT NULL CHECK (amount > 0),
  expires TIMESTAMP NOT NULL
);