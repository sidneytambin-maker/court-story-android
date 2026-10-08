# Legacy instrumentation discovers tests by class and method name.
-keep class * extends junit.framework.TestCase { *; }
# Preserve optional class-loading checks in the accessibility test framework.
# The target app is still fully optimized with the shipping release rules.
-dontoptimize
# Optional remote test-storage integration is not shipped by AndroidX storage.
# The legacy in-process runner does not use it; this rule applies only to tests.
-dontwarn androidx.test.services.storage.internal.InternalTestStorage
