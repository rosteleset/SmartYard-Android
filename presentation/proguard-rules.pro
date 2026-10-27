# 1. COMMON ATTRIBUTES
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# 2. DTO & DATA MODELS (Moshi & Gson)
# Moshi uses KotlinJsonAdapterFactory (reflection), not generated adapters.
# Constructor-only rules do not protect reflected properties/accessors, and
# @param:Json annotations are not matched by the @Json <fields> rule below.
# Keep model structure and names for Moshi and Gson-backed saved preferences.
# Includes request/response subpackages, nested models and generic ApiResult<S>.
-keep class com.sesameware.domain.model.** {
    *;
}

-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class *JsonAdapter {
    public <init>(...);
}
-keep class kotlin.Metadata { *; }

# 3. RETROFIT
-keepattributes AnnotationDefault
-keep interface com.sesameware.data.remote.** { *; }
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# Retrofit 2.6.0 predates modern R8 full-mode consumer rules.
# Suspend return types are read from Continuation<? super Response<DTO>>.
# Match these classes explicitly so R8 retains their generic type metadata;
# executable members may still be shrunk, optimized and obfuscated.
-keep,allowoptimization,allowshrinking,allowobfuscation interface kotlin.coroutines.Continuation
-keep,allowoptimization,allowshrinking,allowobfuscation class retrofit2.Response

# 4. ROOM DATABASE
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# 5. PARCELABLE & KOTLIN @Parcelize
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 6. SERIALIZABLE
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 7. APPLICATION ENUMS
# Gson/Moshi reflect on enum constants. Preserve their names for JSON and saved prefs.
# Also keep value fields, accessors, values()/valueOf(), Kotlin entries and static
# methods/fields (including Companion and @JvmStatic bridges). Matches nested enums too.
# Intentionally conservative: only application enums, not third-party library enums.
-keep enum com.sesameware.** {
    *;
}

# Kotlin companion methods live in a separate class, not in the enum itself.
# Capture the enum name (including enclosing classes) and keep only its companion.
-if enum com.sesameware.**
-keep class com.sesameware.<1>$Companion {
    *;
}
