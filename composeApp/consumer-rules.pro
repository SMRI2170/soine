# Soine — Compose shared module consumer rules
#
# These rules travel with the AAR produced by composeApp and applied to
# the consuming androidApp module. They cover symbols owned by the
# shared module that downstream R8 might otherwise remove.
#
# See docs/security-audit-2026-10.md.

# The shared snapshot codecs use Enum.valueOf(...) to deserialize
# persisted data, including older versions upgraded by SchemaMigration.
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

# Keep data records that the codec instantiates reflectively in
# defensive tests. The runtime path does not require reflection, so
# these rules only protect the test surface.
-keep class app.soine.sleep.SleepSessionRecord { *; }
-keep class app.soine.relationship.CompanionProgress { *; }
-keep class app.soine.sound.SoundEvent { *; }
-keep class app.soine.dream.DreamDiscovery { *; }