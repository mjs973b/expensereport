package name.mjs001.expensereport;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * User DAO. Supports adding, editing, and deleting users.
 */
public class UserDao {
    /** Database instance which will be queried. */
    private SQLiteDatabase database;

    /** Instance of the database helper class. */
    private ExpenseData dbHelper;

    // columns
    private String[] colsToReturn = {
            ExpenseData.USER_ID,
            ExpenseData.USER_NAME
    };

    // constructor creates an instance of the helper class
    public UserDao(Context context) {
        dbHelper = new ExpenseData(context);
    }

    // methods to open and close DB
    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    // methods to open and close DB
    public void openReadonly() throws SQLException {
        database = dbHelper.getReadableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /**
     * Determines if specified user exists in the database.
     * @param user The user whose existence will be checked.
     * @return True if user exists in DB, false otherwise.
     */
    public boolean exists(String user) {
        Cursor res = database.query(ExpenseData.USERS_TABLE, colsToReturn, ExpenseData.USER_NAME +
                " = '" + user + "'", null, null, null, null);
        int cnt = res.getCount();
        res.close();
        return cnt > 0;
    }

    /**
     * Inserts a new user into the database.
     * @param name The name of the user to insert.
     * @return The inserted user.
     */
    public User newUser(String name) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseData.USER_NAME, name);

        // watch for unique constraint exception
        try {
            // returns column position, or -1 if fail
            int insertId = (int)database.insert(ExpenseData.USERS_TABLE, null, cv);

//            // query db to get id and return added user
//            Cursor cursor = database.query(ExpenseData.USERS_TABLE, colsToReturn, ExpenseData.USER_ID +
//                    " = " + insertId, null, null, null, null);

            if (insertId > 0) {
                return new User(new UserId(insertId), name);
            } else {
                return null; // insertion failed
            }
        } catch (SQLiteConstraintException ce) {
            // unique constraint violated
            return null;
        }
    }

    /**
     * Updates an existing user's name in the database.
     * @param name The user object with the new name.
     * @return The updated user.
     */
    public User editUser(User name) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseData.USER_NAME, name.getName());
        database.update(ExpenseData.USERS_TABLE, cv, ExpenseData.USER_ID + " = '" + name.getId() + "'", null);
        return name;
    }

    /**
     * Deletes an existing user from the database. Do not allow the last user to be deleted.
     *
     * @param user The user to be deleted.
     * @return true on success.
     */
    public boolean deleteUser(User user) {
        List<User> users = getAllUsers();
        if (users.size() == 1) {
            return false;
        }

        // delete this user's expenses
        database.delete(ExpenseData.EXPENSES_TABLE, ExpenseData.USER_ID + " = '" + user.getId() + "'", null);
        // delete user's categories
        database.delete(ExpenseData.CATEGORIES_TABLE, ExpenseData.USER_ID + " = '" + user.getId() + "'", null);
        // delete user
        database.delete(ExpenseData.USERS_TABLE, ExpenseData.USER_ID + " = '" + user.getId() + "'", null);

        return true;
    }

    /**
     * Retrieves all users from the database.
     * @return The list of retrieved users.
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();

        String sql = "select user_id,name from users order by name asc";
        Cursor res = database.rawQuery(sql, null);

        res.moveToFirst();
        while (!res.isAfterLast()) {
            int id = res.getInt(0);
            String name = res.getString(1);
            users.add(new User(new UserId(id), name));
            res.moveToNext();
        }

        res.close();
        return users;
    }
}
