package name.mjs001.expensereport;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Helper class to add, edit or delete an expense. Shows a dialog box for add or edit and
 * updates the database if ok is clicked. If the caller provides a callback, it is called if the
 * database is modified. The caller needs to provide an open database object.
 */
public class ExpenseUtil {

    private Context context;        // for current activity
    private ExpenseDao dbase;       // database access
    private Callback callback;      // notification that db changed

    public ExpenseUtil(Context ctx, ExpenseDao db, Callback cb) {
        context = ctx;
        dbase = db;
        callback = cb;
    }

    /**
     * Class to asynchronously add new expense to database.
     */
    private class AddExpense extends AsyncTask<Void, Void, Expense> {
        private Expense exp;

        public AddExpense(Expense exp) {
            this.exp = exp;
        }
        @Override
        protected Expense doInBackground(Void... params) {
            dbase.newExpense(exp);
            return exp;
        }

        @Override
        protected void onPostExecute(Expense result) {
            if (callback != null) {
                try {
                    callback.onAdd(result);
                } catch(Exception e) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Class to asynchronously edit an expense in database.
     */
    private class EditExpense extends AsyncTask<Void, Void, Expense> {
        private Expense exp;

        public EditExpense(Expense exp) {
            this.exp = exp;
        }
        @Override
        protected Expense doInBackground(Void... params) {
            dbase.updateExpense(exp);
            return exp;
        }
        @Override
        protected void onPostExecute(Expense result) {
            if (callback != null) {
                try {
                    callback.onEdit(result);
                } catch(Exception e) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Class to asynchronously delete an expense from database.
     */
    private class DeleteExpense extends AsyncTask<Void, Void, Expense> {
        private Expense exp;

        public DeleteExpense(Expense exp) {
            this.exp = exp;
        }
        @Override
        protected Expense doInBackground(Void... params) {
            // delete item from db
            dbase.deleteExpense(exp);
            return exp;
        }
        @Override
        protected void onPostExecute(Expense result) {
            if (callback != null) {
                try {
                    callback.onDelete(result);
                } catch(Exception e) {
                    // do nothing
                }
            }
        }
    }

    public interface Callback {
        public void onAdd(Expense exp);
        public void onEdit(Expense exp);
        public void onDelete(Expense exp);
    }

    /** write value to database */
    public void addExpense(Expense newExp) {
        // add to database in background, then notify caller
        AddExpense job = new AddExpense(newExp);
        job.execute();
    }

    /** write value to database */
    public void editExpense(Expense newExp) {
        // add to database in background, then notify caller
        EditExpense job = new EditExpense(newExp);
        job.execute();
    }

    /**
     * Method to delete selected expense. Called when Delete button is clicked in context menu.
     */
    public void deleteExpense(long rowId) {
        Expense exp = dbase.lookupExp(rowId);
        if (exp == null) return;
        DeleteExpense job = new DeleteExpense(exp);
        job.execute(); // delete expense async
    }
}
