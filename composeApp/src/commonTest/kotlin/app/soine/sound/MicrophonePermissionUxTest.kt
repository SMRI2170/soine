package app.soine.sound

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MicrophonePermissionUxTest {

    @Test
    fun grantedPermissionCanEnableImmediately() {
        assertEquals(
            MicrophoneEnableAction.ENABLE,
            microphoneEnableAction(MicrophonePermissionState.GRANTED),
        )
    }

    @Test
    fun firstRequestAndRetryShowPrePermissionCopy() {
        assertEquals(
            MicrophoneEnableAction.SHOW_PRE_PERMISSION,
            microphoneEnableAction(MicrophonePermissionState.NOT_REQUESTED),
        )
        assertEquals(
            MicrophoneEnableAction.SHOW_PRE_PERMISSION,
            microphoneEnableAction(MicrophonePermissionState.DENIED),
        )
    }

    @Test
    fun permanentDenialRoutesToSettings() {
        assertEquals(
            MicrophoneEnableAction.OPEN_SETTINGS,
            microphoneEnableAction(MicrophonePermissionState.PERMANENTLY_DENIED),
        )
    }

    @Test
    fun unavailableMicrophoneCannotBeEnabled() {
        assertEquals(
            MicrophoneEnableAction.UNAVAILABLE,
            microphoneEnableAction(MicrophonePermissionState.UNAVAILABLE),
        )
    }

    @Test
    fun defaultPreferenceIsOptInOff() {
        assertFalse(SoundAnalysisPreferences().enabled)
        assertTrue(SoundAnalysisPreferences(enabled = true).enabled)
    }

    @Test
    fun permissionCopyStatesNoRawAudioDefaultAndCoreFlowContinues() {
        assertTrue(MicrophonePermissionCopy.PRE_PERMISSION.contains("音声そのものは標準では保存しません"))
        assertTrue(MicrophonePermissionCopy.PRE_PERMISSION.contains("睡眠記録は使えます"))
        assertTrue(MicrophonePermissionCopy.DENIED.contains("中心機能はそのまま使えます"))
    }
}
