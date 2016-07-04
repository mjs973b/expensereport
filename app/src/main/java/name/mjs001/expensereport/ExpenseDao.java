package name.mjs001.expensereport;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Expenses DAO. Supports adding, (editing), deleting expenses.
 */
public class ExpenseDao {
    /** Instance of database which will be queried. */
    private SQLiteDatabase database;

    /** Instance of the database helper class. */
    private ExpenseData dbHelper;

    // columns
    private String[] colsToReturn = {
            ExpenseData.EXPENSE_ID,
            ExpenseData.USER_ID,
            ExpenseData.CATEGORY_ID,
            ExpenseData.COST_COLUMN,
            ExpenseData.DESCRIPTION_COLUMN,
            ExpenseData.DAY_COLUMN,
            ExpenseData.MONTH_COLUMN,
            ExpenseData.YEAR_COLUMN,
            ExpenseData.VDATE_COLUMN
    };

    public ExpenseDao(Context context) {
        dbHelper = new ExpenseData(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void openReadonly() throws SQLException {
        database = dbHelper.getReadableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Expense lookupExp(long rowId) {
        String[] args = new String[] { Long.toString(rowId) };
        Cursor res = database.query( ExpenseData.EXPENSES_TABLE,
            colsToReturn,
            "expense_id = ?",
            args,
            null, null, null);

        List<Expense> ans = convToExpenseList(res);

        res.close();
        if (ans.size() != 1) {
            return null;
        }
        return ans.get(0);
    }

    /**
         * Insert a new expense record into the database.
         * @param exp  expense object, passed id is ignored.
         * @return  true on success, false on error
         */
    public boolean newExpense(Expense exp) {
        ContentValues cv = convertToCv(exp, true);

        long newRowId = database.insert(ExpenseData.EXPENSES_TABLE, null, cv);

        exp.setRowId( (int)newRowId );

        return newRowId > 0;
    }

    /**
     * Updates the cost and description of an existing expense.
     * @param ex The expense to be updated, having the ID of an existing expense, and the new cost
     *           and description.
     * @return  true on success, false on error
     */
    public boolean updateExpense(Expense ex) {
        ContentValues cv = convertToCv(ex, false);
        int rowCnt = database.update(ExpenseData.EXPENSES_TABLE, cv, ExpenseData.EXPENSE_ID + " = '" +
                ex.getRowId() + "'", null);

        return rowCnt == 1;
    }

    private ContentValues convertToCv(Expense exp, boolean bSetOldDate) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseData.USER_ID, exp.getUserId().toInt());
        cv.put(ExpenseData.CATEGORY_ID, exp.getCategoryId().toInt());
        cv.put(ExpenseData.COST_COLUMN, exp.getCostAsCents());
        cv.put(ExpenseData.DESCRIPTION_COLUMN, exp.getDescription());
        String date = exp.getDate();
        if (bSetOldDate) {
            // set these once, on row create
            int iyear = Integer.parseInt(date.substring(0, 4));
            int imon = Integer.parseInt(date.substring(5, 7)) - 1;  // zero-based
            int iday = Integer.parseInt(date.substring(8, 10));
            // db defines these columns as strings
            cv.put(ExpenseData.DAY_COLUMN, Integer.toString(iday));
            cv.put(ExpenseData.MONTH_COLUMN, Integer.toString(imon));
            cv.put(ExpenseData.YEAR_COLUMN, Integer.toString(iyear));
        }
        cv.put(ExpenseData.VDATE_COLUMN, date);
        return cv;
    }

    /**
     * Deletes an existing expense from the database.
     * @param exp The expense to be deleted.
     * @return true on success, false on error.
     */
    public boolean deleteExpense(Expense exp) {
        return deleteExpense(exp.getRowId());
    }

    public boolean deleteExpense(long rowId) {
        if (rowId < 1) return false;

        String[] args = new String[1];
        args[0] = Long.toString(rowId);

        //database.execSQL("delete from expenses where expense_id = ?", args);
        int n = database.delete("expenses", "expense_id = ?", args);
        return n == 1;
    }

    /**
     * Retrieves the sum total of all expenses for the given criteria. This method is used in the
     * ViewExpenses activity to show the total at the top.
     * @param us The user whose expenses should be included in the total.
     * @param cat The category whose expenses should be included in the total.
     * @return The dollar amount of the sum total of expenses that match the criteria.
     */
    public BigDecimal getTotalCost(User us, Category cat) {
        String[] cols = { ExpenseData.COST_COLUMN };
        Cursor res = database.query(ExpenseData.EXPENSES_TABLE,
                cols,
                ExpenseData.USER_ID + " = " + us.getId() + " AND " +
                        ExpenseData.CATEGORY_ID + " = " + cat.getId(),
                null, null, null, null);

        BigDecimal totCost = new BigDecimal(0);
        res.moveToFirst();
        while (!res.isAfterLast()) {
            totCost = totCost.add(new BigDecimal(res.getLong(0)));
            res.moveToNext();
        }
        res.close();

        // move decimal point
        return totCost.movePointLeft(2);
    }

//    /**
//     * Retrieves the sum total of all expenses for the given criteria. This method is used in the
//     * ViewExpenses activity to show the total at the top.
//     * @param us The user whose expenses should be included in the total.
//     * @param cat The category whose expenses should be included in the total.
//     * @param month The expenses for this month should be included in the total.
//     * @param year The expenses for this year should be included in the total.
//     * @return The dollar amount of the sum total of expenses that match the criteria.
//     */
//    public BigDecimal getTotalCost(User us, Category cat, int month, int year) {
//        String[] cols = { ExpenseData.COST_COLUMN };
//        Cursor res = database.query(ExpenseData.EXPENSES_TABLE, cols, ExpenseData.USER_ID + " = '" +
//                us.getId() + "' AND " + ExpenseData.CATEGORY_ID + " = '" + cat.getId() + "' AND " +
//                ExpenseData.MONTH_COLUMN + " = '" + Integer.toString(month) + "' AND " +
//                ExpenseData.YEAR_COLUMN + " = '" + Integer.toString(year) + "'", null, null, null, null);
//
//        BigDecimal totCost = new BigDecimal(0);
//        res.moveToFirst();
//        while (!res.isAfterLast()) {
//            totCost = totCost.add(new BigDecimal(res.getLong(0)));
//            res.moveToNext();
//        }
//        res.close();
//
//        // move decimal point
//        return totCost.movePointLeft(2);
//    }

//    /**
//     * Retrieves the sum total of all expenses for the given user and month/year.
//     * @param us The user whose expenses should be included in the total.
//     * @param month The expenses for this month should be included in the total.
//     * @param year The expenses for this year should be included in the total.
//     * @return The dollar amount of the sum total of expenses that match the criteria.
//     */
//    public BigDecimal getTotalCost(User us, int month, int year) {
//        String[] args = {
//                String.format("%04d-%02d-__", year, month + 1)
//        };
//        String[] cols = { ExpenseData.COST_COLUMN };
//        Cursor res = database.query(ExpenseData.EXPENSES_TABLE, cols, ExpenseData.USER_ID + " = '" +
//                us.getId() + "' AND " + /*ExpenseData.MONTH_COLUMN + " = '" + Integer.toString(month) +
//                "' AND " + ExpenseData.YEAR_COLUMN + " = '" + Integer.toString(year) + "'"*/
//                "vdate like ? ", args, null, null, null);
//
//        BigDecimal totCost = new BigDecimal(0);
//        res.moveToFirst();
//        while (!res.isAfterLast()) {
//            totCost = totCost.add(new BigDecimal(res.getLong(0)));
//            res.moveToNext();
//        }
//        res.close();
//
//        // move decimal point
//        return totCost.movePointLeft(2);
//    }

    /** used to export data from the app */
    public Cursor getCursorExpensesAllRows() {
        String sql = "select expense_id as _id,expenses.user_id as user_id,vdate,categories.category as cat_name,description,cost " +
                "from expenses " +
                "left join categories " +
                "on expenses.cat_id = categories.cat_id " +
                "order by user_id,vdate,_id asc ";
        return database.rawQuery(sql, null);
    }

    /** columns are _id,cost,description,vdate */
    public Cursor getCursorExpensesByUser(UserId userId) {
        String[] args = new String[] {
                userId.toString()
        };
        String sql = "select expense_id as _id,cost,description,vdate " +
                "from expenses where user_id = ? " +
                "order by vdate,_id asc ";
        return database.rawQuery(sql, args);
    }

    /** columns are _id,cost,description,vdate */
    public Cursor getCursorExpensesByUser(UserId userId, CatId catId) {
        String[] args = new String[] {
                userId.toString(),
                catId.toString()
        };
        String sql = "select expense_id as _id,cost,description,vdate " +
                "from expenses where user_id = ? and cat_id = ? " +
                "order by vdate,_id asc ";
        return database.rawQuery(sql, args);
    }

    /** columns are _id,cost,description,vdate */
    public Cursor getCursorExpensesLatest(UserId userId, int cnt) {
        String[] args = new String[] {
                userId.toString()
        };
        // count the number of records
        String sql = "select count(expense_id) " +
                "from expenses where user_id = ? ";
        Cursor res = database.rawQuery(sql, args);
        res.moveToFirst();
        int offset = 0;
        if (!res.isAfterLast()) {
            offset = res.getInt(0);
        }
        res.close();
        if (offset >= cnt) offset -= cnt; else offset = 0;  // rows to skip
        // sort and pick the last rows
        args = new String[] {
                userId.toString(),
                Integer.toString(cnt),
                Integer.toString(offset)
        };
        sql = "select expense_id as _id,cost,description,vdate " +
                "from expenses where user_id = ?  " +
                "order by vdate,_id asc " +
                "limit ? offset ? ";
        return database.rawQuery(sql, args);
    }

    /** columns are _id,cost,description,vdate */
    public Cursor getCursorExpensesLatest(UserId userId, CatId catId, int cnt) {
        String[] args = new String[] {
                userId.toString(),
                catId.toString()
        };
        // count the number of records
        String sql = "select count(expense_id) " +
                "from expenses where user_id = ? and cat_id = ? ";
        Cursor res = database.rawQuery(sql, args);
        res.moveToFirst();
        int offset = 0;
        if (!res.isAfterLast()) {
            offset = res.getInt(0);
        }
        res.close();
        if (offset >= cnt) offset -= cnt; else offset = 0;  // rows to skip
        // sort and pick the last rows
        args = new String[] {
                userId.toString(),
                catId.toString(),
                Integer.toString(cnt),
                Integer.toString(offset)
        };
        sql = "select expense_id as _id,cost,description,vdate " +
                "from expenses where user_id = ? and cat_id = ? " +
                "order by vdate,_id asc " +
                "limit ? offset ? ";
        return database.rawQuery(sql, args);
    }

//    public Cursor getCursorExpensesByCat(User user, Category cat, int mon, int year) {
//        String[] args = {
//                user.getId().toString(),
//                cat.getId().toString(),
//                String.format("%04d-%02-__", year, mon+1)
//        };
//
//        String sql = "select expense_id as _id,cost,description " +
//                "from expenses where user_id = ? and cat_id = ? and vdate like ? " +
//                "order by vdate,_id asc ";
//        return database.rawQuery(sql, args);
//    }

    /**
     * Find the most-recently entered expense matching user and desc (Note: not latest date)
     * and return the cost field. Return 0 if no match.
     *
     * @param userId  the user id to match
     * @param desc  the description to match
     * @return   cost in cents
     */
    public int getLastCreatedCostByDesc(UserId userId, String desc) {
        String[] args = {
                userId.toString(),
                desc
        };

        String sql = "select cost " +
                "from expenses where user_id = ? and description = ? " +
                "order by expense_id desc " +
                "limit 1 ";
        Cursor res = database.rawQuery(sql, args);

        res.moveToFirst();
        int nCost = 0;
        if(!res.isAfterLast()) {
            nCost = res.getInt(0);
        }
        res.close();
        return nCost;
    }

    public List<String> getSortedCostValues(Expense exp) {
        ArrayList<String> list = new ArrayList<String>();
        String[] args = {
                exp.getUserId().toString(),
                exp.getCategoryId().toString()
        };

        String sql = "select distinct cost " +
                "from expenses where user_id = ? and cat_id = ? " +
                "order by cost asc ";
        Cursor res = database.rawQuery(sql, args);

        res.moveToFirst();
        String s;
        while(!res.isAfterLast()) {
            s = res.getString(0);       // converts int to string
            list.add(s);
            res.moveToNext();
        }
        res.close();
        return list;
    }

    /** used for auto-completion during EditExp activity */
    public List<String> getSortedDescValues(CatId cat_id) {
        /* cat_id are globally unique, so user_id not needed */
        ArrayList<String> list = new ArrayList<String>();
        String[] args = {
                cat_id.toString()
        };

        String sql = "select distinct description " +
                "from expenses where cat_id = ? " +
                "order by description collate nocase asc ";
        Cursor res = database.rawQuery(sql, args);

        res.moveToFirst();
        String s;
        while(!res.isAfterLast()) {
            s = res.getString(0);
            list.add(s);
            res.moveToNext();
        }
        res.close();
        return list;
    }

//    /**
//         * Retrieves the expenses for the specified user, category, month, and year.
//         * @param us The user for which expenses should be returned.
//         * @param cat The category for which expenses should be returned.
//         * @param mon The month for which expenses should be returned.
//         * @param yea The year for which expenses should be returned.
//         * @return A list of expenses that match the given criteria.
//         */
//    public List<Expense> getExpensesByCat(User us, Category cat, int mon, int yea) {
//        // underscore is sql wildcard
//        String[] args = {
//                String.format("%04d-%02d-__", yea, mon+1)
//        };
//        Cursor res = database.query(ExpenseData.EXPENSES_TABLE,
//                colsToReturn,
//                ExpenseData.USER_ID +
//                " = '" + us.getId() + "' AND " + ExpenseData.CATEGORY_ID + " = '" + cat.getId() +
//                "' AND " + /*ExpenseData.MONTH_COLUMN + " = '" + Integer.toString(mon) + "' AND " +
//                ExpenseData.YEAR_COLUMN + " = '" + Integer.toString(yea) + "'", */
//                        "vdate like ?",
//                args, null, null, null);
//
//        List<Expense> ans = convToExpenseList(res);
//
//        res.close();
//        return ans;
//    }

//    /**
//     * Retrieves all the expenses for the specified user and category for all months and years.
//     * @param us The user for which expenses should be returned.
//     * @param cat The category for which expenses should be returned.
//     * @return A list of expenses that match the given criteria.
//     */
//    public List<Expense> getExpensesByCat(User us, Category cat) {
//
//        Cursor res = database.query(ExpenseData.EXPENSES_TABLE, colsToReturn, ExpenseData.USER_ID +
//                " = '" + us.getId() + "' AND " + ExpenseData.CATEGORY_ID + " = '" +
//                cat.getId() + "'", null, null, null, null);
//
//        List<Expense> ans = convToExpenseList(res);
//
//        res.close();
//        return ans;
//    }

//    /**
//     * Retrieves all the expenses for the specified user.
//     * @param user The user for which expenses should be returned.
//     * @return A list of expenses that match the given criteria.
//     */
//    public List<Expense> getExpensesByUser(User user) {
//
//        Cursor res = database.query(ExpenseData.EXPENSES_TABLE, colsToReturn, ExpenseData.USER_ID +
//                " = " + user.getId(), null, null, null, null);
//
//        List<Expense> ans = convToExpenseList(res);
//
//        res.close();
//        return ans;
//    }

    private List<Expense> convToExpenseList(Cursor res) {
        List<Expense> ans = new ArrayList<>();
        res.moveToFirst();
        while (!res.isAfterLast()) {
            Expense ex = convToExpense(res);
            ans.add(ex);
            res.moveToNext();
        }
        return ans;
    }

    public static Expense convToExpense(Cursor res) {
        Expense exp = new Expense();
        exp.setRowId(res.getInt(0)); // expense id
        UserId userId = new UserId(res.getInt(1));
        exp.setUserId(userId);      // user id
        CatId catId = new CatId(res.getInt(2));
        exp.setCategoryId(catId);   // category id
        exp.setCost( res.getInt(3));  /*new BigDecimal(res.getLong(3)).movePointLeft(2));*/ // cost
        exp.setDescription(res.getString(4)); // description

//        int idate = res.getInt(5);
//        int imon = res.getInt(6);      // zero-based
//        int iyear = res.getInt(7);
//
//        // yyyy-mm-dd
//        String strDate = String.format("%04d-%02d-%02d", iyear, imon+1, idate);
        exp.setDate(res.getString(8));

        return exp;
    }
}
