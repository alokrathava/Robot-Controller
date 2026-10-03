# Consumer rules for :core:database

# Preserve Room Database, Entities, and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep class com.alokrathava.database.entity.** { *; }
-keep class com.alokrathava.database.dao.** { *; }
-keep interface com.alokrathava.database.dao.** { *; }
-keep class com.alokrathava.database.RobotDatabase { *; }
