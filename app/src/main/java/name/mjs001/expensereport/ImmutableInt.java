package name.mjs001.expensereport;

/**
 * immutable
 */
public class ImmutableInt implements Comparable<ImmutableInt> {
    private int n;

    public ImmutableInt(int n) {
        this.n = n;
    }
    public int toInt() {
        return n;
    }
    @Override
    public String toString() {
        return Integer.toString(n);
    }
    @Override
    public boolean equals(Object other) {
        if (other instanceof ImmutableInt) {
            return n == ((ImmutableInt)other).n;
        }
        return false;
    }
    @Override
    public int hashCode() {
        return n;
    }
    public int compareTo(ImmutableInt other) {
        return n - other.n;
    }
}
