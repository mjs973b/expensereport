package com.simgeoapps.expensereport;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Database helper class.
 */
public class ExpenseData extends SQLiteOpenHelper {
    // fields
    /** Name of the database. */
    private static final String DATABASE_NAME = "Expenses.db";

    /** Database version. */
    private static final int DATABASE_VERSION = 2;

    // tables
    public static final String USERS_TABLE = "users";
    public static final String CATEGORIES_TABLE = "categories";
    public static final String EXPENSES_TABLE = "expenses";

    // USERS, CATEGORIES and EXPENSES table columns
    public static final String USER_ID = "user_id";
    public static final String USER_NAME = "name";

    public static final String CATEGORY_ID = "cat_id";
    public static final String CATEGORY_NAME = "category";

    public static final String EXPENSE_ID = "expense_id";
    public static final String COST_COLUMN = "cost"; // in cents
    public static final String DESCRIPTION_COLUMN = "description";
    public static final String DAY_COLUMN = "day";
    public static final String MONTH_COLUMN = "month"; // 0-based
    public static final String YEAR_COLUMN = "year";
    public static final String VDATE_COLUMN = "vdate";  /* sortable without any funny business */

    // sql statements
    private static final String CREATE_USER_TABLE = "CREATE TABLE " + USERS_TABLE + " (" +
            USER_ID + " integer primary key autoincrement, " +
            USER_NAME + " text not null unique );";

    private static final String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + CATEGORIES_TABLE + " (" +
            CATEGORY_ID + " integer primary key autoincrement, " +
            USER_ID + " integer not null, " +
            CATEGORY_NAME + " text not null );";

    private static final String CREATE_EXPENSES_TABLE = "CREATE TABLE " + EXPENSES_TABLE + " (" +
            EXPENSE_ID + " integer primary key autoincrement, " +
            USER_ID + " integer not null, " +
            CATEGORY_ID + " integer not null, " +
            COST_COLUMN + " integer not null, " +
            DESCRIPTION_COLUMN + " text, " +
            DAY_COLUMN + " text not null, " +
            MONTH_COLUMN + " text not null, " +
            YEAR_COLUMN + " text not null, " +
            VDATE_COLUMN + " text default '1970-01-01' ) ";

    // constructor
    public ExpenseData(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        // create tables
        database.execSQL(CREATE_USER_TABLE);
        database.execSQL(CREATE_CATEGORIES_TABLE);
        database.execSQL(CREATE_EXPENSES_TABLE);
    }

    /**
     * db is open for writing, and this call is wrapped in a transaction. If an exception is
     * thrown, all changes are undone and db stays are original version. Must not call
     * startTransaction or endTransaction.
     *
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(GlobalConfig.LOG_TAG,
                "Upgrading database from version " + oldVersion + " to " + newVersion );

        if (oldVersion == 1) {
            // create new column
            db.execSQL("alter table expenses add column vdate text default '1970-01-01' ");

            // re-write the new column
            Cursor cur = db.rawQuery("select expense_id,year,month,day from expenses ", null);
            String strDate;
            String[] args = new String[1];
            ContentValues cv = new ContentValues();
            cur.moveToFirst();
            while (!cur.isAfterLast()) {

                args[0] = cur.getString(0);
                strDate = cur.getString(1) + '-' +
                        digit2(cur.getInt(2) + 1) + '-' +
                        digit2(cur.getInt(3));
                cv.clear();
                cv.put("vdate", strDate);
                db.update("expenses", cv, "expense_id = ?", args);

                cur.moveToNext();
            }

            cur.close();
            Log.w(GlobalConfig.LOG_TAG, "Database update to v2 complete");
        }
    }

    private String digit2(int n) {
        String s = Integer.toString(n);
        if (n < 10) {
            return "0" + s;
        }
        return s;
    }

}
