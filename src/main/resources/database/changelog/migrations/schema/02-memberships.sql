-- comment: create a memberships table to store user-organization membership relationships
CREATE TABLE iam.memberships
(
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    branch_id           VARCHAR(36),
    branch_name         VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ,
    is_primary          BOOLEAN      NOT NULL DEFAULT FALSE,
    keycloak_group_path VARCHAR(255) NOT NULL,
    keycloak_user_id    VARCHAR(36)  NOT NULL,
    organization_id     VARCHAR(36)  NOT NULL,
    organization_name   VARCHAR(255) NOT NULL,
    roles               TEXT[]       NOT NULL DEFAULT '{}',
    user_extension_id   UUID         NOT NULL,

    CONSTRAINT pk_memberships PRIMARY KEY (id),
    CONSTRAINT uq_memberships_user_org UNIQUE (user_extension_id, organization_id),
    CONSTRAINT uq_memberships_user_group UNIQUE (keycloak_user_id, keycloak_group_path),
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_extension_id)
        REFERENCES iam.user_extensions (id) ON DELETE CASCADE
);


-- comment: create indexes for the memberships table
CREATE INDEX idx_membership_keycloak_id ON iam.memberships (keycloak_user_id);
CREATE INDEX idx_membership_user_extension_id ON iam.memberships (user_extension_id);
CREATE INDEX idx_membership_organization_id ON iam.memberships (organization_id);
CREATE INDEX idx_membership_roles ON iam.memberships USING GIN (roles);


-- comment: add table and column comments for memberships
COMMENT ON TABLE iam.memberships IS 'User-organisation membership relationships, scoped per organisation and optionally per branch. organization_name and branch_name are denormalized from the organization service and kept in sync via OrganizationUpdated/BranchUpdated events.';
COMMENT ON COLUMN iam.memberships.id IS 'Primary key - auto-generated UUID';
COMMENT ON COLUMN iam.memberships.branch_id IS 'Branch scope of this membership. Distinct from user_extensions.branch_id which is the user''s home branch. Nullable for organisation-level memberships.';
COMMENT ON COLUMN iam.memberships.branch_name IS 'Branch display name - denormalized from organization service, kept in sync via BranchUpdated events. Nullable for organisation-level memberships.';
COMMENT ON COLUMN iam.memberships.created_at IS 'Record creation timestamp in UTC';
COMMENT ON COLUMN iam.memberships.updated_at IS 'Record last updated timestamp in UTC - captures role changes, primary flag toggles, and name syncs';
COMMENT ON COLUMN iam.memberships.is_primary IS 'Indicates the user''s primary organisation context, used when no explicit context is provided in the token';
COMMENT ON COLUMN iam.memberships.keycloak_group_path IS 'Immutable Keycloak group path representing this membership - drives RBAC in the identity provider';
COMMENT ON COLUMN iam.memberships.keycloak_user_id IS 'Keycloak user identifier (VARCHAR(36)) - denormalized for query performance without joining user_extensions';
COMMENT ON COLUMN iam.memberships.organization_id IS 'Organisation reference - immutable after creation';
COMMENT ON COLUMN iam.memberships.organization_name IS 'Organisation display name - denormalized from organization service, kept in sync via OrganizationUpdated events';
COMMENT ON COLUMN iam.memberships.roles IS 'Set of roles assigned to this user within the organisation scope';
COMMENT ON COLUMN iam.memberships.user_extension_id IS 'Foreign key to the owning user extension record';
