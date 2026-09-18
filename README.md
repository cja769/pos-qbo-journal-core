# POS QuickBooks Journal Core

Reusable Java 17 library that validates normalized POS daily summaries, creates balanced journal entries, and reconciles them through a replaceable `QuickBooksClient`.

## Safety properties

- `BigDecimal` money with explicit currency
- reconciliation before posting
- deterministic idempotency keys to prevent duplicate days
- source fingerprints that distinguish create, update, and unchanged results
- atomic flat-file receipts for audit history and restart safety
- an exclusive process lock for single-instance deployments
- calendar reconciliation planning for daily, Sunday, month, quarter, and year checks
- configurable QuickBooks account IDs and tender mappings
- no secrets or business-specific values in source
- in-memory QuickBooks implementation for development and tests

The transport-independent client contract supports lookup, creation, and automatic updates. The OAuth/HTTP transport remains isolated from accounting logic and can be tested against a mock server before sandbox credentials are supplied.

## Reconciliation calendar

`ReconciliationPlanner` always includes the previous completed day. It additionally includes:

- the previous seven completed days on Sunday
- the previous calendar month on the first of a month
- the previous calendar quarter on January 1, April 1, July 1, and October 1
- the previous calendar year on January 1

Overlapping windows are deduplicated. A matching remote fingerprint causes no write; a changed fingerprint updates the existing journal; a missing remote journal is recreated.

## Flat-file state

`FileSyncReceiptStore` writes one checksummed receipt per source idempotency key using a temporary file, filesystem sync, and atomic replacement. Mount its directory on persistent storage. Receipts are an audit trail; remote QuickBooks lookup remains authoritative.

## Local use

```bash
mvn clean install
```

The connector repositories consume `io.github.cja769:pos-qbo-journal-core:0.1.0-SNAPSHOT`.
