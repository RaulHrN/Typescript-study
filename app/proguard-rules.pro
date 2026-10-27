# The app uses no reflection; the default optimized rules are enough.
# Strip verbose logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
