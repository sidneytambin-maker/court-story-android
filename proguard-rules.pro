# Keep the shared Court Story model/API surface stable across the separately
# compiled native regression APK. Apply the same contract to release builds so
# validation exercises the shipping optimizer configuration. Names and method
# bodies may still be optimized; third-party libraries remain fully shrinkable.
-keep,allowoptimization,allowobfuscation class com.courtstory.app.** { *; }

# APIs consumed by the separately compiled accessibility and health/complication
# assertions. Preserve their public contract in release as well as validation;
# implementation bodies and names remain optimizable.
-keep,allowoptimization,allowobfuscation class com.google.common.** { public *; protected *; }
-keep,allowoptimization,allowobfuscation class androidx.health.connect.client.records.ExerciseSessionRecord { public *; }
-keep,allowoptimization,allowobfuscation class androidx.health.connect.client.records.metadata.Metadata { public *; }
-keep,allowoptimization,allowobfuscation class androidx.wear.watchface.complications.data.* { public *; protected *; }
