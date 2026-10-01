# Reference testing

- `src/test/java/contract/ReserveLedgerContractTest.java`: 11 black-box JUnit tests (JDK HttpClient + Jackson)
  covering oversell, idempotent retry and conflict, expiry boundary, single release, confirm accounting,
  terminal transitions, multi-SKU atomicity, zero quantity and state filtering. They kill all ten controlled
  faults (D10 = 10) in the judge's mutation run.
- The 80 formal cases, performance calibration and mutant validation are run by `tools/validate.py --track t1-spring`.
