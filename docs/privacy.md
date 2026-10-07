# Privacy and Permission Design

## Defaults

- no account
- no cloud requirement
- local-first
- no microphone permission during onboarding
- no Health permission during onboarding
- raw overnight audio is not retained by default

## Permission timing

Ask only when the user explicitly enables a feature.

Microphone:
"寝言や大きな音など、夜の音イベントを端末内で見つけるために使います。"

The app shows Soine's pre-permission explanation first. OS microphone permission is requested only after the user explicitly enables night sound analysis and accepts that explanation.

Health:
"端末やウェアラブルの睡眠記録を、Soineの朝の記録に追加できます。"

The app remains usable when either is denied.

## Data minimization

Prefer derived events over raw signals. Keep provenance so the UI can distinguish manual, Health, wearable and inferred data.

## Future cloud

If sync is added later, it must be opt-in with an explicit data inventory and deletion/export design before implementation.
