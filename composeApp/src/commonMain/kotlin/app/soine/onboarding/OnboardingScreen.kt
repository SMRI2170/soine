package app.soine.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.soine.accessibility.AccessibilityPolicy

/**
 * 3-step onboarding flow.
 *
 * The screen is permission-free by design: no Health probe, no
 * microphone request, no notification request. Optional features
 * (Health, microphone, ambient sound) are explained in step 2
 * ("optional features") and the user enables them later in
 * Settings.
 *
 * The screen is short on purpose: 3 pages, each scannable in
 * under 10 seconds. The "はじめる" CTA is the final action; a
 * "スキップ" CTA in the top-right is reachable on every step so
 * the user never feels trapped.
 *
 * The screen honors [AccessibilityPolicy.MIN_TOUCH_TARGET_DP] for
 * every interactive control and the [AccessibilityPolicy] CTA
 * content-description constants. See
 * [AccessibilitySourceAuditTest] for the textual pin.
 */
@Composable
fun OnboardingScreen(
    controller: OnboardingController,
    onCompleted: () -> Unit,
    onSkipped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(controller) {
        // Fire ONBOARDING_STARTED the first time the screen is
        // composed. The controller dedupes nothing — the LaunchedEffect
        // is keyed on the controller instance, so navigating away and
        // back will refire onShown() and the analytics funnel can
        // observe the replay path through ONBOARDING_REPLAYED.
        controller.onShown()
    }

    val currentStep = controller.currentStep
    val lastResult = controller.lastResult

    LaunchedEffect(lastResult) {
        when (lastResult) {
            OnboardingController.Result.COMPLETED -> onCompleted()
            OnboardingController.Result.SKIPPED -> onSkipped()
            OnboardingController.Result.PENDING -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepIndicator(currentStep = currentStep, totalSteps = OnboardingController.TOTAL_STEPS)
            TextButton(
                onClick = { controller.skip() },
                modifier = Modifier
                    .heightIn(min = AccessibilityPolicy.MIN_TOUCH_TARGET_DP.dp)
                    .semantics { contentDescription = AccessibilityPolicy.ONBOARDING_SKIP_CONTENT_DESCRIPTION },
            ) { Text("スキップ") }
        }

        Spacer(Modifier.height(8.dp))

        when (currentStep) {
            0 -> OnboardingStep(
                headline = "夜の相棒、Soine",
                body = listOf(
                    "Soineは、眠る前も、眠っている間も、朝まで、そばにいるアプリです。",
                    "特別な準備はいりません。ふとんの横に置いて、ボタンをひとつ押すだけ。",
                ),
            )
            1 -> OnboardingStep(
                headline = "「一緒に眠る」を体験",
                body = listOf(
                    "「一緒に寝る」を押すと、コンパニオンが静かに眠り始めます。",
                    "音や計測を設定しなくても、そのまま始められます。",
                    "翌朝、「起きる」を押すと夜のふりかえりが届きます。",
                ),
            )
            else -> OnboardingStep(
                headline = "端末の中で完結。",
                body = listOf(
                    "記録はあなたの端末に残ります。アカウントの作成は不要です。",
                    "Health Connect / HealthKit や音声の解析はあとから設定できます。今は「一緒に寝る」を試してみてください。",
                ),
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { controller.retreat() },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .semantics { contentDescription = AccessibilityPolicy.ONBOARDING_BACK_CONTENT_DESCRIPTION },
                    shape = MaterialTheme.shapes.large,
                ) { Text("もどる") }
            }
            Button(
                onClick = {
                    if (currentStep == OnboardingController.LAST_STEP) {
                        controller.complete()
                    } else {
                        controller.advance()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .semantics { contentDescription = AccessibilityPolicy.ONBOARDING_NEXT_CONTENT_DESCRIPTION },
                shape = MaterialTheme.shapes.large,
            ) {
                Text(if (currentStep == OnboardingController.LAST_STEP) "はじめる" else "次へ")
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val isActive = index <= currentStep
            Text(
                text = if (isActive) "●" else "○",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics {
                    contentDescription = "ステップ ${index + 1} / $totalSteps"
                },
            )
        }
    }
}

@Composable
private fun OnboardingStep(headline: String, body: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        body.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
