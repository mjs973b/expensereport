package name.mjs001.expensereport;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Helper class to add, edit or delete an expense. Shows a dialog box for add or edit and
 * updates the database if ok is clicked. If the caller provides a callback, it is called if the
 * database is modified. The caller needs to provide an open database object.
 */
public class ExpenseUtil {

    private Context context;        // for current activity
    private ExpenseDao dbase;       // database access
    private Callback callback;      // notification that db changed
    private UserId userId;          // expense user (for obsolete dialog)
    private CatId catId;            // expense category (for obsolete dialog)

    public ExpenseUtil(Context ctx, ExpenseDao db, Callback cb, UserId userId, CatId catId) {
        context = ctx;
        dbase = db;
        callback = cb;
        this.userId = userId;
        this.catId = catId;
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
     * Method to record a new expense. Called when Add button in action bar is clicked.
     */
    public void addExpense() {
        // build dialog to ask for expense details
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Record expense");
        builder.setMessage("Please enter expense details.");

        // construct input fields
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        final AutoCompleteTextView enterCost = new AutoCompleteTextView(context);
//        List<String> costHistory = dbase.getSortedCostValues(user);
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
//                android.R.layout.simple_dropdown_item_1line, costHistory);
//        enterCost.setAdapter(adapter);
        enterCost.setHint("Cost");
        enterCost.setInputType(InputType.TYPE_CLASS_NUMBER); // to accept dollar amount
        enterCost.setKeyListener(DigitsKeyListener.getInstance("0123456789.")); // accept digits

        final EditText enterDesc = new EditText(context);
        enterDesc.setHint("Description (optional)");
        //enterDesc.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); // description text
        enterDesc.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        ll.addView(enterCost);
        ll.addView(enterDesc);
        builder.setView(ll);

        // add ok and cancel buttons
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        // create dialog
        final AlertDialog dia = builder.create(); // don't show yet

        // set listener to description input field to click OK when done
        enterDesc.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // click dialog's OK when user presses Done on keyboard
                    dia.getButton(Dialog.BUTTON_POSITIVE).performClick();
                    handled = true;
                }
                return handled;
            }
        });

        // set input mode to let keyboard appear when dialog is shown
        dia.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        dia.show();

        // override onclick for OK button; must be done after show()ing to retrieve OK button
        dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // retrieve name entered
                String cost = enterCost.getText().toString().trim();
                String desc = enterDesc.getText().toString().trim();

                // perform checks and add if pass
                if (cost.equals("")) { // must not be empty
                    enterCost.setError("Please enter a dollar amount.");
                } else if (!Pattern.matches("^(\\d{1,10})?(\\.\\d{0,2})?$", cost)) { // must be $$
                    enterCost.setError("Please enter a valid dollar amount.");
                } else {
                    // can be added
                    Expense exp = new Expense();
                    exp.setUserId(userId);
                    exp.setCategoryId(catId);
                    exp.setCost(new BigDecimal(cost));
                    exp.setDescription(desc);
                    AddExpense job = new AddExpense(exp);
                    job.execute();
                    dia.dismiss();
                }
            }
        });
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

    /**
     * Method to edit selected expense. Called when Edit button is clicked in context menu.
     */
    public void editExpense(long rowId) {
        Expense orig = dbase.lookupExp(rowId);
        if (orig == null) return;

        // build dialog to ask for expense details
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit expense");
        builder.setMessage("Please enter expense details.");

        // construct input fields
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        final AutoCompleteTextView enterCost = new AutoCompleteTextView(context);
//        List<String> costHistory = dbase.getSortedCostValues(orig);
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
//                android.R.layout.simple_dropdown_item_1line, costHistory);
//        enterCost.setAdapter(adapter);
        String sCost = orig.getCost().toPlainString();
        if (sCost.endsWith(".00")) sCost = sCost.substring(0, sCost.length()-3);
        enterCost.setText(sCost);
        enterCost.setInputType(InputType.TYPE_CLASS_NUMBER); // to accept dollar amount
        enterCost.setKeyListener(DigitsKeyListener.getInstance("0123456789.")); // accept digits

        final EditText enterDesc = new EditText(context);
        enterDesc.setText(orig.getDescription());
        enterDesc.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); // description text
        enterDesc.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});

        ll.addView(enterCost);
        ll.addView(enterDesc);
        builder.setView(ll);

        // add ok and cancel buttons
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        // create dialog
        final AlertDialog dia = builder.create(); // don't show yet

        // set listener to description input field to click OK when done
        enterDesc.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // click dialog's OK when user presses Done on keyboard
                    dia.getButton(Dialog.BUTTON_POSITIVE).performClick();
                    handled = true;
                }
                return handled;
            }
        });

        // set input mode to let keyboard appear when dialog is shown
        dia.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        dia.show();

        // override onclick for OK button; must be done after show()ing to retrieve OK button
        MyClickListener listenOK = new MyClickListener(orig, dia, enterCost, enterDesc);
        dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(listenOK);

        // move cursor to end
        enterDesc.setSelection(enterDesc.getText().length());
        enterCost.setSelection(enterCost.getText().length());
    }

    private class MyClickListener implements View.OnClickListener {
        private Expense exp;
        private AlertDialog dlg;
        private EditText wCost;
        private EditText wDesc;

        public MyClickListener(Expense exp, AlertDialog dlg, EditText wCost, EditText wDesc) {
            this.exp = exp;
            this.dlg = dlg;
            this.wCost = wCost;
            this.wDesc = wDesc;
        }

        @Override
        public void onClick(View v) {
            // retrieve name entered
            String cost = wCost.getText().toString().trim();
            String desc = wDesc.getText().toString().trim();

            // perform checks and add if pass
            if (cost.equals("")) { // must not be empty
                wCost.setError("Please enter a dollar amount.");
            } else if (!Pattern.matches("^(\\d{1,10})?(\\.\\d{0,2})?$", cost)) { // must be $$
                wCost.setError("Please enter a valid dollar amount.");
            } else {
                // can be changed
                Expense newExp = new Expense(exp);
                newExp.setCost(new BigDecimal(cost));
                newExp.setDescription(desc);

                dlg.dismiss();

                EditExpense job = new EditExpense(newExp);
                job.execute();
            }
        }
    } // class

}
