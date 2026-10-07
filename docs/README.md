# Documentation Map

Start here when implementing Soine.

## Planning
- [issues.md](issues.md) — priorities, Epics, dependency order and AI-agent workflow
- ../AGENTS.md — repository-wide implementation rules
- roadmap.md — product milestones
- GitHub Issues — executable work items

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
- microphone-permission-ux.md — opt-in microphone permission flow
- health-data-contract.md — normalized Health sleep-data boundary
- health-merge-policy.md — manual session vs external Health precedence
- android-health-connect.md — Android Health Connect sleep adapter
- ios-healthkit.md — iOS HealthKit sleep adapter
- accessibility.md — core-flow accessibility and reduce-motion baseline
- sound-event-model.md — derived overnight sound-event contract
- android-sound-detection.md — Android microphone foreground-service spike and field-test plan
- analytics-plan.md — validation metrics
- testing-strategy.md — release quality

## Decisions
- decisions/0001-kmp-app-shell.md
- decisions/0002-local-first.md

[issue-185]: https://github.com/SMRI2170/soine/issues/185
