# Add project specific ProGuard rules here.

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class com.example.MyJavaScriptInterface {
#   public *;
#}

# Preserve line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide original source file name.
#-renamesourcefileattr "ObfuscatedFile"


# Keep all members in okhttp3 classes (example for third-party libraries).
-keep class com.squareup.okhttp3.** { *; }

# Keep all members in Guava ListenableFuture (example for third-party libraries).

# Keep serialization methods.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    private void readObjectNoData();
}
