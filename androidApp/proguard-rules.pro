# Soine — Android release shrinker rules
#
# This file is the minimum keep set required after R8 shrinking.
# Library consumer rules (Compose, Kotlin, AndroidX) are pulled in
# automatically from the respective AARs.
#
# See docs/security-audit-2026-10.md for the audit that produced these
# rules.

# Preserve Kotlin metadata for reflection-based tools and serialization
# helpers. Our codecs call enum valueOf(...) directly, which R8 keeps
# by default, but the metadata is also useful for crash triage tooling.
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Health Connect client is reflection-driven internally. Keep its
# public surface so dynamic permission and record decoding keep working
# after shrinking.
-keep class androidx.health.connect.** { *; }
-keep interface androidx.health.connect.client.** { *; }
-dontwarn androidx.health.connect.**

# Keep our enum constants that the snapshot codec and sound-event codec
# resolve via Enum.valueOf. R8 normally keeps enum names automatically;
# these are explicit safety nets.
-keepclassmembers enum app.soine.sleep.SleepSessionStatus {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum app.soine.sleep.SleepSessionSource {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum app.soine.sound.SoundEventType {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum app.soine.sound.SoundEventSource {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Strip android.util.Log calls from release builds. The audit confirmed
# the codebase does not currently emit logs in production code, but
# this prevents a regression from leaking identifiers if a future PR
# adds debug-only logging.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Keep Compose runtime metadata for tooling. Compose libraries bundle
# their own consumer rules; this line keeps reflective entry points
# stable across AGP upgrades.
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**