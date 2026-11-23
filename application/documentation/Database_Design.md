# Database Design
See `database/schema.sql` for the full DDL. Entities map 1:1 to tables.

| Table | Purpose | Key constraints |
|---|---|---|
| roles, users, user_roles | accounts and RBAC | `users.email` UNIQUE, `users.student_id` UNIQUE, composite PK on `user_roles` |
| item_categories | report categories | `name` UNIQUE |
| lost_items, found_items | reports (shared fields via a mapped superclass) | FK reporter, FK category, status CHECK, indexes on status/date |
| item_matches | suggested/confirmed matches | UNIQUE (`lost_item_id`,`found_item_id`) |
| claims, claim_evidence | ownership claims and evidence entries | FK found item/claimant/reviewer, status CHECK |
| recoveries | handover record | `claim_id` UNIQUE (one recovery per claim) |
| notifications | in-app notifications | `is_read`, index on user |
| activity_logs | audit trail with `suspicious` flag | index on suspicious |
| status_history | every status change: from, to, who, when, note | index (entity_type, entity_id) |

```
users 1─* lost_items *─1 item_categories 1─* found_items *─1 users
lost_items 1─* item_matches *─1 found_items
found_items 1─* claims *─1 users ; claims 1─* claim_evidence ; claims 1─1 recoveries
users 1─* notifications ; users 1─* activity_logs ; (entity) 1─* status_history
```
Transactions: claim submission, approval and recovery are `@Transactional` and lock the found-item row with
`PESSIMISTIC_WRITE`, so two concurrent actions on the same item cannot both succeed.
Timestamps: `created_at`/`updated_at` via Hibernate `@CreationTimestamp`/`@UpdateTimestamp`.
