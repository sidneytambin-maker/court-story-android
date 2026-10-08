# Legacy instrumentation discovers tests by class and method name.
-keep class * extends junit.framework.TestCase { *; }
# Optional remote test-storage integration is not shipped by AndroidX storage.
# The legacy in-process runner does not use it; this rule applies only to tests.
-dontwarn androidx.test.services.storage.internal.InternalTestStorage
