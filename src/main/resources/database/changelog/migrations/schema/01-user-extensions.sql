--comment: create the user_extensions table to store business metadata for Keycloak user
CREATE TABLE iam.user_extensions
(
    id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    branch_id             VARCHAR(36),
    country_code          VARCHAR(2)   NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    deleted_at            TIMESTAMPTZ,
    email                 VARCHAR(255) NOT NULL,
    email_verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    first_name            VARCHAR(255),
    keycloak_user_id      VARCHAR(36)  NOT NULL,
    last_name             VARCHAR(255),
    organization_id       VARCHAR(36)  NOT NULL,
    phone_number          VARCHAR(20),
    phone_number_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    status                VARCHAR(50)  NOT NULL,
    username              VARCHAR(255) NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_user_extensions PRIMARY KEY (id),
    CONSTRAINT uq_user_extensions_keycloak_user_id UNIQUE (keycloak_user_id),
    CONSTRAINT uq_user_extensions_username UNIQUE (username),
    CONSTRAINT uq_user_extensions_email UNIQUE (email)
);

--comment: create indexes for user_extensions table
CREATE INDEX idx_user_ext_keycloak_user_id ON iam.user_extensions (keycloak_user_id);
CREATE INDEX idx_user_ext_username ON iam.user_extensions (username);
CREATE INDEX idx_user_ext_email ON iam.user_extensions (email);
CREATE INDEX idx_user_ext_status ON iam.user_extensions (status);
CREATE INDEX idx_user_ext_deleted_at ON iam.user_extensions (deleted_at) WHERE deleted_at IS NULL;

--comment: add table and column comments for user_extensions
COMMENT ON TABLE iam.user_extensions IS 'Stores business metadata for Keycloak users. Authentication data lives in Keycloak, business data lives here.';
COMMENT ON COLUMN iam.user_extensions.id IS 'Primary key - auto-generated UUID';
COMMENT ON COLUMN iam.user_extensions.branch_id IS 'Home branch for this user. Distinct from memberships.branch_id which scopes a specific membership. Nullable for unaffiliated users.';
COMMENT ON COLUMN iam.user_extensions.country_code IS 'ISO 3166-1 alpha-2 country code for business purposes';
COMMENT ON COLUMN iam.user_extensions.created_at IS 'Record creation timestamp in UTC';
COMMENT ON COLUMN iam.user_extensions.deleted_at IS 'Soft delete timestamp in UTC - should align with status=DELETED';
COMMENT ON COLUMN iam.user_extensions.email IS 'Email cached from Keycloak for query performance';
COMMENT ON COLUMN iam.user_extensions.email_verified IS 'Email verification status cached from Keycloak';
COMMENT ON COLUMN iam.user_extensions.first_name IS 'First name cached from Keycloak';
COMMENT ON COLUMN iam.user_extensions.keycloak_user_id IS 'Foreign key to Keycloak user UUID (VARCHAR(36)) - immutable reference';
COMMENT ON COLUMN iam.user_extensions.last_name IS 'Last name cached from Keycloak';
COMMENT ON COLUMN iam.user_extensions.organization_id IS 'Primary organisation reference - immutable, set at registration';
COMMENT ON COLUMN iam.user_extensions.phone_number IS 'Phone number - may differ from Keycloak phone attribute';
COMMENT ON COLUMN iam.user_extensions.phone_number_verified IS 'Phone verification status cached from Keycloak';
COMMENT ON COLUMN iam.user_extensions.status IS 'Business status (ACTIVE, SUSPENDED, DELETED, etc.) - independent of Keycloak enabled flag';
COMMENT ON COLUMN iam.user_extensions.username IS 'Username cached from Keycloak for query performance';
COMMENT ON COLUMN iam.user_extensions.version IS 'Optimistic locking version for concurrent update protection';
