# ReserveLedger-Spring reference (private)

Trusted reference for track t1-spring. Not part of the candidate kit; never give it to a model.

```bash
python3 bench.py grade --track t1-spring --workspace tracks/t1-spring/reference --out <empty dir> \
  --model-label REFERENCE --run-id ref --allow-exec
```

It is also the calibration baseline for D09 and the host of the ten controlled faults (`BENCH_MUTANT=M01..M10`),
which run with Flyway `target=1` so that the V2 database constraints do not mask their business effect.
