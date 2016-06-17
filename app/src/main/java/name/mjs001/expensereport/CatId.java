package name.mjs001.expensereport;

/**
 * Database category id. immutable
 */
public class CatId extends ImmutableInt {
    public CatId() {
        super(0);
    }
    public CatId(int n) {
        super(n);
    }
    public boolean isUserCreated() {
        int v = toInt();
        return v > 0 && v < 999;
    }
}
