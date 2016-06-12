package com.simgeoapps.expensereport;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Activity to display list of expenses for user's category.
 */
public class ViewExpenses extends ListActivity {
        //implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int REQUEST_EDIT = 99;
    private static final int REQUEST_ADD = 101;

    private static GlobalConfig gc;

    /** Expenses data source. */
    private ExpenseDao dbase;

    /** Currently active user, as specified in config class. */
    //private User curUser;

    /** Variable to hold currently specified date. */
    //private static Calendar date;

    /** Currently selected category, as specified by intent received from ViewCategories class. */
    private Category curCat;

    /** The sum total of all expenses for the active category. */
    private BigDecimal categoryTotal;

    /** Action mode for the context menu. */
    private ActionMode aMode;

    /** Call back methods for the context menu. */
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        /** TextView which displays category name. */
        private TextView title;

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_expenses, menu);

            // disable listener here; moved from onPrepareActionMode
            title = (TextView) findViewById(R.id.exCat);
            title.setClickable(false); // prevent navigation away from activity
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // EDIT 03/22/15: This method does not get called anymore
            // ANDROID ISSUE: 159527
//            title = (TextView) findViewById(R.id.exCat);
//            title.setClickable(false); // prevent navigation away from activity
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_edit:
                    // edit selected expense
                    showEditExpense();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.action_del:
                    // delete selected expense
                    deleteExpense();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // unselect item that was selected (if it wasn't deleted)
            final ListView lv = getListView();
            lv.clearChoices();
            lv.setItemChecked(lv.getCheckedItemPosition(), false);
            // ((ArrayAdapter<Expense>) getListAdapter()).notifyDataSetChanged();
            // prevent item selection when context menu is inactive
            // doesn't work if called in same thread and item remains highlighted;
            // calling from new thread as a work around
            lv.post(new Runnable() {
                @Override
                public void run() {
                    lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
                }
            });
            aMode = null;

            title.setClickable(true); // restore category name click
        }
    };

    /**
     * Class to asynchronously retrieve expenses from database.
     */
    private class GetExpensesByCat extends AsyncTask<Void, Void, List<Expense>> {
        @Override
        protected List<Expense> doInBackground(Void... params) {
            // retrieve all expenses for the user and category and specified month and year
            User curUser = gc.getCurUser();
            //return dbase.getExpensesByCat(curUser, curCat);
            return new ArrayList<Expense>();
        }

        @Override
        protected void onPostExecute(final List<Expense> result) {
            // use adapter to show elements in list
//            ArrayAdapter<Expense> aa = new ArrayAdapter<>(ViewExpenses.this,
//                    android.R.layout.simple_list_item_activated_1, result);
            MyArrayAdapter aa = new MyArrayAdapter(ViewExpenses.this);
            aa.addAll(result);
            setListAdapter(aa);

            final ListView lv = getListView();
            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                // Called when the user long-clicks on an item
                public boolean onItemLongClick(AdapterView<?> aView, View view, int i, long l) {
                    if (aMode != null) {
                        return false;
                    }
                    lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    // mark item at position i as selected
                    lv.setItemChecked(i, true);
                    // Start the CAB using the ActionMode.Callback defined above
                    aMode = ViewExpenses.this.startActionMode(mActionModeCallback);
                    return true;
                }
            });
        }

        private class MyArrayAdapter extends ArrayAdapter<Expense> {
            private Context ctx;

            public MyArrayAdapter(Context ctx) {
                super(ctx, R.layout.row_layout_exp);
                this.ctx = ctx;
            }
            /** number of View class types we can return. used to create a cache for convertView arg.  */
            @Override
            public int getViewTypeCount() {
                // our rows are all the same View object type
                return 1;
            }
            /** create/populate a custom row view to put in the associated ListView */
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Expense exp = this.getItem(position);

                // previous view may be passed back to us for re-use
                View v = convertView;
                if(v == null || (v instanceof LinearLayout) == false) {
                    LayoutInflater inf = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inf.inflate(R.layout.row_layout_exp, null);
                }

                TextView tv = (TextView)v.findViewById(R.id.rowDesc);
                tv.setText( exp.getDescription() );
                tv = (TextView)v.findViewById(R.id.rowCost);
                tv.setText( exp.getFormattedCost() );

                return v;
            }
        }
    }

    /**
     * Class to update widgets after asynchronous modification to database. The methods are called
     * from the gui-event thread.
     */
    private class DbaseChanged implements ExpenseUtil.Callback {
        private Expense orig;       // existing object in ArrayAdapter

        public DbaseChanged(Expense orig) {
            this.orig = orig;
        }

        @Override
        public void onAdd(Expense newExp) {
            if (orig == null || newExp == null) {
                return;
            }
            if (orig.getUserId() != newExp.getUserId() ||
                    orig.getCategoryId() != newExp.getCategoryId()) {
                // item not in current category, so nothing to do
                return;
            }

            @SuppressWarnings("unchecked")
            ArrayAdapter<Expense> aa = (ArrayAdapter<Expense>) getListAdapter();
            aa.add(newExp);
            aa.notifyDataSetChanged();

            // update total
            categoryTotal = categoryTotal.add(newExp.getCost());
            repaintTotal();
        }

        @Override
        public void onEdit(Expense newExp) {
            if (orig == null || newExp == null) {
                return;
            }
            if (orig.getUserId() != newExp.getUserId() ||
                    orig.getCategoryId() != newExp.getCategoryId()) {
                // item effectively deleted from current category
                onDelete(newExp);
                return;
            }

            if (orig.getCost().equals(newExp.getCost()) == false) {
                // update total
                categoryTotal = categoryTotal.subtract(orig.getCost());
                categoryTotal = categoryTotal.add(newExp.getCost());
                repaintTotal();
            }

            // update object in ArrayAdapter
            orig.setCost( newExp.getCost());
            orig.setDescription( newExp.getDescription());

            @SuppressWarnings("unchecked")
            ArrayAdapter<Expense> aa = (ArrayAdapter<Expense>) getListAdapter();
            aa.notifyDataSetChanged();
        }

        @Override
        public void onDelete(Expense result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<Expense> aa = (ArrayAdapter<Expense>) getListAdapter();
            aa.remove(result); // remove selected item from adapter
            aa.notifyDataSetChanged();

            // update total
            categoryTotal = categoryTotal.subtract(result.getCost());
            repaintTotal();
       }

        private void repaintTotal() {
            TextView total = (TextView) findViewById(R.id.exTotal);
            total.setText("Total: " + NumberFormat.getCurrencyInstance().format(categoryTotal));
        }
    }

    /**
     * Show activity to collection input for new expense.
     */
    private void showAddExpense() {
        User curUser = gc.getCurUser();
        //Category curCat = gc.getCurCat();
        // add item
        Expense exp = new Expense();
        exp.setUserId( curUser.getId() );
        exp.setCategoryId( curCat.getId() );
        exp.setDate( Expense.today() );

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
    private void addExpense(Expense newExp) {
        User curUser = gc.getCurUser();
        //Category curCat = gc.getCurCat();
        ExpenseUtil util = new ExpenseUtil(this, dbase, new DbaseChanged(null), curUser.getId(), curCat.getId());
        // update database
        util.addExpense(newExp);
    }

    /**
     * Show activity to edit selected expense.
     */
    private void showEditExpense() {
        // retrieve adapter and retrieve selected expense
        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        final ArrayAdapter<Expense> aa = (ArrayAdapter<Expense>) getListAdapter();
        // get item at checked pos
        selectedExp = aa.getItem(lv.getCheckedItemPosition());

        Bundle bundle = new Bundle();
        selectedExp.copyToBundle(bundle);
        Intent intent = new Intent(this, EditExp.class);
        intent.setAction(EditExp.ACTION_EDIT);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    /**
     * Method to update database after user edits values; assume selectedExp points at object in
     * arrayadapter
     */
    private void editExpense(Expense newExp) {
        User curUser = gc.getCurUser();
        //Category curCat = gc.getCurCat();
        ExpenseUtil util = new ExpenseUtil(this, dbase, new DbaseChanged(selectedExp), curUser.getId(), curCat.getId());
        // update database
        util.editExpense(newExp);
    }

    /**
     * Method to delete selected expense from database. Called when Delete button is clicked in
     * context menu.
     */
    private void deleteExpense() {
        // get list view and list adapter
        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        ArrayAdapter<Expense> aa = (ArrayAdapter<Expense>) getListAdapter();
        int pos = lv.getCheckedItemPosition(); // get pos of selected item
        Expense del = aa.getItem(pos); // get item in adapter at position pos

        User curUser = gc.getCurUser();
        //Category curCat = gc.getCurCat();
        ExpenseUtil util = new ExpenseUtil(this, dbase, new DbaseChanged(del), curUser.getId(), curCat.getId());
        util.deleteExpense(del.getRowId());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_expenses);

        // add "<" to action bar
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

//        ActionBar actionBar = getActionBar();
//        if (actionBar != null) {
//            actionBar.setHomeButtonEnabled(true);
//        }

        Intent intent = getIntent();
        curCat = (Category)intent.getSerializableExtra(IntentTags.CURRENT_CATEGORY);

        gc = (GlobalConfig) getApplication();

        User curUser = gc.getCurUser();
//        Calendar curDate = gc.getCurDate();
//        Category curCat = gc.getCurCat();
        // set totalCost = ; here

        // set title to category
        TextView title = (TextView) findViewById(R.id.exCat);
        title.setText(curCat.getName());
//        title.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent it = new Intent(ViewExpenses.this, ViewCategories.class);
//                startActivity(it);
//            }
//        });

        // open data source
        dbase = new ExpenseDao(this);
        dbase.open();

        // display total for user, cat
        categoryTotal = dbase.getTotalCost(curUser, curCat);
        TextView total = (TextView) findViewById(R.id.exTotal);
        total.setText("Total: " + NumberFormat.getCurrencyInstance().format(categoryTotal));

        GetExpensesByCat job = new GetExpensesByCat();
        job.execute(); // retrieve display expenses for the category
        //displayListView();
    }

//    private void displayListView() {
//
//        Cursor cursor = dbase.getCursorExpensesByCat(curUser, curCat,
//                date.get(Calendar.MONTH), date.get(Calendar.YEAR));
//
//        // activity will close the cursor when necessary
//        //startManagingCursor(cursor);
//
//        // the desired columns to be bound
//        String[] columns = new String[] {
//                "_id",
//                ExpenseData.COST_COLUMN,
//                ExpenseData.DESCRIPTION_COLUMN
//        };
//
//        // the XML defined views which the data will be bound to
//        int[] to = new int[] {
//            R.id.rowDate,
//            R.id.rowCost,
//            R.id.rowDesc
//        };
//
//        //SimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to)
//        SimpleCursorAdapter adapt = new SimpleCursorAdapter(this,
//                R.layout.row_layout_all_exp,
//                cursor,
//                columns,
//                to
//                );
//
//        setListAdapter(adapt);
//    }

//    private SimpleCursorAdapter mAdapter;
//
//    private void displayListView2() {
//        // Prepare the loader.  Either re-connect with an existing one,
//        // or start a new one.
//        getLoaderManager().initLoader(0, null, this);
//    }
//
//    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//        // This is called when a new Loader needs to be created.  This
//        // sample only has one Loader, so we don't care about the ID.
//
//        // the desired columns to be bound
//        String[] columns = new String[] {
//                ExpenseData.COST_COLUMN,
//                ExpenseData.DESCRIPTION_COLUMN
//        };
//
//        // the XML defined views which the data will be bound to
//        int[] to = new int[] {
//                R.id.rowCost,
//                R.id.rowDesc
//        };
//
//        // Now create and return a CursorLoader that will take care of
//        // creating a Cursor for the data being displayed.
//        String select = "((" + Contacts.DISPLAY_NAME + " NOTNULL) AND ("
//                + Contacts.HAS_PHONE_NUMBER + "=1) AND ("
//                + Contacts.DISPLAY_NAME + " != '' ))";
//
//        return new CursorLoader( getActivity(),
//                baseUri,
//                columns,
//                select,
//                null,
//                null);
//    }
//
//    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        // Swap the new cursor in.  (The framework will take care of closing the
//        // old cursor once we return.)
//        mAdapter.swapCursor(data);
//    }
//
//    public void onLoaderReset(Loader<Cursor> loader) {
//        // This is called when the last Cursor provided to onLoadFinished()
//        // above is about to be closed.  We need to make sure we are no
//        // longer using it.
//        mAdapter.swapCursor(null);
//    }

    @Override
    protected void onPause() {
//        SimpleCursorAdapter adapt = (SimpleCursorAdapter)getListAdapter();
//        Cursor oldCursor = adapt.swapCursor(null);
//        //stopManagingCursor(oldCursor);
//        oldCursor.close();
//        dbase.close();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

//        dbase.open();
//        displayListView();
    }

    @Override
    protected void onDestroy() {
//        SimpleCursorAdapter adapt = (SimpleCursorAdapter)getListAdapter();
//        Cursor oldCursor = adapt.swapCursor(null);
//        //stopManagingCursor(oldCursor);
//        oldCursor.close();
        dbase.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.view_expenses, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            showAddExpense();
            return true;
        } else if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // remember item being edited right now
    private Expense selectedExp;

    /** called with results after user finishes editing record fields. data is null if activity
     * was cancelled.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) return;
        Bundle bundle = data.getExtras();
        Expense newExp = new Expense(bundle);
        if (requestCode == REQUEST_ADD && resultCode == RESULT_OK) {
            // add: update db
            addExpense(newExp);
        } else if (requestCode == REQUEST_EDIT && resultCode == RESULT_OK) {
            // edit: update db
             editExpense(newExp);
        }
    }
}
