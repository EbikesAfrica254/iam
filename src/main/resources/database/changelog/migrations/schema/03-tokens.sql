-- comment: create token_type enum for token purpose classification
CREATE TYPE iam.token_type AS ENUM (
    'ACCOUNT_ACTIVATION',
    'EMAIL_OTP',
    'PASSWORD_RESET',
    'SMS_OTP',
    'TWO_FACTOR_AUTH'
    );

-- comment: create a tokens table to track issued tokens for audit and validation
CREATE TABLE iam.tokens
(
    id                UUID           NOT NULL DEFAULT gen_random_uuid(),
    consumed          BOOLEAN        NOT NULL DEFAULT false,
    created_at        TIMESTAMPTZ    NOT NULL,
    expires_at        TIMESTAMPTZ    NOT NULL,
    token_hash        VARCHAR(64)    NOT NULL,
    token_type        iam.token_type NOT NULL,
    user_extension_id UUID           NOT NULL,
    version           BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT pk_tokens PRIMARY KEY (id),
    CONSTRAINT uq_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_tokens_user_extension FOREIGN KEY (user_extension_id)
        REFERENCES iam.user_extensions (id) ON DELETE CASCADE
);

-- comment: create indexes for tokens table
CREATE INDEX idx_tokens_user_type ON iam.tokens (user_extension_id, token_type);
CREATE INDEX idx_tokens_active_expiry ON iam.tokens (user_extension_id, expires_at)
    WHERE consumed = false;

-- comment: add table and column comments for tokens
COMMENT ON TABLE iam.tokens IS 'Audit trail for issued tokens (OTP, verification, password reset). Tokens are hashed for security.';
COMMENT ON COLUMN iam.tokens.id IS 'Primary key - auto-generated UUID';
COMMENT ON COLUMN iam.tokens.consumed IS 'Whether the token has been used - tokens are single-use';
COMMENT ON COLUMN iam.tokens.created_at IS 'Token issuance timestamp in UTC';
COMMENT ON COLUMN iam.tokens.expires_at IS 'Token expiration timestamp in UTC';
COMMENT ON COLUMN iam.tokens.token_hash IS 'SHA-256 hex digest of the token value for secure lookup - fixed 64 characters';
COMMENT ON COLUMN iam.tokens.token_type IS 'Token purpose: ACCOUNT_ACTIVATION, EMAIL_OTP, PASSWORD_RESET, SMS_OTP, TWO_FACTOR_AUTH';
COMMENT ON COLUMN iam.tokens.user_extension_id IS 'User this token was issued to';
COMMENT ON COLUMN iam.tokens.version IS 'Optimistic locking version for concurrent update protection';