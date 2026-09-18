# POS QuickBooks Journal Core

Reusable Java 17 library that validates normalized POS daily summaries, creates balanced journal entries, and posts them through a replaceable `QuickBooksClient`.

## Safety properties

- `BigDecimal` money with explicit currency
- reconciliation before posting
- deterministic idempotency keys to prevent duplicate days
- configurable QuickBooks account IDs and tender mappings
- no secrets or business-specific values in source
- in-memory QuickBooks implementation for development and tests

The real OAuth/QuickBooks Online client is intentionally deferred until production credentials and sandbox testing are available.

## Local use

```bash
mvn clean install
```

The connector repositories consume `io.github.cja769:pos-qbo-journal-core:0.1.0-SNAPSHOT`.
