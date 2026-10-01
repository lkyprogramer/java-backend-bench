package app.service;

/** Judge-controlled single-fault variants of the reference (BENCH_MUTANT). Never set in normal runs. */
public enum Mutant {
    M01, M02, M03, M04, M05, M06, M07, M08, M09, M10;

    private static final String ACTIVE = System.getenv("BENCH_MUTANT");

    public boolean on() {
        return name().equals(ACTIVE);
    }
}
