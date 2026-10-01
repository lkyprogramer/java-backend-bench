package app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Item {

    @Id
    private String sku;
    private long total;
    private long available;
    private long held;
    private long sold;

    protected Item() {
    }

    public Item(String sku, long total) {
        this.sku = sku;
        this.total = total;
        this.available = total;
    }

    public String getSku() {
        return sku;
    }

    public long getTotal() {
        return total;
    }

    public long getAvailable() {
        return available;
    }

    public long getHeld() {
        return held;
    }

    public long getSold() {
        return sold;
    }

    public void hold(long quantity) {
        available -= quantity;
        held += quantity;
    }

    public void release(long quantity) {
        held -= quantity;
        available += quantity;
    }

    public void sell(long quantity) {
        held -= quantity;
        sold += quantity;
    }

    /** Only used by controlled mutant M06. */
    public void corruptReturn(long quantity) {
        available += quantity;
    }
}
