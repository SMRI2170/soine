# Companion Dialogue Selection

The shared selector chooses one authored line from product context without generating medical or diagnostic claims.

Priority for morning dialogue is:

1. newly discovered dream
2. notable night event
3. supported routine observation
4. relationship familiarity
5. generic fallback

Bedtime uses routine, relationship, then generic lines.

Each line carries a cooldown measured in selections. Recently shown IDs are filtered when another eligible candidate exists. Generic dialogue always remains available so missing or weak context never forces an unsupported claim.

Routine timing claims require high confidence. Ambient-sound suggestions require a supported frequent sound and a known display label. Routine phase mismatches are ignored.

Deterministic test mode accepts a stable seed and chooses reproducibly among equal-priority candidates. Production callers may use the default mode.

Dialogue text is authored content only. Sleep duration, routine observations, events, dreams, or relationship state choose among lines but are never converted into diagnosis, treatment advice, sleep-quality judgements, or negative feedback.
