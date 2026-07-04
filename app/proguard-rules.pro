# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# PDFBox (note import)
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**
-dontwarn com.gemalto.jp2.**
