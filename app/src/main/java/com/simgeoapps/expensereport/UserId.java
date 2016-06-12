package com.simgeoapps.expensereport;

/**
 * Database user id. immutable
 */
public class UserId extends ImmutableInt {
    public UserId() {
        super(0);
    }
    public UserId(int n) {
        super(n);
    }
}
