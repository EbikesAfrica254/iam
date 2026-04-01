-- update outbox status enum
ALTER TABLE iam.outbox DROP CONSTRAINT chk_outbox_status;
ALTER TABLE iam.outbox
    ADD CONSTRAINT chk_outbox_status
        CHECK (status IN ('DEAD_LETTER', 'FAILED', 'PENDING', 'SENT'));
