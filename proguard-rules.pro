# Keep the shared Court Story model/API surface stable across the separately
# compiled native regression APK. Apply the same contract to release builds so
# validation exercises the shipping optimizer configuration. Names and method
# bodies may still be optimized; third-party libraries remain fully shrinkable.
-keep,allowoptimization,allowobfuscation class com.courtstory.app.** { *; }
# Accessibility Test Framework's Material/AppCompat widgets implement Core
# interfaces even when the app uses platform widgets. Preserve that shared API
# contract for the separately loaded test APK on older Android class loaders.
-keep,allowoptimization,allowobfuscation class androidx.core.** { public *; protected *; }

# APIs consumed by the separately compiled accessibility and health/complication
# assertions. Preserve their public contract in release as well as validation;
# implementation bodies and names remain optimizable.
# Exact Guava types referenced by Accessibility Test Framework 4.1.1, plus their
# Guava ancestors. No blanket retention of reflection or unrelated utilities.
-keep,allowoptimization,allowobfuscation class com.google.common.annotations.Beta { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.annotations.VisibleForTesting { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.base.Ascii { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.base.Function { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.base.Joiner { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.base.Preconditions { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.base.Predicate { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.base.Predicates { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.base.Splitter { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.base.StandardSystemProperty { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.base.Strings { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.AbstractBiMap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.AbstractListMultimap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.AbstractMapBasedMultimap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.AbstractMultimap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ArrayListMultimap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ArrayListMultimapGwtSerializationDependencies { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.BiMap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ClassToInstanceMap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.Collections2 { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.EnumBiMap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.FluentIterable { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ForwardingMap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ForwardingObject { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.HashBiMap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableBiMap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableBiMap$Builder { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableClassToInstanceMap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableClassToInstanceMap$Builder { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableCollection { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableCollection$ArrayBasedBuilder { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableCollection$Builder { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableList { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableList$Builder { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableMap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableMap$Builder { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableSet { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ImmutableSet$Builder { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.Iterables { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.ListMultimap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.Lists { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.Multimap { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.Range { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.RangeGwtSerializationDependencies { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.Sets { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.Sets$SetView { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.collect.UnmodifiableIterator { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.primitives.Ints { public *; protected *; }
-keep,allowoptimization,allowobfuscation class com.google.common.primitives.IntsMethodsForWeb { public *; protected *; }
-keep,allowoptimization,allowobfuscation class androidx.health.connect.client.records.ExerciseSessionRecord { public *; }
-keep,allowoptimization,allowobfuscation class androidx.health.connect.client.records.metadata.Metadata { public *; }
-keep,allowoptimization,allowobfuscation class androidx.wear.watchface.complications.data.* { public *; protected *; }
