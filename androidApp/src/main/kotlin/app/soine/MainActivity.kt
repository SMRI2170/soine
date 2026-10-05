package app.soine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.soine.sleep.StoredSleepSessionRepository
import app.soine.storage.AndroidSleepSessionStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = StoredSleepSessionRepository(AndroidSleepSessionStore(applicationContext))
        setContent { SoineApp(repository) }
    }
}
