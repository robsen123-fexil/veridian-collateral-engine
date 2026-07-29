# Veridian Collateral Engine

**Author:** misgana tsegaye

JVM library for collateral vault operations: pledge envelopes, haircut grids, margin aggregation, FIX 4.4 collateral messages, SWIFT MT940 scanning, and tri-party reconciliation pipelines.

## Build

```bash
./gradlew build
```

## CLI

```bash
./gradlew runCtl --args="allocate 1000"
./gradlew runCtl --args="haircut 1000000"
./gradlew runCtl --args="pipeline"
```

## Test

```bash
./gradlew test
```

## Fuzz (local)

```bash
bash .clusterfuzzlite/build.sh
./out/BatchFuzzer -runs=1000 corpus/batch/
./out/RouterFuzzer -runs=1000 corpus/router/
./out/SessionFuzzer -runs=1000 corpus/session/
./out/FixFuzzer -runs=1000 corpus/fix/
./out/SwiftFuzzer -runs=1000 corpus/swift/
```

ClusterFuzzLite uses Jazzer with `-Djazzer.max_len=8192` and a single fat jar for fast startup.

## Modules

| Module | Package | Responsibility |
|--------|---------|----------------|
| `BinaryBatchCodec` | wire | VCB1 binary batch decode with deferred digest slots |
| `EnvelopeWireCodec` | wire | VCE1 nested envelope parse |
| `Fix44Parser` | wire | FIX 4.4 collateral tag parse |
| `Fix44ExtendedCodec` | wire | FIX collateral message builders |
| `SwiftMt940Scanner` | wire | SWIFT MT940 :61: transaction scan |
| `Iso8583FieldMap` | desk | ISO8583 field registry |
| `WireDispatcher` | ingress | Magic dispatch to ingress sweep |
| `IngressSweep` | ingress | Envelope channel queue + seal |
| `CollateralPipeline` | engine | `runCollateralPipeline` orchestrator |
| `BatchNormalizer` | engine | Batch record normalization |
| `BatchDigest` | engine | Batch digest flush |
| `ChannelTape` | engine | Deferred channel seal |
| `SessionMerger` | engine | Session leg merge + digest |
| `PipelineStageRegistry` | engine | Multi-stage audit pipeline |
| `ProRataAllocator` | desk | Pro-rata share allocation |
| `ConstraintAllocator` | desk | Min/max constrained allocation |
| `HaircutGrid` | desk | Asset-class haircut grid |
| `MarginAggregator` | desk | SPAN-style margin rollup |
| `SpanMarginCalculator` | desk | Scanning risk scenarios |
| `ComplianceRuleEngine` | desk | Collateral compliance rules |
| `PledgeEnvelopeLedger` | desk | Pledge envelope lifecycle |
| `CollateralReconciler` | desk | Tri-party leg matching |
| `BasisAdjustmentLedger` | desk | FX basis accrual |
| `NostroSweepLedger` | desk | Nostro sweep planning |
| `SwapConfirmMatcher` | desk | Swap confirm two-leg match |
| `LiquidityRouteTable` | desk | Liquidity tier routing |
| `CreditLimitGuard` | desk | Counterparty limit guard |
| `FixTagDictionary` | desk | FIX tag metadata |
| `PositionLedger` | ledger | Custody position journal |
| `AuditSpool` | ledger | Module audit spool |
| `VaultCheckpointStore` | ledger | Checkpoint digest store |
| `CollateralCoverageAnalyzer` | desk | Coverage ratio and deficit analysis |
| `CollateralAllocationSolver` | desk | Priority-based collateral allocation |
| `AssetEligibilityEngine` | desk | Asset eligibility screening |
| `RegulatoryCapitalCalculator` | desk | RWA and capital charge calculator |
| `CollateralWorkflowRegistry` | desk | Pledge/release/substitution workflows |
| `VeridianCtl` | tools | CLI entry point |

See module table above for the full collateral vault surface area (100+ source modules).

## Wire formats

See [docs/FORMAT.md](docs/FORMAT.md).

## Fuzz harnesses

| Harness | Entry API |
|---------|-----------|
| `BatchFuzzer` | `CollateralPipeline.runCollateralPipeline` |
| `RouterFuzzer` | `WireDispatcher.dispatchIngressBytes` |
| `SessionFuzzer` | `CollateralPipeline.mergeCollateralSessions` |
| `FixFuzzer` | `Fix44Parser.parseWithSession` |
| `SwiftFuzzer` | `SwiftMt940Scanner.scanStatement` |
