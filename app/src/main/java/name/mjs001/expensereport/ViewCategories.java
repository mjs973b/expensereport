package name.mjs001.expensereport;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.InputFilter;
import android.text.InputType;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Activity to display list of categories for a user.
 */
public class ViewCategories extends ListActivity {

    /** Category data source. */
    private CategoryDao catSource;

    /** Expense data source. Used for calculating total cost. */
    //private ExpenseDao exSource;

    private static GlobalConfig gc;

    /** Currently active user, as specified in global config class. */
    //private User curUser;

    /** Variable to hold currently specified date. */
    //private static Calendar date;

    /** Variable to hold the title TextView of the activity. Displays month and year. */
//    private TextView acTitle;

    //private TextView tvTotal;

    //private ArrayAdapter<Category> adapter;

//    public static final String[] MONTHS = { "January", "February", "March", "April", "May", "June",
//            "July", "August", "September", "October", "November", "December"};

    /** Action mode for the context menu. */
    private ActionMode aMode;

    /** Call back methods for the context menu. */
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        /** To temporarily store items' listener when removed. */
        private AdapterView.OnItemClickListener iLstn;

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_categories, menu);

            // disable listeners here; moved from onPrepareActionMode
            ListView lv = getListView();
            iLstn = lv.getOnItemClickListener();
            lv.setOnItemClickListener(null);
            // disable title on click which would open date picker
//            acTitle.setClickable(false);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // EDIT: This method does not get called anymore!
            // ANDROID ISSUE: 159527
            // moved code to onCreateActionMode

            // disable other listeners temporarily to prevent multiple actions
            // disable on item click which would start expenses activity
//            ListView lv = getListView();
//            iLstn = lv.getOnItemClickListener();
//            lv.setOnItemClickListener(null);
//            // disable title on click which would open date picker
//            acTitle.setClickable(false);
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_edit:
                    // edit a category name
                    editCategory();
                    mode.finish(); // close the CAB
                    return true;
                case R.id.action_del:
                    // delete selected category
                    deleteCategory();
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

            // restore listeners
            getListView().setOnItemClickListener(iLstn);
//            acTitle.setClickable(true);
        }
    };

//    /**
//     * Static class for the date picker dialog.
//     */
//    public static class DateSelector extends DialogFragment
//            implements DatePickerDialog.OnDateSetListener {
//
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState) {
//            // Use the date specified in config as the default date in the picker
//            Calendar curDate = gc.getCurDate();
//            int year = curDate.get(Calendar.YEAR);
//            int month = curDate.get(Calendar.MONTH);
//            int day = curDate.get(Calendar.DAY_OF_MONTH); // don't need this
//
//            // Create a new instance of DatePickerDialog and return it
//            return new DatePickerDialog(getActivity(), this, year, month, day);
//        }
//
//        @SuppressWarnings("unchecked")
//        @Override
//        public void onDateSet(DatePicker view, int year, int month, int day) {
//            // specify new date in config
//            Calendar c = Calendar.getInstance();
//            c.set(year, month, day); // set new date
////            GlobalConfig gc = (GlobalConfig) getActivity().getApplication();
//            gc.setCurDate(c); // change global date
////            date = c; // change var for this activity
//
////            // change title to reflect new date
////            TextView title = (TextView) getActivity().findViewById(R.id.catMon);
////            title.setText(MONTHS[date.get(Calendar.MONTH)] + " " + date.get(Calendar.YEAR));
//
//            // refresh month/year total for all categories
////            TextView total = (TextView) getActivity().findViewById(R.id.monYTot);
////            total.setText("Total: " + NumberFormat.getCurrencyInstance().format(
////                    ((ViewCategories) getActivity()).exSource.getTotalCost(gc.getCurUser(), month, year)));
////
////            // refresh categories; must show new totals for the new month/year
////            ((ArrayAdapter<Category>) ((ViewCategories) getActivity()).getListAdapter()).notifyDataSetChanged();
//            ((ViewCategories)getActivity()).refreshAll();
//        }
//    }

//    public void refreshAll() {
//        // the gc date has probably changed, but the set of categories is unchanged
//        updateTitle();
////        updateGrandTotal();
//        adapter.notifyDataSetChanged();
//    }

//    /**
//     * Class to asynchronously retrieve categories from database.
//     */
//    private class GetCategories extends AsyncTask<Void, Void, List<Category>> {
//        @Override
//        protected List<Category> doInBackground(Void... params) {
//            return catSource.getCategories(gc.getCurUser());
//        }
//
//        @Override
//        protected void onPostExecute(final List<Category> result) {
//            // use adapter to show the elements in a ListView
//            // change to custom layout if necessary
//            adapter = new ArrayAdapter<Category>(ViewCategories.this,
//                    R.layout.row_layout_category, R.id.catLabel, result) {
//
//                // override get view in order to allow two items to be displayed: title and total cost
//                @Override
//                public View getView(int position, View convertView, ViewGroup parent) {
//                    View view = super.getView(position, convertView, parent);
//                    TextView text1 = (TextView) view.findViewById(R.id.catLabel);
////                    TextView text2 = (TextView) view.findViewById(R.id.catCost);
//                    text1.setText(result.get(position).toString());
////                    User curUser = gc.getCurUser();
////                    Calendar curDate = gc.getCurDate();
////                    int month = curDate.get(Calendar.MONTH);
////                    int year = curDate.get(Calendar.YEAR);
////                    Category curCat = result.get(position);
//                    // database lookup
////                    BigDecimal sum = exSource.getTotalCost(curUser, curCat, month, year);
////                    text2.setText(NumberFormat.getCurrencyInstance().format(sum));
//                    return view;
//                }
//            };
//
//            setListAdapter(adapter);
//
//            final ListView lv = getListView();
////            // set item onclick listener to each item in list
////            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
////                @Override
////                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
////                    // retrieve selected category
////                    Category cat = adapter.getItem(i);
////
////                    // pass category to ViewExpenses activity and start it
////                    Intent intent = new Intent(ViewCategories.this, ViewExpenses.class);
////                    intent.putExtra(IntentTags.CURRENT_CATEGORY, cat);
////                    startActivity(intent);
////                }
////            });
//
//            // set long click listener, to display CAB
//            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//                // Called when the user long-clicks on an item
//                public boolean onItemLongClick(AdapterView<?> aView, View view, int i, long l) {
//                    if (aMode != null) {
//                        return false;
//                    }
//                    lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//                    // mark item at position i as selected
//                    lv.setItemChecked(i, true);
//                    // Start the CAB using the ActionMode.Callback defined above
//                    aMode = ViewCategories.this.startActionMode(mActionModeCallback);
//                    return true;
//                }
//            });
//        }
//    }

    /**
     * Class to asynchronously add new category to database.
     */
    private class AddCategory extends AsyncTask<String, Void, Category> {
        @Override
        protected Category doInBackground(String... params) {
            User curUser = gc.getCurUser();
            return catSource.newCategory(params[0], curUser);
        }

        @Override
        protected void onPostExecute(Category result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<Category> adapter = (ArrayAdapter<Category>) getListAdapter();
            adapter.add(result);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Class to asynchronously edit a category name in database.
     */
    private class EditCategory extends AsyncTask<Category, Void, Category> {
        @Override
        protected Category doInBackground(Category... params) {
            return catSource.editCategory(params[0]); // change in db;
        }

        @Override
        protected void onPostExecute(Category result) {
            // refresh view
            @SuppressWarnings("unchecked")
            ArrayAdapter<Category> aa = (ArrayAdapter<Category>) getListAdapter();
            aa.notifyDataSetChanged();
        }
    }

    /**
     * Class to asynchronously delete a category from database, along with all expense items
     * belonging to that category. Attempting to delete the last category for a user will fail.
     */
    private class DeleteCategory extends AsyncTask<Category, Void, Category> {
        private boolean status = false;

        @Override
        protected Category doInBackground(Category... params) {
            User user = gc.getCurUser();
            Category cat = params[0];
            // delete the cat and expenses from database
            status = catSource.deleteCategory(user, cat);
            return cat;
        }

        @Override
        protected void onPostExecute(Category result) {
            if (status) {
                @SuppressWarnings("unchecked")
                ArrayAdapter<Category> aa = (ArrayAdapter<Category>) getListAdapter();
                aa.remove(result); // remove from adapter
                aa.notifyDataSetChanged(); // update view
            } else {
                Toast t = Toast.makeText(ViewCategories.this, "Delete failed", Toast.LENGTH_SHORT);
                t.show();
            }
        }
    }

    /**
     * Retrieve all categories for the current user, populate the list view with them, and set listeners.
     */
    private void populateCategories() {

        // read database
        User curUser = gc.getCurUser();
        final List<Category> result = catSource.getCategories(curUser);

        // use adapter to show the elements in a ListView
        // change to custom layout if necessary
        ArrayAdapter<Category> adapter = new ArrayAdapter<Category>(ViewCategories.this,
                R.layout.row_layout_category, R.id.catLabel, result) {
            // override get view in order to allow two items to be displayed: title and total cost
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(R.id.catLabel);
//                TextView text2 = (TextView) view.findViewById(R.id.catCost);
                text1.setText(result.get(position).toString());
//                User curUser = gc.getCurUser();
//                Calendar curDate = gc.getCurDate();
//                text2.setText(NumberFormat.getCurrencyInstance().format(
//                        exSource.getTotalCost(curUser, result.get(position),
//                                curDate.get(Calendar.MONTH), curDate.get(Calendar.YEAR))));
                return view;
            }
        };
        setListAdapter(adapter);

        final ListView lv = getListView();
//        // set onclick listener on listview
//        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                // retrieve selected category
//                Category cat = adapter.getItem(i);
//
//                // pass category to ViewExpenses activity and start it
//                Intent intent = new Intent(ViewCategories.this, ViewExpenses.class);
//                intent.putExtra(IntentTags.CURRENT_CATEGORY, cat);
//                startActivity(intent);
//            }
//        });

        // set long click listener on listview, to display CAB
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
                aMode = ViewCategories.this.startActionMode(mActionModeCallback);
                return true;
            }
        });
    }

    /**
     * Method to add a new category. Called when Add button in action bar is clicked.
     */
    private void addCategory() {
        // build dialog to ask for category
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create category");
        builder.setMessage("Please enter a category name.");

        // construct input field
        final EditText enterCat = new EditText(this);
        enterCat.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); // capitalized phrase
        enterCat.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        builder.setView(enterCat);

        // add ok and cancel buttons
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        // create dialog
        final AlertDialog dia = builder.create(); // don't show yet

        // set listener to input field to click OK when done
        enterCat.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                String catName = enterCat.getText().toString().trim();
                User curUser = gc.getCurUser();

                // perform checks and add if pass
                if (catName.equals("")) { // must not be empty
                    enterCat.setError("Please enter a name.");
                } else if (catName.indexOf('"') >= 0) {
                    enterCat.setError("Double-quote character not permitted");
                } else if (catSource.exists(catName, curUser)) { // must not exist for current user
                    enterCat.setError("This category already exists.");
                } else {
                    // can be added
                    new AddCategory().execute(catName);
                    dia.dismiss();
                }
            }
        });
    }

    /**
     * Method to edit a category title, called when the Edit button in the context menu is clicked.
     */
    private void editCategory() {
        // retrieve adapter and retrieve selected category
        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        final ArrayAdapter<Category> aa = (ArrayAdapter<Category>) getListAdapter();
        final Category catToEdi = aa.getItem(lv.getCheckedItemPosition()); // get item at checked pos

        // show dialog to enter new name for the category
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit category");
        builder.setMessage("Please enter a new category name.");

        // construct input field
        final EditText enterName = new EditText(this);
        enterName.setText(catToEdi.getName()); // prepopulate with current category name
        enterName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); // capitalized phrase
        enterName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        builder.setView(enterName);

        // add ok and cancel buttons
        builder.setPositiveButton(R.string.conf, null);
        builder.setNegativeButton(R.string.cancel, null);

        // create dialog
        final AlertDialog dia = builder.create(); // does not show it yet

        // set listener to input field to click OK when done
        enterName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
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

        dia.show(); // show dialog

        // override onclick for OK button; must be done after show()ing to retrieve OK button
        dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // retrieve name entered
                String catName = enterName.getText().toString().trim();
                User curUser = gc.getCurUser();

                // perform checks and add only if pass
                if (catName.equals("")) { // must not be empty
                    enterName.setError("Please enter a name.");
                } else if (catName.indexOf('"') >= 0) {
                    enterName.setError("Double-quote character not permitted");
                } else if (catSource.exists(catName, curUser)) { // must not exist
                    enterName.setError("This category already exists.");
                } else {
                    // can be changed
                    catToEdi.setName(catName); // change name in object
                    new EditCategory().execute(catToEdi);
                    dia.dismiss();
                }
            }
        });
    }

    /**
     * Method to delete a category, called when the Delete button in the context menu is clicked.
     */
    private void deleteCategory() {
        // show dialog confirming deletion
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete category");
        builder.setMessage("Are you sure? All expenses for this category will be deleted.");

        // add ok and cancel buttons
        builder.setPositiveButton(R.string.conf, null);
        builder.setNegativeButton(R.string.cancel, null);

        // create dialog
        final AlertDialog dia = builder.create();
        dia.show(); // show dialog

        // get list view and list adapter
        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        final ArrayAdapter<Category> aa = (ArrayAdapter<Category>) getListAdapter();
        final Category catToDel = aa.getItem(lv.getCheckedItemPosition()); // get item at checked pos

        // override onclick for OK button; must be done after show()ing to retrieve OK button
        dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // delete category from db
                new DeleteCategory().execute(catToDel);
                dia.dismiss(); // close dialog
            }
        });
    }

//    private void showDatePicker() {
//        // note: DateSelector is a class in this file
//        new DateSelector().show(getFragmentManager(), "configDatePicker");
//    }

//    private void updateTitle() {
//        Calendar curDate = gc.getCurDate();
//        String sTitle = MONTHS[curDate.get(Calendar.MONTH)] + " " + curDate.get(Calendar.YEAR);
//        acTitle.setText(sTitle);
//    }

//    private void updateGrandTotal() {
//        User curUser = gc.getCurUser();
//        Calendar curDate = gc.getCurDate();
//        int month = curDate.get(Calendar.MONTH);
//        int year = curDate.get(Calendar.YEAR);
//        BigDecimal nTot = exSource.getTotalCost(curUser, month, year);
//        String sTot = "Total: " + NumberFormat.getCurrencyInstance().format(nTot);
//        tvTotal.setText(sTot);
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_categories);

        // add "<-" icon to action bar
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setHomeAsUpIndicator(R.drawable.ic_back_dark);
            bar.setDisplayHomeAsUpEnabled(true);
        }

        // non-persistent shared config
        gc = (GlobalConfig) getApplication();
//        curUser = settings.getCurUser();
//        date = gc.getDate();

        // grand total
//        tvTotal = (TextView) findViewById(R.id.monYTot);

        // set month selector listener
//        acTitle = (TextView) findViewById(R.id.catMon);
//        acTitle.setText(MONTHS[date.get(Calendar.MONTH)] + " " + date.get(Calendar.YEAR));
//        updateTitle();
//        acTitle.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showDatePicker();
//            }
//        });

        // open data sources
        // expense source used only for getting total cost
//        exSource = new ExpenseDao(this);
//        exSource.open();
        // for adding and deleting categories
        catSource = new CategoryDao(this);
        catSource.open();

//        // calculate and display month/year total for this user for all categories
//        TextView total = (TextView) findViewById(R.id.monYTot);
//        total.setText("Total: " + NumberFormat.getCurrencyInstance().format(
//                exSource.getTotalCost(curUser, date.get(Calendar.MONTH), date.get(Calendar.YEAR))));
//        updateGrandTotal();

        // retrieve categories asynchronously
        // doesn't work when getting adapter in onResume method
        // new GetCategories().execute();

//        populateCategories(); // retrieve categories and populate view
    }

    @Override
    protected void onResume() {
        super.onResume();
        catSource.open();
//        exSource.open();
        populateCategories(); // retrieve categories and populate view

//        @SuppressWarnings("unchecked")
//        ArrayAdapter<Category> aa = ((ArrayAdapter<Category>) getListAdapter());
//        aa.notifyDataSetChanged();

//        // calculate and display month/year total for this user for all categories
//        TextView total = (TextView) findViewById(R.id.monYTot);
//        total.setText("Total: " + NumberFormat.getCurrencyInstance().format(
//                exSource.getTotalCost(curUser, date.get(Calendar.MONTH), date.get(Calendar.YEAR))));
//        updateGrandTotal();
    }

    @Override
    protected void onPause() {
        catSource.close();
//        exSource.close();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.view_categories, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new_cat) {
            addCategory();
            return true;
//        } else if (id == R.id.action_pick_date) {
//            showDatePicker();
//            return true;
//        } else if (id == R.id.action_pick_user) {
//            Intent intent = new Intent(this, ViewUsers.class);
//            startActivity(intent); // start user activity
//            return true;
        } else if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
