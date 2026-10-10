# Documentation Map

Start here when implementing Soine.

## Planning
- [issues.md](issues.md) — priorities, Epics, dependency order and AI-agent workflow
- ../AGENTS.md — repository-wide implementation rules
- roadmap.md — product milestones
- GitHub Issues — issues / executable work items

## Product
- product.md — product brief
- mvp-spec.md — MVP contract
- experience.md — signature experience
- companion-spec.md — character/animation contract
- content-guide.md — character voice

## Systems
- architecture.md — technical architecture
- data-model.md — persisted data
- night-event-system.md — overnight memories
- relationship-system.md — familiarity/progression
- dream-system.md — collectible dreams
- 3d-plan.md — renderer PoC
- privacy.md — permissions/data handling
- failure-matrix.md — failure containment matrix and severity classification ([#185][issue-185])
- security-audit-2026-10.md — V1 least-privilege / privacy audit snapshot ([#186][issue-186])
- performance-budget.md — binary / runtime / memory / battery budget targets ([#184][issue-184])
- asset-manifest.json — reference asset inventory for size-diff tracking ([#184][issue-184])
- overnight-battery-benchmark.md — overnight battery measurement template
- dependency-policy.md — add / update / remove policy and Renovate grouping ([#187][issue-187])
- release-candidate-scenarios.md — 7-night RC scenarios, release blockers, acceptance criteria ([#182][issue-182])
- observability.md — privacy contract for crash / non-fatal diagnostics ([#188][issue-188])
- analytics-vendor-selection.md — provider constraints and V1 selection framework ([#180][issue-180])
- ios-ambient-audio-verification.md — iOS ambient audio physical-device scenarios ([#23][issue-23])
- ios-target-matrix.md — iOS architecture target list (current vs parent commit)
- store-listing-facts.md — bundle IDs, permissions, capabilities, privacy copy inputs ([#181][issue-181])
- quality-epic-status.md — closure summary for the quality hardening EPIC ([#183][issue-183])
- ci-quality-policy.md — CI lint / static-analysis / UI-smoke gate policy ([#193][issue-193])
- microphone-permission-ux.md — opt-in microphone permission flow
- health-data-contract.md — normalized Health sleep-data boundary
- health-merge-policy.md — manual session vs external Health precedence
- android-health-connect.md — Android Health Connect sleep adapter
- ios-healthkit.md — iOS HealthKit sleep adapter
- accessibility.md — core-flow accessibility and reduce-motion baseline
- accessibility-audit-2026-10.md — V1 release accessibility audit + device follow-up ([#195][issue-195])
- sound-event-model.md — derived overnight sound-event contract
- android-sound-detection.md — Android microphone foreground-service spike and field-test plan
- analytics-plan.md — validation metrics
- testing-strategy.md — release quality
- timezone.md — absolute time, LocalTimeZone / DisplayFormatter boundary, wall-clock safety ([#196][issue-196])
- onboarding.md — permission-free first-run flow, replay, analytics funnel ([#194][issue-194])
- design-system.md — V1 color / type / spacing / radius tokens and components ([#168][issue-168])
- bedtime-redesign.md — V1 bedtime screen as the signature moment, hero companion + relationship-stage variation ([#169][issue-169])
- sleeping-redesign.md — V1 sleeping screen as a quiet bedside scene, auto-hide + tap-to-reveal + anchored wake ([#170][issue-170])
- morning-redesign.md — V1 morning screen as the "昨夜を発見する" reveal, Emotion → Memory → Data order with dream discovery special reveal ([#171][issue-171])
- dream-album-redesign.md — V1 Dream Album as a visual collection, per-dream gradient + motif, immersive sheet detail, no loot-box pressure ([#172][issue-172])
- relationship-presentation.md — V1 relationship-stage visual presentation, single source of truth for bedtime / sleeping / morning visuals, no XP / level / number ([#175][issue-175])
- settings-ia.md — V1 Settings & Privacy information architecture, five named groups (Sleep / Companion / Optional / Privacy / About), destructive action behind navigation ([#176][issue-176])
- motion-language.md — V1 motion language, single source of truth for duration / easing / reduce-motion, SoineAnimatedVisibility wrapper, button press feedback, haptics at meaningful moments ([#177][issue-177])
- sound-event-ux.md — V1 overnight sound-event UX, SoundEventSummary aggregator with "*のような*" copy pattern, no confidence %, no medical claim, privacy link to settings ([#178][issue-178])

## Decisions
- decisions/0001-kmp-app-shell.md
- decisions/0002-local-first.md

[issue-184]: https://github.com/SMRI2170/soine/issues/184
[issue-185]: https://github.com/SMRI2170/soine/issues/185
[issue-186]: https://github.com/SMRI2170/soine/issues/186
[issue-187]: https://github.com/SMRI2170/soine/issues/187
[issue-182]: https://github.com/SMRI2170/soine/issues/182
[issue-181]: https://github.com/SMRI2170/soine/issues/181
[issue-183]: https://github.com/SMRI2170/soine/issues/183
[issue-168]: https://github.com/SMRI2170/soine/issues/168
[issue-169]: https://github.com/SMRI2170/soine/issues/169
[issue-170]: https://github.com/SMRI2170/soine/issues/170
[issue-171]: https://github.com/SMRI2170/soine/issues/171
[issue-172]: https://github.com/SMRI2170/soine/issues/172
[issue-175]: https://github.com/SMRI2170/soine/issues/175
[issue-176]: https://github.com/SMRI2170/soine/issues/176
[issue-177]: https://github.com/SMRI2170/soine/issues/177
[issue-178]: https://github.com/SMRI2170/soine/issues/178
[issue-193]: https://github.com/SMRI2170/soine/issues/193
[issue-194]: https://github.com/SMRI2170/soine/issues/194
[issue-195]: https://github.com/SMRI2170/soine/issues/195
[issue-196]: https://github.com/SMRI2170/soine/issues/196
[issue-188]: https://github.com/SMRI2170/soine/issues/188
[issue-180]: https://github.com/SMRI2170/soine/issues/180
[issue-23]: https://github.com/SMRI2170/soine/issues/23
