package name.mjs001.expensereport;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

/**
 * Activity to display and modify a list of expenses. Can be filtered by various criteria.
 */
public class ViewExpensesByTime extends ListActivity {
    private static final int REQUEST_EDIT = 99;     // for EditExp Activity
    private static final int REQUEST_ADD = 101;     // for EditExp Activity

    private GlobalConfig gc;

    /** Expenses data source. */
    private ExpenseDao dbase;

    /**
     * Remember the expense date the user chooses in add activity.
     * This does not survive activity destruction.
     */
    private String lastAddDate;     // yyyy-mm-dd
    private Date lastAddModifyTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_all_expenses);

        // get shared app config
        gc = (GlobalConfig) getApplication();

        lastAddDate = Expense.today();
        lastAddModifyTime = new Date();     // now

        // ask system to call onContextMenuCreate() for long click
        ListView listView = (ListView)findViewById(android.R.id.list);

        // long-press will invoke context menu
        registerForContextMenu(listView);


        // the desired columns to be bound
        String[] columns = new String[] {
                ExpenseData.VDATE_COLUMN,
                ExpenseData.COST_COLUMN,
                ExpenseData.DESCRIPTION_COLUMN
        };

        // the XML defined views which the data will be bound to
        int[] to = new int[] {
                R.id.rowDate,
                R.id.rowCost,
                R.id.rowDesc
        };

        //SimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to)
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.row_layout_all_exp,
                null,   //cursor,
                columns,
                to
        );

        adapter.setViewBinder( new MyViewBinder() );
        setListAdapter(adapter);

        // open data source
        dbase = new ExpenseDao(this);
        dbase.open();
    }

    /** if cat is changed, we need to reload the expense item list */
    private class CatListener implements AdapterView.OnItemSelectedListener {
        public void onNothingSelected(AdapterView<?> parent) {
            // Another interface callback
        }
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            Category cat = (Category)parent.getItemAtPosition(pos);
            gc.setViewCatId(cat.getId());
            refreshScreen();
        }
    }

    /** populate the spinner with cat items for the specified user */
    private void loadCategories(UserId userId) {
        CategoryDao dbase2 = new CategoryDao(this);
        dbase2.openReadonly();
        List<Category> catList = dbase2.getCategories(userId);
        dbase2.close();

        // Pseudo-category is first
        catList.add(0, Category.SHOW_ALL);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<Category> catAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, catList);
        // Specify the layout to use when the list of choices appears
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner tvCat = (Spinner)findViewById(R.id.choose_cat);
        tvCat.setAdapter(catAdapter);
        tvCat.setOnItemSelectedListener(new CatListener());

        // try to restore the selection from last time (may fail if user changed)
        CatId lastSel = gc.getViewCatId();
        for(int i = 0; i < catList.size(); i++) {
            if (lastSel.equals(catList.get(i).getId())) {
                tvCat.setSelection(i);
                break;
            }
        }
    }

    /** called when the show mode or category is changed, User has not changed. */
    private void refreshScreen() {
        if (gc.getCurView() == GlobalConfig.VIEW_ALL) {
            setTitle(R.string.title_view_all_expenses);
        } else {
            setTitle(R.string.title_view_recent_expenses);
        }
        // update expense item list
        reloadCursor();
    }

    private void reloadCursor() {
        SimpleCursorAdapter adapt = (SimpleCursorAdapter)getListAdapter();
        UserId userId = gc.getCurUser().getId();
        CatId catId = gc.getViewCatId();
        int cat_id = catId.toInt();  // 0 means invalid

        Cursor newCursor;
        if (gc.getCurView() == GlobalConfig.VIEW_ALL) {
            if (cat_id == 0 || cat_id == Category.SHOW_ALL_CAT_ID) {
                newCursor = dbase.getCursorExpensesByUser(userId);
            } else {
                newCursor = dbase.getCursorExpensesByUser(userId, catId);
            }
        } else {
            int NUM_ITEMS = 7;
            if (cat_id == 0 || cat_id == Category.SHOW_ALL_CAT_ID) {
                newCursor = dbase.getCursorExpensesLatest(userId, NUM_ITEMS);
            } else {
                newCursor = dbase.getCursorExpensesLatest(userId, catId, NUM_ITEMS);
            }
        }
        // adapter will notify listview to redraw
        Cursor oldCursor = adapt.swapCursor(newCursor);
        if (oldCursor != null) {
            oldCursor.close();
        }
    }

    /** don't hold a cursor when another activity is foremost */
    private void disconnListView() {
        SimpleCursorAdapter adapt = (SimpleCursorAdapter)getListAdapter();
        Cursor oldCursor = adapt.swapCursor(null);
        //stopManagingCursor(c);
        oldCursor.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // user or cat or expense data might have changed from last time
        User user = gc.getCurUser();
        loadCategories(user.getId());
        reloadCursor();
    }

    @Override
    protected void onPause() {
        disconnListView();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        dbase.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu_all_expenses, menu);
        // initialize check mark state
        MenuItem item = menu.findItem(R.id.action_show_recent);
        if (item != null) {
            item.setChecked(gc.getCurView() == GlobalConfig.VIEW_RECENT);
        }
        return true;
    }

    /** the menu in the upper right */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            showAddExpense();
            return true;
        } else if (id == R.id.action_show_recent) {
            // toggle check mark
            boolean showRecent = !item.isChecked();
            item.setChecked(showRecent);
            // remember
            gc.setCurView( showRecent ? GlobalConfig.VIEW_RECENT : GlobalConfig.VIEW_ALL );
            refreshScreen();
            return true;
        } else if (id == R.id.action_man_cats) {
            Intent intent = new Intent(this, ViewCategories.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_man_users) {
            Intent intent = new Intent(this, ViewUsers.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_export_data) {
            // kick off in background thread
            DataExport export = new DataExport(this);
            new Thread(export).start();
            //export.writeFile();
            return true;
        } else if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // temporarily store the dbase row _id of the selected item
    private long expenseId;

    /** called when the user long-presses a row. v is the ListView */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.menu_exp_title);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_expenses, menu);

        // _id field from row
        expenseId = ((AdapterView.AdapterContextMenuInfo)menuInfo).id;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        long rowId = expenseId;
        expenseId = 0;

        int id = item.getItemId();
        if(id == R.id.action_edit) {
            showEditExpense(rowId);
            return true;
        } else if(id == R.id.action_del){
            deleteExpenseInDb(rowId);
            return true;
        }
        // not handled by us
        return super.onContextItemSelected(item);
    }


    /**
     * Called with results after user finishes editing record fields. Data contains all the field
     * values in the extras bundle, or is null if activity was cancelled.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        Bundle bundle = data.getExtras();
        Expense newExp = new Expense(bundle);

        if (requestCode == REQUEST_ADD) {
            addExpenseToDb(newExp);
        } else if (requestCode == REQUEST_EDIT) {
            editExpenseInDb(newExp);
        }
    }

    /** this class populates the 3 TextViews in each row of the ListAdapter */
    private class MyViewBinder implements SimpleCursorAdapter.ViewBinder {

        // v is the cell within the row, col is configured cursor src column
        @Override
        public boolean setViewValue(View v, Cursor cursor, int col) {
//    Cursor layout is:
//        0    "_id", int
//        1    ExpenseData.COST_COLUMN, int
//        2    ExpenseData.DESCRIPTION_COLUMN, string
//        3    VDATE, string, yyyy-mm-dd
            String s = "?";
            if (col == 3) {                     // date
                s = cursor.getString(col).substring(5);
            } else if (col == 1) {            // cost
                int cents = cursor.getInt(col);
                s = Expense.formatCost(cents);
            } else if (col == 2) {
                s = cursor.getString(col);        // desc
            }

            TextView tv = (TextView)v;
            tv.setText(s);
            return true;
        }
    }

    /**
     * Show activity to collect values for new expense entry.
     */
    private void showAddExpense() {

        // add item
        Expense exp = new Expense();

        User curUser = gc.getCurUser();
        exp.setUserId( curUser.getId() );

        /* try to be clever: if user viewing a single category, use that one, else preset to
         * the last category the user added */
        CatId catId = gc.getViewCatId();
        if (!catId.isUserCreated()) {
            catId = gc.getAddCatId();
        }
        exp.setCategoryId(catId);

        // if user set an expense date in last 2 minutes, default to that value
        boolean usePrevDate = false;
        if (lastAddDate != null && lastAddModifyTime != null) {
            long deltaSinceLastMillis = new Date().getTime() - lastAddModifyTime.getTime();
            usePrevDate = deltaSinceLastMillis < 2*60*1000;
        }
        exp.setDate(usePrevDate ? lastAddDate : Expense.today() );

        // transfer fields in a bundle
        Bundle bundle = new Bundle();
        exp.copyToBundle(bundle);

        Intent intent = new Intent(this, EditExp.class);
        intent.setAction(EditExp.ACTION_ADD);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_ADD); // start edit activity
    }

    /**
     * Method to update database with new expense. Called by onActivityResult()
     */
    private void addExpenseToDb(Expense newExp) {

        // config background thread
        ExpenseUtil util = new ExpenseUtil(this, dbase, new DbaseChanged(), null, null);
        // update database
        util.addExpense(newExp);

        // remember expense date in case user adds another item
        lastAddDate = newExp.getDate();
        lastAddModifyTime = new Date();     // now
    }

    /**
     * Show activity to edit selected expense.
     */
    private void showEditExpense(long rowId) {
        // need existing fields to initialize the EditExp activity
        Expense exp = dbase.lookupExp(rowId);
        if (exp == null) {
            Toast.makeText(this, "Invalid rowid", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle bundle = new Bundle();
        exp.copyToBundle(bundle);

        Intent intent = new Intent(this, EditExp.class);
        intent.setAction(EditExp.ACTION_EDIT);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    /**
     * Method to update database after user edits values; assume selectedExp points at object in
     * arrayadapter
     */
    private void editExpenseInDb(Expense newExp) {
        // config background thread
        ExpenseUtil util = new ExpenseUtil(this, dbase, new DbaseChanged(), null, null);
        // update database
        util.editExpense(newExp);
    }

    /**
     * Method to delete selected expense from database. Called when Delete button is clicked in
     * context menu.
     */
    private void deleteExpenseInDb(long rowId) {
        // config background thread
        ExpenseUtil util = new ExpenseUtil(this, dbase, new DbaseChanged(), null, null);
        // update database
        util.deleteExpense(rowId);
    }

    /** after the database is modified, this is called to update our screen */
    private class DbaseChanged implements ExpenseUtil.Callback {
        public void onAdd(Expense exp) {
            reloadCursor();
        }
        public void onEdit(Expense exp) {
            reloadCursor();
        }
        public void onDelete(Expense exp) {
            reloadCursor();
        }
    }
}
