package fyi.teddy.android.data

import android.util.Log
import androidx.room.Dao
import androidx.room.Query
import fyi.teddy.android.network.SyncResponse
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Moving this device's rows from the id the server used to call the account to the one it calls
 * it now -- and, just as importantly, not doing that a moment before the server has.
 *
 * ## What is changing on the server
 *
 * An account used to be named by the identifier its sign-in provider gave it: Google's own `sub`,
 * which this client reads straight out of Google's ID token and has never had to ask us for. That
 * is being replaced by a **surrogate** -- an opaque UUID the server generates, derived from
 * nothing -- because the provider subject is disclosed to every co-member of a shared list
 * through six payload fields, and is computable offline by anyone who learns it.
 *
 * The server is part-way through:
 *
 * 1. The surrogate column exists and is backfilled.
 * 2. It is returned as `user_uuid` from `/auth/login`, `/auth/refresh` and the pairing poll.
 *    **This is where we are.**
 * 3. Clients store it and know what to do when it differs. (This release.)
 * 4. The server starts *sending* it: in the `user_id` of every row a sync returns.
 *
 * ## Why a mismatch is not the signal
 *
 * From stage 2 the stored `user_uuid` and the id this app runs on differ **always** -- a UUID
 * against a Google subject -- and go on differing until stage 4. That is the expected state, not
 * a signal, and re-keying the local rows on it would be silently destructive: every query scopes
 * on `userId` ([fyi.teddy.android.grocery.data.GroceryDao] and friends), while every row arriving
 * from the server would still be keyed by the subject. The lists would empty themselves and the
 * rows would still be there, correct and invisible.
 *
 * So the trigger is evidence that stage 4 has actually happened: a row **the server sent us**
 * carrying the surrogate. [cutoverEvidence] looks for exactly that and nothing else. Until it
 * appears this release changes no behaviour at all; it only stores a field.
 *
 * ## What is deliberately not migrated
 *
 * The id sent to `/auth/login` and `/auth/refresh` ([fyi.teddy.android.auth.UserSession.authUserId]).
 * Refresh finds the session by `(user_id, client_uuid)` from an unauthenticated body, and by
 * `user_id` it means the provider subject. Sending a surrogate there fails the lookup and signs
 * the device out.
 */
object UserIdMigration {
    private const val TAG = "UserIdMigration"

    /**
     * Whether [response] proves the server has started keying rows by the surrogate.
     *
     * True when any row it sent us is owned by [userUuid]. One row is enough: the server does not
     * re-key one account's rows and not another's, and every table's payload carries the same
     * `user_id` field (`owner_id` on a list). Deletes carry no data and are skipped.
     *
     * A false answer is always safe -- it means one more sync runs on the old key, and the next
     * one that carries a row will trigger. A false *positive* is what would hurt, which is why
     * this asks about the surrogate specifically rather than about anything having changed.
     */
    fun cutoverEvidence(response: SyncResponse, userUuid: String?): Boolean {
        if (userUuid.isNullOrBlank()) return false
        val payloads = buildList {
            addAll(response.remoteTodoChanges.map { it.data })
            addAll(response.remoteTodoListChanges.map { it.data })
            addAll(response.remoteGroceryChanges.map { it.data })
            addAll(response.remoteGroceryListChanges.map { it.data })
            addAll(response.remoteGroceryListMemberChanges.map { it.data })
            addAll(response.remoteStoreChanges.map { it.data })
            addAll(response.remoteCategoryChanges.map { it.data })
            addAll(response.remoteGroceryItemStoreInfoChanges.map { it.data })
        }
        return payloads.any { it.namesOwner(userUuid) }
    }

    private fun JsonElement?.namesOwner(userUuid: String): Boolean {
        val fields = (this as? JsonObject ?: return false).jsonObject
        return OWNER_FIELDS.any { (fields[it] as? JsonPrimitive)?.contentOrNullSafe() == userUuid }
    }

    private fun JsonPrimitive.contentOrNullSafe(): String? = if (isString) content else null

    private val OWNER_FIELDS = listOf("user_id", "owner_id")

    /**
     * Re-keys every local row from [from] to [to].
     *
     * Runs inside the caller's transaction -- half a migration is a half-invisible database -- and
     * touches only the ownership columns. `sync_state`, `version` and `is_deleted` are left alone:
     * the server is the one renaming the account, so a row that follows it is not a local change
     * and must not be republished as one.
     *
     * The `user_sync_metadata` cursor moves with the rows. Leaving it behind would make the next
     * sync a first sync, which re-labels every local row `PENDING_INSERT` and uploads the whole
     * database.
     *
     * @return how many rows moved, for the log.
     */
    suspend fun migrate(dao: UserIdMigrationDao, from: String, to: String): Int {
        var moved = 0
        moved += dao.reassignTodoItems(from, to)
        moved += dao.reassignTodoLists(from, to)
        moved += dao.reassignGroceryItems(from, to)
        moved += dao.reassignGroceryListOwners(from, to)
        moved += dao.reassignGroceryListMembers(from, to)
        moved += dao.reassignStores(from, to)
        moved += dao.reassignCategories(from, to)
        moved += dao.reassignStoreInfo(from, to)

        // The cursor is keyed by user id, so it is moved rather than updated: an existing row
        // under the new id would collide on the primary key.
        dao.dropSyncCursor(to)
        dao.reassignSyncCursor(from, to)

        // No id is logged, in either direction: which account this device holds is a fact about a
        // person, and logcat is readable by anything with the phone plugged in.
        Log.i(TAG, "Account id migrated; $moved rows re-keyed.")
        return moved
    }
}

/**
 * The re-key itself, as raw updates.
 *
 * Its own DAO rather than methods spread across [fyi.teddy.android.todo.data.TodoDao] and
 * [fyi.teddy.android.grocery.data.GroceryDao] because it is one operation that happens once in
 * the life of an install, spans every table, and should be findable in one piece. It adds no
 * schema of its own, so no database version bump comes with it.
 */
@Dao
interface UserIdMigrationDao {
    @Query("UPDATE todo_items SET userId = :to WHERE userId = :from")
    suspend fun reassignTodoItems(from: String, to: String): Int

    @Query("UPDATE todo_lists SET userId = :to WHERE userId = :from")
    suspend fun reassignTodoLists(from: String, to: String): Int

    @Query("UPDATE grocery_items SET userId = :to WHERE userId = :from")
    suspend fun reassignGroceryItems(from: String, to: String): Int

    @Query("UPDATE grocery_lists SET ownerId = :to WHERE ownerId = :from")
    suspend fun reassignGroceryListOwners(from: String, to: String): Int

    @Query("UPDATE grocery_list_members SET userId = :to WHERE userId = :from")
    suspend fun reassignGroceryListMembers(from: String, to: String): Int

    @Query("UPDATE stores SET userId = :to WHERE userId = :from")
    suspend fun reassignStores(from: String, to: String): Int

    @Query("UPDATE categories SET userId = :to WHERE userId = :from")
    suspend fun reassignCategories(from: String, to: String): Int

    @Query("UPDATE grocery_item_store_info SET userId = :to WHERE userId = :from")
    suspend fun reassignStoreInfo(from: String, to: String): Int

    @Query("DELETE FROM user_sync_metadata WHERE userId = :userId")
    suspend fun dropSyncCursor(userId: String): Int

    @Query("UPDATE user_sync_metadata SET userId = :to WHERE userId = :from")
    suspend fun reassignSyncCursor(from: String, to: String): Int
}
