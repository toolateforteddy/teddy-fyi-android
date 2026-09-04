package fyi.teddy.android.network

import android.content.Context
import android.util.Log
import androidx.work.WorkManager

/**
 * A short, deliberate pause on synchronisation.
 *
 * It exists so a change the user can still take back — a delete sitting behind an undo
 * snackbar — does not get pushed to the server or overwritten by a remote copy of the
 * same row mid-undo. [SyncWorker] consults this before enqueuing and before running.
 *
 * The hold carries a deadline rather than relying purely on being released. If whoever
 * took it never lets go (the screen went away, the process died), sync resumes on its
 * own instead of staying off forever.
 */
object SyncHold {

    private const val TAG = "SyncHold"

    @Volatile
    private var heldUntilMillis = 0L

    fun isHeld(): Boolean = System.currentTimeMillis() < heldUntilMillis

    /**
     * Parks sync for [durationMillis] and clears any sync already waiting in the queue.
     * Extends an existing hold rather than shortening it.
     */
    @Suppress("TooGenericExceptionCaught")
    fun hold(context: Context, durationMillis: Long) {
        heldUntilMillis = maxOf(heldUntilMillis, System.currentTimeMillis() + durationMillis)
        try {
            WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
        } catch (e: Exception) {
            Log.w(TAG, "Could not cancel queued sync while taking the hold.", e)
        }
    }

    /** Lifts the hold and syncs immediately, since whatever it was protecting is now settled. */
    @Suppress("TooGenericExceptionCaught")
    fun release(context: Context) {
        if (heldUntilMillis == 0L) return
        heldUntilMillis = 0L
        try {
            SyncWorker.enqueue(context)
        } catch (e: Exception) {
            Log.w(TAG, "Could not enqueue the sync deferred by the hold.", e)
        }
    }
}
