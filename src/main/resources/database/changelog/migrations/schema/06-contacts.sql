-- comment: create the contacts table to store pre-identity contacts harvested from external sources
CREATE TABLE iam.contacts
(
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    branch_id         VARCHAR(36),
    created_at        TIMESTAMPTZ  NOT NULL,
    expires_at        TIMESTAMPTZ  NOT NULL,
    organization_id   VARCHAR(36)  NOT NULL,
    phone_number      VARCHAR(20)  NOT NULL,
    source_reference  VARCHAR(255) NOT NULL,
    source_type       VARCHAR(50)  NOT NULL,
    status            VARCHAR(50)  NOT NULL DEFAULT 'UNRESOLVED',
    user_extension_id UUID,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_contacts PRIMARY KEY (id),
    CONSTRAINT uq_contacts_phone_org UNIQUE (phone_number, organization_id),
    CONSTRAINT fk_contacts_user_extension FOREIGN KEY (user_extension_id)
        REFERENCES iam.user_extensions (id)
);

-- comment: create indexes for the contacts table
CREATE INDEX idx_contacts_phone_number ON iam.contacts (phone_number);
CREATE INDEX idx_contacts_organization_id ON iam.contacts (organization_id);
CREATE INDEX idx_contacts_branch_id ON iam.contacts (branch_id);
CREATE INDEX idx_contacts_source_ref ON iam.contacts (source_reference);
CREATE INDEX idx_contacts_status ON iam.contacts (status);
CREATE INDEX idx_contacts_expires_at ON iam.contacts (expires_at) WHERE status = 'UNRESOLVED';

-- comment: add table and column comments for contacts
COMMENT ON TABLE iam.contacts IS 'Stores pre-identity contacts harvested from external sources before the person has presented themselves to the system.';
COMMENT ON COLUMN iam.contacts.id IS 'Primary key - auto-generated UUID';
COMMENT ON COLUMN iam.contacts.branch_id IS 'Branch context from which this contact was sourced - nullable for organisation-level sources';
COMMENT ON COLUMN iam.contacts.created_at IS 'Record creation timestamp in UTC';
COMMENT ON COLUMN iam.contacts.expires_at IS 'Timestamp after which this contact is considered stale and eligible for cleanup';
COMMENT ON COLUMN iam.contacts.organization_id IS 'Organisation that originated this contact - scopes deduplication and ownership';
COMMENT ON COLUMN iam.contacts.phone_number IS 'E.164 formatted phone number - the sole identity signal at this stage';
COMMENT ON COLUMN iam.contacts.source_reference IS 'Identifier of the originating source record - e.g. documentId for CSV manifests';
COMMENT ON COLUMN iam.contacts.source_type IS 'Origin channel of this contact (CSV_MANIFEST, WHATSAPP, etc.)';
COMMENT ON COLUMN iam.contacts.status IS 'Lifecycle status: UNRESOLVED - not yet claimed, CLAIMED - converted to full identity, EXPIRED - past expiry without claim';
COMMENT ON COLUMN iam.contacts.user_extension_id IS 'Populated on claim - foreign key to the provisioned UserExtension record';
COMMENT ON COLUMN iam.contacts.version IS 'Optimistic locking version - guards against concurrent claim attempts';