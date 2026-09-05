# Codebase Map — teddy-fyi-android

Orientation doc for agents. Reflects the tree as of commit `b8c7f84`, plus a follow-up cleanup pass
(list-position sync fix, `ApiRoutes`, Detekt baseline enforcement, `util/`→`utils/` merge) and the
colour-theming pass described in §9.
Verified against source, not just the other `*.md` files — several of those are stale (see
[Doc reliability](#doc-reliability)).

---

## 1. What this is

Android app (`fyi.teddy.android`) that bundles two personal-organization modules — **Todo** and
**Grocery** — over a shared **local-first Room database** with **bidirectional cloud sync** against a
Rust backend at `api-rust.teddy.fyi`. Google Sign-In, Jetpack Compose UI, two Glance home-screen
widgets.

Single Gradle module (`:app`). ~128 Kotlin files in `main`, 22 test files. No DI framework — manual
wiring via `ViewModelProvider.Factory` and object singletons.

### Stack
| Concern | Choice |
| --- | --- |
| UI | Jetpack Compose (BOM `2026.06.01`), Material3, `navigation-compose` |
| Local DB | Room `2.8.4` + KSP, schema v**35**, `exportSchema = true` |
| Network | **Ktor `3.5.1`** (OkHttp engine) + `kotlinx-serialization-json` |
| Background | WorkManager `2.11.2` (`SyncWorker`) |
| Auth | `androidx.credentials` + `googleid` → backend JWT exchange |
| Secure storage | `androidx.datastore-preferences` encrypted with **Google Tink** (`AES256_GCM`, keystore-backed) |
| Widgets | Jetpack Glance `1.1.1` |
| On-device AI | MediaPipe `tasks-genai` `0.10.35` (opt-in, see §7) |
| Static analysis | Detekt `1.23.8` (`ignoreFailures = true`), Android Lint |
| SDK | compile 37 / target 36 / **min 34**, Java 21, core library desugaring on |

---

## 2. Package layout

```
fyi/teddy/android/
├── MainActivity.kt          single Activity, NavHost, session bootstrap, login orchestration
├── auth/                    Google sign-in, UserSession, TokenStorage, EncryptedDataStore, telemetry
├── network/                 Ktor client, sync DTOs, SyncWorker, Todo/GrocerySyncManager, list invites
├── data/                    AppDatabase, DatabaseMigrations, SyncLog, UserSyncMetadata
├── repository/              TeddyRepository (health check, weather, /api/hc probes)
├── todo/{data,repository,domain,ui,util}      ui/theme/ holds the Todo app palette
├── grocery/{data,repository,domain,ui}      domain/ai/ = SLM categorizer, ui/theme/ = Grocery palette
├── ui/{navigation,screens,components,theme}  theme/ = host-shell palette only
├── widget/                  not in `main` — the `Widgets` seam per flavour, the widgets in `src/full/`
└── util/, utils/            two packages, both exist — `util/StringUtils`, `utils/{Emulator,Gms,Icon}Utils`
```

Domain isolation is real: Todo and Grocery have separate entities, DAOs, repositories, ViewModels,
and sync managers, and each owns its own colour palette. They meet only at `AppDatabase` and
`SyncWorker`. See §9 for the colour rules.

---

## 3. APIs consumed

### 3.1 `api-rust.teddy.fyi` — the primary backend

Every URL lives in [ApiRoutes.kt](app/src/main/java/fyi/teddy/android/network/ApiRoutes.kt) —
including the Open-Meteo weather call and the unauthenticated `teddy.fyi` probe. Change the host
there, not at the call sites.

| Endpoint | Method | Called from | Purpose |
| --- | --- | --- | --- |
| `/auth/login` | POST | [AuthRepository.kt:55](app/src/main/java/fyi/teddy/android/network/AuthRepository.kt:55) | Exchange Google ID token → app JWTs |
| `/auth/refresh` | POST | [NetworkClient.kt:150](app/src/main/java/fyi/teddy/android/network/NetworkClient.kt:150) | Rotate access/refresh tokens |
| `/auth/device/start` | POST | [DevicePairingRepository.kt](app/src/main/java/fyi/teddy/android/network/DevicePairingRepository.kt) | Ask for a pairing code, where there is no Google account |
| `/auth/device/poll` | POST | [DevicePairingRepository.kt](app/src/main/java/fyi/teddy/android/network/DevicePairingRepository.kt) | Wait for that code to be redeemed at `teddy.fyi/link` |
| `/api/sync` | POST | [SyncWorker.kt:113](app/src/main/java/fyi/teddy/android/network/SyncWorker.kt:113) | **The** bidirectional delta sync |
| `/api/hc` | GET | [TeddyRepository.kt:52](app/src/main/java/fyi/teddy/android/repository/TeddyRepository.kt:52) | Authed health check (debug screens) |
| `/api/assign-icon` | POST | [TodoRepository.kt:118](app/src/main/java/fyi/teddy/android/todo/repository/TodoRepository.kt:118) | Server picks an emoji/asset for a todo title |
| `/api/lists/invite` | POST | [GroceryNetworkRepository.kt:32](app/src/main/java/fyi/teddy/android/network/GroceryNetworkRepository.kt:32) | Mint a share code for a grocery list |
| `/api/lists/join` | POST | [GroceryNetworkRepository.kt:47](app/src/main/java/fyi/teddy/android/network/GroceryNetworkRepository.kt:47) | Redeem a share code |

**Wire conventions**: snake_case JSON via explicit `@SerialName` on every field. `NetworkClient.syncJson`
is configured `encodeDefaults = true, ignoreUnknownKeys = true` — the server may add fields freely.

**Headers**: `Authorization: Bearer <access_token>` (Ktor `Auth`/`bearer` plugin, `sendWithoutRequest { true }`)
plus `X-Client-UUID: <clientUuid>` injected via `defaultRequest`.

**Notable request quirk**: login and refresh both send `expires_in_secs`, derived from network type —
`60` on Wi-Fi, `3600` otherwise (`NetworkClient.getAuthTimeoutSecs`). The short Wi-Fi expiry is
deliberate: it exercises the refresh path frequently.

### 3.2 Third-party / external

| API | Where | Notes |
| --- | --- | --- |
| Google Identity (Credential Manager) | [LoginScreen.kt](app/src/main/java/fyi/teddy/android/auth/LoginScreen.kt) | `GetGoogleIdOption`, server client ID `34718544535-rem2k0n6…` |
| Google OAuth web flow | `LoginScreen.kt:100` | Custom Tabs fallback when GMS is absent; `response_type=id_token`, redirects to scheme `com.googleusercontent.apps.34718544535-a8csa0c9…` (registered in the manifest) |
| `teddy.fyi` root | `TeddyRepository.checkClusterHealth` | Unauthenticated cluster liveness probe |
| **Open-Meteo** | `TeddyRepository.fetchTemperature` | `api.open-meteo.com/v1/forecast`, **coordinates hardcoded** to 42.4154/-71.1565 (Medford/Somerville MA), °F. Parsed with `org.json`, not kotlinx |
| Gravatar / Google profile photos | `AuthUtils.extractPictureFromToken` | Fallback avatar chain: `picture` claim → Gravatar MD5 of email → `google.com/s2/photos/profile/<sub>` |

---

## 4. Auth flow (as implemented)

1. `LoginScreen` obtains a Google **ID token** — Credential Manager if GMS is available, Custom Tabs
   web flow otherwise, or a `fake_emulator_token` shortcut when `EmulatorUtils.isEmulator()`.
2. `AuthUtils.extractUserIdFromToken` base64-decodes the JWT payload locally to read `sub`.
   **No signature verification client-side** — the backend is the validator.
3. `AuthRepository.login` POSTs to `/auth/login` using `NetworkClient.loginClient`, a *deliberately
   separate* Ktor client with no `Auth` plugin, so the main client's bearer provider is never primed
   with a null token.
4. On success `MainActivity` claims orphaned local rows for the new user
   (`todoDao.claimUnownedItems(uid)`, `groceryDao.claimEverything(uid)`), saves the session, calls
   `NetworkClient.resetClient()`, then enqueues one-shot + periodic sync.
5. Tokens persist through `EncryptedDataStore` → Tink `Aead` → DataStore Preferences.
   `TokenStorage` adds an in-memory cache, a `Mutex`, and a one-shot 50 ms retry on decrypt failure.

### The second way in — pairing
On a device with no Play Services (a Fire tablet) Credential Manager has no provider to answer it,
so `LoginScreen` asks `GmsUtils.isGmsAvailable` first and offers pairing: the tablet gets a code
from `/auth/device/start`, shows it, and polls `/auth/device/poll` until somebody redeems it at
`teddy.fyi/link` on a device that does have a Google account. What comes back is the same token
pair `/auth/login` mints, so steps 4 and 5 above are unchanged.

Debug builds also carry a "Sign in with a code (debug)" link where Google sign-in works, so the
pairing path can be exercised on any device; release builds offer one way in per device.

Google sign-in needs an **Android OAuth client per applicationId** (package name + signing SHA-1),
so the `grocery` flavour needs its own entry or Credential Manager reports no credentials on a
device that has a Google account — see §7 of [AUTH_SPECIFICATION.md](AUTH_SPECIFICATION.md).

The one thing to know downstream: **a paired session has no Google ID token.** The user id comes
out of the access token's `sub`, there is no avatar, and `session.idToken` is null — so
"is somebody signed in?" is `UserSession.isSignedIn` (the access token), never `idToken != null`.
See §7 of [AUTH_SPECIFICATION.md](AUTH_SPECIFICATION.md).

### Refresh semantics — `NetworkClient.performRefreshToken`
Guarded by a `refreshMutex`. Four outcomes, and the distinction matters:
- **Stale-token check**: if the stored access token already differs from the one that 401'd, another
  thread refreshed — return the current tokens without a network call.
- **200** → save new pair, continue.
- **400 / 401** → refresh token genuinely rejected → `session.clear()`, full logout.
- **5xx or network exception** → return `null` but **retain the session**. A backend outage must not
  log the user out. Preserve this behavior when touching this function.

`session.clear()` deliberately **preserves `clientUuid`** across logout so device identity survives.

`AuthTelemetry` keeps a breadcrumb ring buffer, masks tokens, and flushes on logout — the intended
tool for debugging auth issues.

---

## 5. The sync engine (the heart of the app)

### Entities and their tracking columns
Ten Room entities; eight are syncable. Every syncable one carries UUID string PK +
`sync_state` / `version` / `is_deleted`, and **defaults to `syncState = "PENDING_INSERT"`** on
construction.

| Table | Notes |
| --- | --- |
| `todo_lists`, `todo_items` | item→list FK `SET_NULL`; supports subtasks (`parentId`), recurrence, `isDaily`, priority, icon |
| `grocery_lists`, `grocery_items` | FK `CASCADE`; `timesBought` powers "Recommended" |
| `stores`, `categories` | per-list, positioned |
| `grocery_list_members` | roles for shared lists |
| `grocery_item_store_info` | per-item-per-store price/availability |
| `sync_logs` | **local only**, 7-day retention, pruned by `SyncWorker` |
| `user_sync_metadata` | **local only**, per-user `lastSyncedAt` cursor |

### Protocol
One endpoint, `POST /api/sync`. `SyncRequest` carries `last_synced_at`, `client_id`, and **eight
parallel delta arrays** (one per syncable table). Each delta is `{id, type: INSERT|UPDATE|DELETE,
version, data}` where `data` is a `JsonElement?` — `null` for deletes. `SyncResponse` mirrors this
with `remote_*` arrays plus `success_ids` and an authoritative `server_timestamp` that becomes the
next cursor.

### Worker behavior — [SyncWorker.kt](app/src/main/java/fyi/teddy/android/network/SyncWorker.kt)
- Process-wide `syncMutex`; response application runs inside `db.withTransaction { }`.
- **First sync** (`lastSyncedAt == null`) re-labels *every* local row as `PENDING_INSERT` and uploads
  the whole database.
- **Adaptive backoff**: exponential from 30 s while charging, from 5 min on battery.
- **Metered-network guard**: a *periodic* run on a metered network is skipped unless the last success
  was >24 h ago.
- Triggers: `enqueue` (immediate — on resume, on login, on todo mutation), `enqueueDebounced` (10 s —
  grocery mutations), `schedulePeriodicSync` (every 2 h), `enqueueIfNecessary` (startup, only if
  unsynced rows exist), plus a 5-minute loop while the Grocery **Shopping** phase is on screen.
- Every run writes a `SyncLog` row with per-table sent/received counts — that table is the first place
  to look when diagnosing sync.

### State machine
`SYNCED` / `PENDING_INSERT` / `PENDING_UPDATE` / `PENDING_DELETE`, enforced in the repositories:
update flips `SYNCED`→`PENDING_UPDATE` and is otherwise idempotent; delete hard-deletes a
`PENDING_INSERT` row and soft-deletes anything else.

There is a **fifth, undocumented state: `NEED_UPDATE`**, set only from `DebugScreen` and honored in
the sync managers (`data` is sent as `null`, i.e. "re-send me this row from the server"). It is a
manual repair tool, not part of the normal lifecycle.

---

## 6. Known gotchas

- **List `position` now survives sync.** `TodoListDto` / `GroceryListDto` carry a *nullable*
  `position`: the client always uploads it, and on the way back down a `null` means "server does not
  track ordering", in which case the sync managers pass the local row's position as the fallback so
  `@Upsert` cannot reset it. Covered by `ListPositionSyncTest`. (Before this, any remote list change
  silently undid the user's reordering of spaces.)
- **`GroceryItemStoreInfo` has a composite PK** (`groceryItemId` + `storeId`) yet also carries an `id`
  field that the sync deltas key on. The `id` is not the primary key.
- **`NetworkClient` is a mutable global** — `session`, `client`, and `loginClient` are all reassignable
  `var`s on an `object`. `resetClient()` closes and rebuilds them. Tests swap `refreshClientFactory`
  and use `ktor-client-mock`.
- **Room migrations are manual, not auto.** 32 hand-written migrations (`MIGRATION_3_4` … `MIGRATION_34_35`)
  in [DatabaseMigrations.kt](app/src/main/java/fyi/teddy/android/data/DatabaseMigrations.kt) (651 lines).
  Any `@Entity` change requires bumping `AppDatabase.version`, adding a migration, and committing the
  new `app/schemas/*.json`. `AppDatabaseMigrationTest` validates the chain.
- **Detekt now fails the build on new findings.** Pre-existing findings are recorded in
  `app/detekt-baseline.xml`; `ignoreFailures = false`. Regenerate the baseline with
  `./gradlew :app:detektBaseline` only when you have deliberately accepted a finding. Inline
  `@Suppress` is still used heavily in the older files.
- **`local.properties` is no longer tracked** (it was, with another machine's `sdk.dir`). It stays
  on disk and is gitignored, as is the `lint.errors` scratch file.
- There is one util package, `utils/` (`StringUtils`, `EmulatorUtils`, `GmsUtils`, `IconUtils`).
  The old `util/` package was merged into it.
- The manifest declares only `INTERNET`. No location permission — hence the hardcoded weather coords.

---

## 7. Features worth knowing about

**Todo** — four modes (`BACKLOG`, `PLANNING`, `TODAY`, `SCHEDULED`), subtasks, daily-recurring tasks
with `TodoResetScheduler`, snooze via `TaskSchedulerUtils` (month-overflow rule: an invalid resulting
day rolls to the **1st of the following month**; snoozing never resets recurrence — see
`AI_SCHEDULING_RULES.md`, which is accurate), konfetti on completion, server-assigned icons.

**Grocery** — three phases (`NEED` → `PLANNING` → `SHOPPING`), per-store price/availability tracking,
categories with icons, recommended items driven by `timesBought`, and shared lists via invite codes.

**On-device AI** — `GroceryCategorizer` wraps MediaPipe `LlmInference`. It looks for
`filesDir/llm/model.bin`; **the model is not shipped**, so this silently no-ops unless a model is
side-loaded. Output is validated against the allowed category list to reject hallucinations.

**Widgets** — two Glance widgets (`TodoTacticalWidget`, `GroceryWidget`) with custom canvas renderers
(`TacticalHexCanvasRenderer`). They are a `full`-only feature and live entirely in
`src/full/java/.../widget/` — code, layouts, `xml/*_widget_info`, receivers and the Glance
dependency (`fullImplementation`). Shared code never touches them directly: it calls the
`fyi.teddy.android.widget.Widgets` seam, which each flavour supplies — the `full` copy delegates to
`WidgetUpdateHelper` and the widget actions, the `grocery` copy is a no-op that answers "no" to
`opensTodo` / `opensGrocery`. Refreshed from `MainActivity.onStop` / `onWindowFocusChanged(false)` /
lifecycle `ON_PAUSE` and after every repository mutation.

**DebugScreen** (1061 lines) — the largest single file. Sync-log inspection, forced re-sync,
per-table `NEED_UPDATE` marking, auth probes. First stop for manual diagnosis.

---

## 8. Build, test, run

```bash
./gradlew assembleDebug           # both flavours; or: make build
./gradlew assembleGroceryDebug    # the grocery-only build; or: make build-grocery
./gradlew testFullDebugUnitTest   # or: make test
./run_focused_tests.sh         # git-aware selective runner (what CI uses)
make install && make run       # adb install + launch on first connected device
```

There are two product flavours on one `surface` dimension, so every variant task is
flavour-qualified: `full` is Teddy FYI as it has always been, and `grocery` is the grocery list
on its own (`fyi.teddy.android.grocery`, label *Teddy Grocery*, same icon), for a tablet left
where somebody has no reason to see anybody's todo list. Both can be installed at once. There is
no flavour-less `testDebugUnitTest` task any more — the tests are shared, so
`testFullDebugUnitTest` runs all of them, and that is what `run_focused_tests.sh` calls.

What the `grocery` flavour changes, all of it driven by `BuildConfig.INCLUDE_TODO`: sign-in lands
on `Screen.Grocery` instead of the dashboard (`HOME_DESTINATION` in `MainActivity`), the `Home`
and `Todo` routes are not registered, and sign-out moves into grocery settings because there is no
dashboard to carry it. Independently of `INCLUDE_TODO`, the grocery build ships no home-screen
widgets at all — it runs on a Fire tablet, whose launcher hosts none — so both widgets, their
receivers and Glance are `full`-only. Everything
below the UI — the database, both sync managers, the todo tables — is shared and unchanged, so
the two installs are the same account's lists rather than copies.

`run_focused_tests.sh` inspects the diff against `main` and applies four rules: docs-only → skip;
build/infra touched → full suite; changes in exactly one subpackage → `--tests fyi.teddy.android.<pkg>.*`;
multiple subpackages → full suite. Pass a pattern or Gradle task to override.

Tests use JUnit4 + Robolectric + MockK + Turbine + `ktor-client-mock` + `work-testing`.
`SyncPipelineTest` (380 lines) and `GuardedRefreshTest` are the highest-value regression tests for the
areas most likely to break.

**CI** ([.github/workflows/ci.yml](.github/workflows/ci.yml)): JDK 21 → Android Lint → Detekt →
`run_focused_tests.sh`. A second workflow, **code-janitor**, runs nightly at 03:00 UTC using an
external Gemini-backed action that opens automated cleanup PRs — that explains the `janitor/*` branches
in the history.

---

## 9. Theming

Three palettes: one per app, plus the shell that hosts them. Each lives in its own package and
none of them import each other — that is deliberate, so Todo and Grocery can be split into
separate apps by moving a directory.

| Package | Applied by | Look |
| --- | --- | --- |
| `ui/theme` (`TeddyTheme`) | `MainActivity`, wrapping everything | Launcher shell: violet-black gradient, indigo panels, teal accent |
| `todo/ui/theme` (`TodoTheme`) | `TodoScreen`, `ScheduledTasksScreen`, `BattleMapTodoGrid` | Tactical HUD: near-black surfaces, indigo structure, neon-teal accents |
| `grocery/ui/theme` (`GroceryTheme`) | `GroceryScreen`, the config/store/category screens, `BronzeGroceryTile` | Bronze: warm metallic accents on neutral dark surfaces |

A feature theme fully overrides the shell, so a Todo screen looks the same wherever it is hosted.

Rules:

- **No literal colours in UI code.** Read `TodoTheme.colors` / `GroceryTheme.colors` /
  `TeddyTheme.colors` for the semantic slots, or `MaterialTheme.colorScheme` for the generic ones.
  Raw values live only in the three `*Palette.kt` files.
- Each scheme defines the full `surfaceContainer*` ladder and `errorContainer` / `onErrorContainer`
  explicitly. Material's dark defaults for those roles are purple-tinted, so any role left unset
  leaks purple into an otherwise near-black UI.
- **A component shared by both apps reads `MaterialTheme.colorScheme` only** — never a feature
  theme, and it lives in the neutral `ui/components` package rather than inside either app.
  `IconPickerDialog` is the live example: Todo uses it, and so does Grocery category management,
  so whichever theme wraps it supplies the colours.
- Widgets draw to a `Canvas` outside any composition and cannot read a CompositionLocal, so they
  use the ARGB-int mirrors `TodoWidgetPalette` / `GroceryWidgetPalette`. Keep those in step with
  their palette.
- Widget layout backgrounds have to be Android resources; they live in `values/colors.xml`,
  prefixed `todo_` / `grocery_` so they can move out with their app. The layouts that read them are
  in `src/full/res/`, and resource merging makes `main`'s colours visible there.
- Todo space colours are user-chosen and persisted, so they are hex strings, not `Color`s:
  `TodoSpaceSwatches`.
- **No literal sizes in Grocery UI either.** `GroceryTheme.metrics` carries the tile height,
  control size, column width, sign height, glyph size and item text size for the density the
  device is set to (`GroceryDensity`, chosen in grocery settings and stored per device in
  `GroceryDisplayPreferences`). Hard-coding a dp there makes one surface ignore the setting.
  All three item tiles — need, planning and shopping — and the quantity controls they open read
  it, so the setting moves the whole list rather than one phase of it.

---

## 10. Doc reliability

| File | Status |
| --- | --- |
| `AI_SCHEDULING_RULES.md` | **Accurate** — matches `TaskSchedulerUtils` |
| `ROOM_SYNC_SPECIFICATION.md` | **Mostly accurate**; specifies a `last_modified` column that entities don't actually have, and omits `NEED_UPDATE` |
| `AUTH_SPECIFICATION.md` | **Mostly accurate**; omits the `expires_in_secs` field the client actually sends |
| `AGENTS.md` | **Current** — corrected to say Ktor, splits shipped vs. open architectural work, and no longer duplicates `AI_CONTEXT.md` |
| `AI_CONTEXT.md` | **Stale framing** — written for a Gemini agent, describes Phase 2 cloud sync as upcoming; it has shipped |
| `README.md` | **Current** — describes live bidirectional sync, widgets, and the on-device categorizer |
