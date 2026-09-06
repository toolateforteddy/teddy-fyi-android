# Turning the surrogate id on: the runbook

[AUTH_SPECIFICATION.md](AUTH_SPECIFICATION.md) records the wire contract — the two user ids and
which endpoint means which.
[UserIdMigration.kt](app/src/main/java/fyi/teddy/android/data/UserIdMigration.kt) records why the
local re-key waits for evidence. This file is the ordered list of what still has to happen before
the server can actually key accounts by the surrogate, and how we would know it went wrong.

Cross-repo. The flip is run from `teddy-fyi-api-rust`; `ScribbleRoute-Labs/toybox` has its own
runbook (`Context/USER_IDENTITY_CUTOVER.md`) whose steps differ, because the user id is local
bookkeeping there and a wire value here.

## Where this repo already is

Shipped in #34 and merged:

| Piece | What it does |
| --- | --- |
| `UserSession.userUuid` | stores `user_uuid` from login, refresh and the pairing poll |
| `UserSession.authUserId` | the provider subject — what `/auth/login` and `/auth/refresh` are told. Never migrated |
| `UserSession.userId` | the local key on every row and in every query |
| `UserIdMigration.cutoverEvidence` | true only when the server has sent us a row owned by the surrogate |
| `UserIdMigration.migrate` + `UserIdMigrationDao` | the re-key across eight tables and the sync cursor |
| `SyncWorker.migrateUserIdIfCutover` | runs it inside the response transaction, before any row is applied |

Nothing in it changes behaviour until the server sends such a row. That is deliberate: the
surrogate and the subject differ from the day the server starts *returning* the surrogate, and
that difference is not a signal.

## The end state

`users.id`, `sessions.user_id` and every child table on the server hold the surrogate; tokens
carry it as `sub`; the grocery payloads carry it in the six fields that leak the Google subject
to co-members today; and this client keys its rows by it. The last step — sending it in the
login and refresh bodies — comes after everything else and is step 7.

---

## 1. Close the pairing hole — this repo, before the flip

**The one piece of client work that is actually required here.**

`DevicePairingRepository.classify` reads the id out of **our own** access token
(`AuthUtils.extractUserIdFromToken(tokens.accessToken)`), and `MainActivity`'s `onPaired` assigns
it straight to `session.userId`. After the flip that token carries the surrogate, so a device
that pairs again lands with `session.userId` already equal to the surrogate — while its existing
rows are still keyed by the Google subject.

The migration cannot rescue them. `SyncWorker.migrateUserIdIfCutover` computes
`from = session.userId`, so once that is the surrogate the guard `surrogate != sessionUserId`
is false, `migrate` never runs, and every list, item, store and category the household already
had sits in the database under an id nothing queries by. Signing out is not the escape hatch
either: it is the *re-pair without signing out* that does this.

The fix, in the pairing sign-in rather than in the worker, because that is where the old value is
still in hand:

* Before overwriting `session.userId`, keep what it was.
* If the incoming id differs from it, re-key from the old one — `UserIdMigration.migrate` already
  does exactly that and is idempotent.
* Guard it on the same account: the stored `authUserId` matching, or the incoming id equalling
  the stored `userUuid`. A different account signing in must not inherit these rows.
* Pin it with a test alongside `UserIdMigrationTest`: a pairing sign-in whose token carries the
  surrogate, on a database keyed by the subject, ends with the rows under the surrogate.

Until this lands, the flip strands paired devices.

## 2. Make adoption measurable — this repo and the API

No build identifier reaches the server. `SyncRequest` carries `last_synced_at`, `client_id` and
the eight delta arrays; the auth bodies carry ids and tokens. So "have enough installs upgraded?"
is answerable only from Play's install counts, which is not the same question.

It matters more here than in toybox, because of what an un-upgraded install does at the flip
(next section). Cheapest fix: an `X-Client-Version` header from `NetworkClient`, where every
authenticated call already passes, recorded server-side against the client uuid.

## 3. Understand what an old install does at the flip

Worth writing down before scheduling anything, because it sets how much step 2 matters.

An install without #34 keys its rows by the Google subject and has nowhere to put a `user_uuid`.
After the flip the server sends rows owned by the surrogate; `GroceryItemDto.toEntity()` and
friends file them under it verbatim; every query still scopes by the subject. The result is a
household whose grocery list looks **empty and keeps re-filling with rows they cannot see** —
not an error, not a crash, and not obviously our fault from the user's side. Their own writes
still upload correctly, which makes it worse rather than better: nothing surfaces.

There is no forced-upgrade gate in this app. So the flip's safety is exactly the fraction of
installs carrying #34 plus step 1, and step 2 is how that stops being a guess.

## 4. Teach `/auth/refresh` both ids — API, and the hard prerequisite

`sessions` is `PRIMARY KEY (user_id, client_uuid)` with `user_id TEXT`, and `refresh_handler`
selects the row by that pair from the request body verbatim. This client sends
`session.authUserId` there — the Google subject, deliberately and permanently, since #34.

When `sessions.user_id` becomes the surrogate, that lookup misses, the endpoint answers `401`,
and `NetworkClient` treats a `401`/`400` from refresh as the end of the session and calls
`session.clear()`. Every install in the field is signed out on its next refresh — including,
per step 1, paired devices with no Google sign-in of their own.

So before the flip, `refresh_handler` must resolve a non-UUID body `user_id` through
`(provider, subject)`, the same shim `2026-09-05_identity_model.md` §6 defines for `Claims.sub`.
§6 covers the token; §7 notes the body id is load-bearing but stops short of carrying the shim to
it. It needs both, for the same lifetime, and it must accept **either** shape rather than switch:
sessions minted before the flip send a subject and sessions minted after it (on the pairing
route) send a UUID, from the same build.

## 5. Flip — API

Tokens carry the surrogate as `sub`; the six grocery payload fields carry it in place of the
subject. Nothing here ships alongside it.

What this client then does, on its own, at the next sync: `cutoverEvidence` sees a row owned by
the stored surrogate, `migrate` re-keys the eight tables and moves the `user_sync_metadata`
cursor with them, all inside the transaction that applies the response, and `session.userId`
follows once it commits.

One case that looks like a bug and is not: an install that never receives a remote row — a
single-device household with no co-members and no changes from elsewhere — never sees evidence
and never migrates. Its rows and its queries agree with each other, so nothing is broken, and the
first row that ever arrives migrates it. Do not "fix" this by weakening the trigger.

## 6. Go / no-go, and verifying it

Flip when:

- [ ] Step 1 is merged and released.
- [ ] Step 4 is deployed, with a test proving a subject in the refresh body still resolves.
- [ ] Adoption is at whatever bar step 2 lets us set, or a wait long enough to stand in for it.

Then verify on a real device rather than from the test suite:

1. **A signed-in install from before the flip.** Its lists still show their contents; a new item
   still syncs; the `SyncLog` row for that run shows the usual counts rather than a burst.
2. **The migration ran once and only once.** `Account id migrated; N rows re-keyed.` in logcat,
   on the first sync after the flip and no other.
3. **The cursor followed.** The sync after the migration is a delta, not a first sync. A first
   sync here re-labels every local row `PENDING_INSERT` and re-uploads the database — visible in
   `SyncLog` as a sent-count spike, and the sign that `user_sync_metadata` was left behind.
4. **A token refresh survives it** — the step 4 shim, exercised for real.
5. **A paired device**, per step 1.

**What failure looks like:** a list that renders empty while `grocery_items` still has the rows.
Compare `userId` on a row against the session's `userId`; two values is this migration and
nothing else.

## 7. Afterwards

* Once the shim from step 4 has outlived every session predating the flip, this client can send
  the surrogate in the login and refresh bodies and `authUserId` can be retired. Not before: the
  shim is what keeps the old shape working, and removing our end early is what makes it load-
  bearing in the wrong direction.
* `cutoverEvidence` and `UserIdMigration` stay until every install that could still be carrying
  subject-keyed rows is gone. Given step 5's never-migrates case, that is a date rather than a
  measurement.
* Recovery lever, if a device ends up mis-keyed anyway: clearing its `user_sync_metadata` row
  forces a first sync, which re-pulls the account. It also re-uploads everything local, so it is
  a repair and not a routine, and re-keying the rows directly is the lighter fix where the old id
  is still known.
