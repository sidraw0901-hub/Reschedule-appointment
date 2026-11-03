# CareBridge Architecture Decision Records (ADRs)

## ADR-001: Transactional Outbox Pattern for Reactive Event Propagation
* **Status**: Accepted
* **Context**: Disruption triggers must reliably spawn async reschedule jobs. In-memory event dispatchers risk losing jobs on JVM crash or thread pool rejection. Introducing external message brokers (Kafka/RabbitMQ) adds unnecessary operational complexity for a rapid-response engine.
* **Decision**: Implement a database-backed outbox table (`outbox_events`). The business mutation (e.g., `provider.status = OFFLINE`) and the outbox event are committed within the same relational database transaction. A worker polls via `SELECT ... FOR UPDATE SKIP LOCKED`.
* **Consequences**: At-least-once delivery guaranteed; zero external infrastructure dependencies; transparent observability.

## ADR-002: Strict Separation of Candidate Discovery and AI Evaluation
* **Status**: Accepted
* **Context**: LLMs cannot be trusted to perform relational joins, verify calendar overlaps, or enforce medical room equipment criteria.
* **Decision**: Deterministic SQL queries filter eligible slots to an allowlist of valid candidates. The LLM's role is strictly confined to ranking the candidate list and generating a patient-friendly phone script.
* **Consequences**: Hallucinated slots are impossible; if the LLM fails or times out, the system falls back to rule-based sorting with zero service degradation.

## ADR-003: Two-Tier Concurrency Protection for Slot Allocation
* **Status**: Accepted
* **Context**: Multiple front-desk coordinators viewing the dashboard may attempt to accept recommendations for the same newly-available slot at the exact same second.
* **Decision**: Combine JPA Optimistic Locking (`@Version`) with an atomic SQL CAS update (`WHERE status = 'AVAILABLE'`) and a database conditional unique index.
* **Consequences**: Zero double-booking risk; gracefully returns HTTP 409 Conflict with an explanation, allowing the UI to instantly refresh.
