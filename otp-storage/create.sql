CREATE TABLE IF NOT EXISTS otp_challenges (
  challenge_id VARCHAR(64) PRIMARY KEY,
  user_id      VARCHAR(128) NOT NULL,
  otp_hash     VARCHAR(512) NOT NULL,
  expires_at   TIMESTAMP NOT NULL,
  attempts     INT NOT NULL,
  resends      INT NOT NULL,
  last_sent_at TIMESTAMP NULL,
  status       VARCHAR(16) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_otp_user_status_sent
  ON otp_challenges(user_id, status, last_sent_at);
