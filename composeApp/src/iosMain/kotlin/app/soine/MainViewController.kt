package app.soine

import androidx.compose.ui.window.ComposeUIViewController
import app.soine.sleep.StoredSleepSessionRepository
import app.soine.storage.IosSleepSessionStore

fun MainViewController() = ComposeUIViewController {
    SoineApp(StoredSleepSessionRepository(IosSleepSessionStore()))
}
