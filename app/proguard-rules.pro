# Room Database Rules
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Dao { *; }

# Kotlinx Serialization & Hilt Rules
-keep class com.varsel.expensetracker.data.local.entity.** { *; }
-keep class com.varsel.expensetracker.domain.model.** { *; }
