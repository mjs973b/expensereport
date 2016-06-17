package com.simgeoapps.expensereport;

import java.io.Serializable;

/**
 * Category model. id is immutable.
 */
public class Category implements Serializable {
    public static final int SHOW_ALL_CAT_ID = 999;
    public static final Category SHOW_ALL = new Category(new CatId(SHOW_ALL_CAT_ID), "All Categories");

    // fields corresponding to the category table columns
    private CatId id;
    private String name;

//    public Category() {
//        this.id = new CatId(0);     // invalid
//        this.name = "_invalid";
//    }

    public Category(CatId id, String name) {
        this.id = id;
        this.name = name.replace('"', '_');
    }

    public String getName() {
        return name;
    }

    /** silently replace double-quote with underscore */
    public void setName(String name) {
        this.name = name.replace('"', '_');
    }

    public CatId getId() {
        return id;
    }

//    public void setId(CatId id) {
//        this.id = id;
//    }

    @Override
    public String toString() {
        return name;
    }

}
