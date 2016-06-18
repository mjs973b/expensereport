package name.mjs001.expensereport;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Category DAO. Supports adding, editing, and deleting categories.
 */
public class CategoryDao {
    /** Instance of database which will be queried. */
    private SQLiteDatabase database;

    /** Instance of the database helper class. */
    private ExpenseData dbHelper;

    // columns
    private String[] colsToReturn = {
            ExpenseData.CATEGORY_ID,
            ExpenseData.USER_ID,
            ExpenseData.CATEGORY_NAME
    };

    public CategoryDao(Context context) {
        dbHelper = new ExpenseData(context);
    }

    // open and close DB.
    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    // open and close DB.
    public void openReadonly() throws SQLException {
        database = dbHelper.getReadableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /**
     * Determines if the specified category already exists in the database for the specified user.
     * @param category The category to be checked for existence.
     * @param us The user for which to verify for the existing of the category.
     * @return True if the category exists for the specified user, false otherwise.
     */
    public boolean exists(String category, User us) {
        Cursor res = database.query(ExpenseData.CATEGORIES_TABLE, colsToReturn, ExpenseData.CATEGORY_NAME +
                " = '" + category + "' AND " + ExpenseData.USER_ID + " = '" + us.getId() + "'", null, null, null, null);
        int cnt = res.getCount();
        res.close();
        return cnt == 1;
    }

    /**
     * Inserts a new category in the database for the specified user.
     * @param name The name of the category to insert.
     * @param us The user to which the category belongs.
     * @return The inserted category.
     */
    public Category newCategory(String name, User us) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseData.CATEGORY_NAME, name);
        cv.put(ExpenseData.USER_ID, us.getId().toInt());
        int rowId = (int)database.insert(ExpenseData.CATEGORIES_TABLE, null, cv);

        return new Category(new CatId(rowId), name);

//        // query db and get inserted category
//        Cursor cur = database.query(ExpenseData.CATEGORIES_TABLE, colsToReturn,
//                ExpenseData.CATEGORY_ID + " = " + insertId, null, null, null, null);
//
//        cur.moveToFirst();
//        Category ans = new Category();
//        ans.setId(cur.getInt(0));
//        ans.setName(cur.getString(2));
//        cur.close();

//        return ans;
    }

    /**
     * Updates the name of an existing category. cat_id are unique across all users.
     * @param cat The category object with the new name.
     * @return The updated category.
     */
    public Category editCategory(Category cat) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseData.CATEGORY_NAME, cat.getName());
        database.update(ExpenseData.CATEGORIES_TABLE, cv, ExpenseData.CATEGORY_ID + " = '" +
                cat.getId() + "'", null);
        return cat;
    }

    /**
     * Deletes an existing category that belongs to the specified user. Do not allow the last
     * category for a user to be deleted.
     *
     * @param user  the user
     * @param cat The category to be deleted.
     * @return    true on success.
     */
    public boolean deleteCategory(User user, Category cat) {
        List<Category> cats = getCategories(user.getId());
        if (cats.size() == 1) {
            return false;
        }
        // delete expenses for the category
        database.delete(ExpenseData.EXPENSES_TABLE, ExpenseData.CATEGORY_ID + " = '" + cat.getId() + "'", null);
        // delete category
        database.delete(ExpenseData.CATEGORIES_TABLE, ExpenseData.CATEGORY_ID + " = '" + cat.getId() + "'", null);
        return true;
    }

    public List<Category> getCategories(User user) {
        return getCategories( user.getId() );
    }

    /**
     * Retrieves all categories for the specified user.
     * @param userId The user whose categories to retrieve.
     * @return The list of retrieved categories.
     */
    public List<Category> getCategories(UserId userId ) {
        List<Category> list = new ArrayList<>();

        String[] args = new String[] {
            userId.toString()
        };

        String sql = "select cat_id,category from categories " +
                "where user_id = ? order by category asc";
        Cursor res = database.rawQuery(sql, args);

        res.moveToFirst();
        while (!res.isAfterLast()) {
            int id = res.getInt(0);
            String name = res.getString(1);
            Category cat = new Category( new CatId(id), name);
            list.add(cat);
            res.moveToNext();
        }

        res.close();
        return list;
    }
}
