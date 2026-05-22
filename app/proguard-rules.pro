# Preserve line numbers for better crash reports in Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firebase / Firestore rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep your data models exactly as they are for Firestore serialization
-keep class com.purawale.app.Member { *; }
-keep class com.purawale.app.Memory { *; }
-keep class com.purawale.app.Comment { *; }
-keep class com.purawale.app.Recipe { *; }
-keep class com.purawale.app.Message { *; }
-keep class com.purawale.app.Discussion { *; }
-keep class com.purawale.app.Tradition { *; }
-keep class com.purawale.app.Milestone { *; }
-keep class com.purawale.app.DeletionRequest { *; }
-keep class com.purawale.app.RelationshipOverride { *; }
-keep class com.purawale.app.ChatChannel { *; }
-keep class com.purawale.app.PollOption { *; }
-keep class com.purawale.app.AppNotification { *; }

# Also keep any classes used in Room
-keep class com.purawale.app.AppDatabase { *; }
-keep interface com.purawale.app.MemberDao { *; }

# Keep WorkManager Workers
-keep class com.purawale.app.BirthdayWorker { *; }

# Keep Utils for SHA-256 hashing
-keep class com.purawale.app.UtilsKt { *; }
