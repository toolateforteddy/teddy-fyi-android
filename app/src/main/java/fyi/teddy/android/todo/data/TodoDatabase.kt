package fyi.teddy.android.todo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TodoItem::class], version = 1, exportSchema = false)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var Instance: TodoDatabase? = null

        fun getDatabase(context: Context): TodoDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, TodoDatabase::class.java, "todo_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
