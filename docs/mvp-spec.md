# MVP specification

## Core promise

Open Soine at bedtime, sleep beside the companion, and receive a gentle morning record without setup friction.

## Bedtime screen

Shows:
- companion
- ambient sound selection
- timer
- primary "一緒に寝る" action

Start behavior:
1. check whether an active session already exists
2. create a new session only when none exists
3. persist the session as PREPARING
4. transition and persist it as SLEEPING
5. change companion to SETTLING
6. start selected ambient sound
7. transition companion to SLEEPING
8. allow screen to dim/lock

If the second persistence step fails, the PREPARING record remains recoverable. Repeated start actions return the existing active session instead of creating a duplicate.

Do not require microphone, Health, login, or cloud permissions to start sleeping.

## During sleep

The app prioritizes battery and session reliability over animation.

When visible, render breathing and occasional lightweight motions. When the screen is off, suspend 3D work. Ambient audio and session timing are independent.

## Wake flow

Primary action: "起きる".

1. persist end timestamp
2. stop/fade ambient sound
3. set companion to WAKING
4. calculate summary
5. increment total slept-together time exactly once
6. navigate to morning summary

## Morning summary

MVP:
- session duration
- bedtime
- wake time
- companion message
- accumulated slept-together time

Later:
- estimated actual sleep
- sound events
- health/wearable data
- trends

## Progression

Progress is based on completed valid sessions. Initial milestone candidates: 100h, 300h, 500h.

Rewards should be cosmetic/behavioral: sleeping poses, reactions, dialogue, small scene changes. Do not reward intentionally excessive sleep duration.

## Privacy defaults

- no account required
- local-first storage
- microphone opt-in
- raw audio not retained by default
- explain each permission at the moment it becomes useful
- Health permissions are optional

## Non-goals for MVP

- medical diagnosis
- sleep-stage claims from phone-only signals
- social feed
- multiplayer
- cloud synchronization
- complex physics
- large character roster
- subscriptions before the core sleep loop is validated
