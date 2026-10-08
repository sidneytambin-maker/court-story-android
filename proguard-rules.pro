# Keep the shared Court Story model/API surface stable across the separately
# compiled native regression APK. Apply the same contract to release builds so
# validation exercises the shipping optimizer configuration. Names and method
# bodies may still be optimized; third-party libraries remain fully shrinkable.
-keep,allowoptimization,allowobfuscation class com.courtstory.app.** { *; }
