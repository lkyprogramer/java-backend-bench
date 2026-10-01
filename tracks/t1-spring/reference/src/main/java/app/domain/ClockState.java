package app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clock_state")
public class ClockState {

    public static final int SINGLETON = 1;

    @Id
    private int id = SINGLETON;

    @Column(name = "now_ms")
    private long nowMs;

    protected ClockState() {
    }

    public ClockState(long nowMs) {
        this.nowMs = nowMs;
    }

    public long getNowMs() {
        return nowMs;
    }

    public void setNowMs(long nowMs) {
        this.nowMs = nowMs;
    }
}
