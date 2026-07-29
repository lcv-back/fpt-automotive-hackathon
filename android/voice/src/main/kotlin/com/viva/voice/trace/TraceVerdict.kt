package com.viva.voice.trace

/**
 * Serialization of the `<verdict>` field of the VIVA_TRACE_SUMMARY line.
 *
 * WHY THIS FILE EXISTS: backend/CLAUDE.md ("Cau hoi con treo") records an
 * open question from Vi - 03-contracts.md §4 defines `sealed class Verdict`
 * with Allow/Deny/Confirm each carrying extra fields (rule, reasonVi,
 * suggestion), but §1 logs a single opaque string, so nobody knew whether to
 * expect "Allow" or "Deny:G1_SPEED_LOCK". Vi cannot classify pass/fail
 * without an answer. Long owns §1, so the answer is here.
 *
 * Grammar (frozen 29/07, mirrored into 03-contracts.md §1):
 *
 *     verdict := "Allow"
 *              | "Deny:"    <RULE_ID>
 *              | "Confirm:" <RULE_ID>
 *              | "Error:"   <STAGE_ID>
 *
 * Parse by splitting on the FIRST ':' - kind on the left, detail on the right.
 *
 * Two deliberate choices:
 *
 *  1. The rule id rides along (`Deny:G1_SPEED_LOCK`, not bare `Deny`).
 *     N4b (ablation A1, Tung, 06/08) has to show a before/after table where
 *     SafetyGuard is switched off and "mo cua" at Speed=60 executes anyway.
 *     With the rule id in the log, that table is a group-by over the CSV Vi
 *     already produces; without it, somebody re-runs the demo by hand and
 *     reads logcat. Same cost to emit, 6d + 7d of barem ③ downstream.
 *
 *  2. `Error:<STAGE_ID>` is new - it is not in the sealed class, because a
 *     crashed turn never reaches SafetyGuard at all. Before this, a turn that
 *     died at ASR simply produced no summary line and vanished from the
 *     benchmark. Barem ② has a 4d box "Xu ly loi va kha nang quan sat" that
 *     the plan flags as weak; a named failure stage is exactly what it asks
 *     for, and it reuses the existing grammar so Vi's parser needs no change.
 *
 * reasonVi / questionVi / suggestion stay OUT of the log line: they are
 * free-form Vietnamese sentences read aloud to the driver, they would need
 * escaping, and they are not machine-readable. The rule id is the join key.
 */
sealed class TraceVerdict(val wire: String) {

    object Allow : TraceVerdict("Allow")

    /** [rule] is a SafetyGuard rule id from 03-contracts.md §4, e.g. "G1_SPEED_LOCK". */
    class Deny(rule: String) : TraceVerdict("Deny:" + ruleId(rule))

    /** [rule] is a SafetyGuard rule id from 03-contracts.md §4, e.g. "G2_CONFIRM_DOOR". */
    class Confirm(rule: String) : TraceVerdict("Confirm:" + ruleId(rule))

    /** The turn died before a verdict existed. [stage] is where it died. */
    class Error(stage: Stage) : TraceVerdict("Error:" + stage.id)

    override fun toString(): String = wire
}

/**
 * Rule ids are UPPER_SNAKE by convention (§4). Anything outside [A-Z0-9_]
 * would break the `kind:detail` split or the pipe-delimited line, so it is
 * folded to '_' rather than trusted - a rule id is typed by hand in
 * SafetyGuard and a stray space there must not cost a whole summary line.
 *
 * File-private and top-level rather than a companion member: the subclasses
 * call it from their super-constructor arguments, and a plain top-level
 * function is unambiguously in scope there.
 */
private fun ruleId(raw: String): String {
    val cleaned = raw.trim().uppercase().map { c ->
        if (c in 'A'..'Z' || c in '0'..'9' || c == '_') c else '_'
    }.joinToString("")
    return cleaned.ifEmpty { "UNSPECIFIED" }
}
