-- comment: remove denormalized organization_name and branch_name from memberships table
-- these fields are now resolved at read time via the organization service
ALTER TABLE iam.memberships DROP COLUMN organization_name;
ALTER TABLE iam.memberships DROP COLUMN branch_name;
