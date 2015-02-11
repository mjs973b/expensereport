package com.simgeoapps.expensereport;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Category DAO. Supports adding, editing categories. Support for deleting categories pending.
 * Created by Simeon on 10/11/2014.
 */
public class CategoryDao {
    // Database fields
    private SQLiteDatabase database;
    private ExpenseData dbHelper;
    private String[] colsToReturn = { ExpenseData.CATEGORY_ID, ExpenseData.USER_ID,
            ExpenseData.CATEGORY_NAME };

    public CategoryDao(Context context) {
        dbHelper = new ExpenseData(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public boolean exists(String category, User us) {
        Cursor res = database.query(ExpenseData.CATEGORIES_TABLE, colsToReturn, ExpenseData.CATEGORY_NAME +
                " = '" + category + "' AND " + ExpenseData.USER_ID + " = '" + us.getId() + "'", null, null, null, null);
        int cnt = res.getCount();
        res.close();
        return cnt > 0;
    }

    public Category newCategory(String cat, User us) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseData.CATEGORY_NAME, cat);
        cv.put(ExpenseData.USER_ID, us.getId());
        long insertId = database.insert(ExpenseData.CATEGORIES_TABLE, null, cv);

        // query db and get inserted category
        Cursor cur = database.query(ExpenseData.CATEGORIES_TABLE, colsToReturn,
                ExpenseData.CATEGORY_ID + " = " + insertId, null, null, null, null);

        cur.moveToFirst();
        Category ans = new Category();
        ans.setId(cur.getInt(0));
        ans.setUserId(cur.getInt(1));
        ans.setCategory(cur.getString(2));
        cur.close();

        return ans;
    }

    public Category editCategory(Category cat, User us) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseData.CATEGORY_NAME, cat.getCategory());
        database.update(ExpenseData.CATEGORIES_TABLE, cv, ExpenseData.CATEGORY_ID + " = '" +
                cat.getId() + "' AND " + ExpenseData.USER_ID + " = '" + us.getId() + "'", null);
        return cat;
    }

    public Category deleteCategory(Category cat, User us) {
        // will delete category only. expenses will remain but cannot be accessed
        database.delete(ExpenseData.CATEGORIES_TABLE, ExpenseData.USER_ID + " = '" + us.getId()
                + "' AND " + ExpenseData.CATEGORY_ID + " = '" + cat.getId() + "'", null);
        return cat;
    }

    public List<Category> getCategories(User us) {
        // must return all categories for a certain user
        List<Category> ans = new ArrayList<>();

        // query db and get all categories for user us
        Cursor res = database.query(ExpenseData.CATEGORIES_TABLE, colsToReturn,
                ExpenseData.USER_ID + " = '" + us.getId() + "'", null, null, null, null);

        res.moveToFirst();
        while (!res.isAfterLast()) {
            Category cat = new Category();
            cat.setId(res.getInt(0));
            cat.setUserId(res.getInt(1));
            cat.setCategory(res.getString(2));
            ans.add(cat);
            res.moveToNext();
        }

        res.close();
        return ans;
    }

}
