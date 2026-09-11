# cloud-itonami-isco-7214

Open Occupation Blueprint for **ISCO-08 7214**: Structural Metal Preparers and Erectors.

This repository designs a forkable OSS business for a structural-steel-erection job-site coordination service: a job-site scheduling/logistics coordination robot manages work-record logging, crew/crane scheduling, safety-concern flagging and structural-steel-materials order coordination under a governor-gated actor, so the practice keeps its own operating records instead of renting a closed crew-dispatch SaaS.

**This actor coordinates JOB-SITE SCHEDULING/LOGISTICS ONLY — it never performs structural-steel erection work itself.** Structural metal preparers and erectors assemble steel-frame structures on active job sites, one of the highest fall-risk/crane-hazard trades (structural steel erection at height, crane-load handling); the actor's closed op-allowlist contains no op that directly finalizes a structural-steel-erection-execution decision, authorizes a crane lift, or overrides a site-safety officer's judgment. Any proposal that attempts any of these is a hard, permanent block, never overridable by human approval.

**Maturity: `:implemented`.** `src/steelcoord/` implements the
`SteelCoordActor` as a `langgraph.graph/state-graph`
(`steelcoord.actor`) wired to a `Steel Erection Coordination Advisor`
(`steelcoord.advisor`) and an independent `SteelCoordGovernor`
(`steelcoord.governor`), following the itonami actor pattern
(ADR-2607011000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok? true) +-> :request-approval (:escalate? true, human-in-the-loop
interrupt) +-> :hold (:hard? true)`. See `kbb -M:test` output for
the current test/assertion counts.

HARD invariants (always `:hold`, never overridable): the job site
must be independently verified/registered before any action; a
referenced worker must be a registered crew member belonging to that
site; `:effect` must be `:propose` only (no hardware dispatch, no
structural-steel erection work performed); the closed op-allowlist is
enforced (no op in the allowlist finalizes structural-steel-erection
execution, authorizes a crane lift, or overrides site-safety
authority); and any proposal that attempts to directly finalize a
structural-steel-erection-execution decision, authorize a crane lift,
or override a site-safety officer's judgment is a hard, **permanent**
block — detected as finalization/execution action phrases (never bare
nouns like "steel"/"crane"/"beam", which are ordinary vocabulary for
this domain and must not false-trip the guard).

Always-escalate ops (human sign-off regardless of confidence, mapping
this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (every surfaced fall-hazard/crane-condition/
load-hazard concern) and `:coordinate-supply-order` above the
registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a job-site scheduling/logistics coordination robot performs work-record logging, crew/crane-schedule proposals, safety-concern surfacing and structural-steel-materials order coordination under an actor that proposes
actions and an independent **Steel Erection Coordination Governor** that gates them. The governor never
dispatches hardware itself, never performs structural-steel erection work, never authorizes a crane lift, and never overrides a site-safety officer's judgment; `:high`/`:safety-critical` actions (such as a safety-concern flag or an above-threshold supply order) require human sign-off.

## Core Contract

```text
job-site roster + crew roster + job-site/crane schedule
        |
        v
Steel Erection Coordination Advisor -> SteelCoordGovernor -> log record/schedule/order, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
finalize a structural-steel-erection-execution decision, authorize a
crane lift, override a site-safety officer's judgment, suppress an
operating record, or disclose sensitive data without governor
approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7214`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
