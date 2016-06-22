package name.mjs001.expensereport;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Collect the data to create a new Expense Record or edit an existing one.
 * Return it to the caller.
 */
public class EditExp extends Activity implements DatePickerDialog.OnDateSetListener {
    public static final String ACTION_ADD = "actionAdd";
    public static final String ACTION_EDIT = "actionEdit";

//    private static Calendar selectedDate;

    private Spinner tvUser;
    private Spinner tvCat;
    private TextView tvDate;
    private AutoCompleteTextView tvCost;
    private AutoCompleteTextView tvDesc;
    //private Button button;

    private boolean isValid = false;
    private Expense curExp;
    private List<User> userList = new ArrayList<User>();
    private List<Category> catList = new ArrayList<Category>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_exp);

        Intent intent = getIntent();
        if (intent == null) {
            tvDate.setText("Error: intent is null");
            return;
        }
        // create or edit?
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            tvDate.setText("Error: intent.getExtras() is null");
            return;
        }

        curExp = new Expense(bundle);

        tvDate = (TextView) findViewById(R.id.editDate);
        tvDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDatePicker();
            }
        });
        tvDate.setClickable(true);
        tvDate.setText(curExp.getDate());

        tvCost = (AutoCompleteTextView) findViewById(R.id.editCost);

        tvDesc = (AutoCompleteTextView) findViewById(R.id.editDesc);
        populateAutoCompleteAdapter(curExp.getCategoryId());

        tvCat = (Spinner) findViewById(R.id.editCat);
        UserId userId = curExp.getUserId();
        populateCatsList(userId);
        ArrayAdapter<Category> catAdapter = new ArrayAdapter<Category>(this,android.R.layout.simple_spinner_item, catList);
        // Specify the layout to use when the list of choices appears
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tvCat.setAdapter(catAdapter);
        //tvCat.setSelection( findCatListIndex( curExp.getCategoryId() ));
        tvCat.setOnItemSelectedListener(new CategoryListener());

        tvUser = (Spinner) findViewById(R.id.editUser);
        populateUserList();
        ArrayAdapter<User> userAdapter = new ArrayAdapter<User>(this,android.R.layout.simple_spinner_item, userList);
        // Specify the layout to use when the list of choices appears
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tvUser.setAdapter(userAdapter);
        tvUser.setOnItemSelectedListener(new UserListener());

        // this may trigger a re-load of tvCat (if selection changes)
        int pos = findUserListIndex(userId);
        if (userList.size() > 0 && pos >= 0) {
            tvUser.setSelection(pos);
        }
        // this may trigger a re-load of descriptions (if selection changes)
        pos = findCatListIndex( curExp.getCategoryId() );
        if (catList.size() > 0 && pos >= 0) {
            tvCat.setSelection(pos);
        }

        String action = intent.getAction();
        if (action.equals(ACTION_ADD)) {
            setTitle(R.string.title_activity_editexp_add);     // "Add Expense"
        } else {
            // title default "Edit Expense" configured in manifest
            // get cost as #.##
            String sCost = curExp.getCostAsString();
            // if exact dollar value, drop the cents part for editing
            if (sCost.endsWith(".00")) {
                sCost = sCost.substring(0, sCost.length()-3);
            }
            tvCost.append( sCost );
            tvDesc.append( curExp.getDescription() );
        }

        Button button = (Button) findViewById(R.id.butOK);
        button.setOnClickListener(new MyClickListener());
    }


    /** called by OK button to stop this activity, or when Back is pressed */
    @Override
    public void finish() {
        if (isValid) {
            // return modifed data to caller

            // avoid corrupting userId if the Spinner did not load
            if (userList.size() > 0) {
                User user = userList.get(tvUser.getSelectedItemPosition());
                curExp.setUserId(user.getId());
            }
            // avoid corrupting category if Spinner did not load
            if (catList.size() > 0) {
                Category cat = catList.get(tvCat.getSelectedItemPosition());
                curExp.setCategoryId( cat.getId() );
            }
            String sDate = tvDate.getText().toString().trim();
            String sCost = tvCost.getText().toString().trim();
            String sDesc = tvDesc.getText().toString().trim();

            // update Expense object
            curExp.setDate(sDate);
            curExp.setCost( new BigDecimal(sCost) );
            curExp.setDescription(sDesc);

            // Prepare intent to send back to caller
            Bundle bundle = new Bundle();
            curExp.copyToBundle(bundle);
            Intent intent = new Intent();
            intent.putExtras(bundle);

            // exit_code and data to return
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED);
        }

        super.finish();
    }

    /** find matching entry in userList */
    private int findUserListIndex(UserId userId) {
        for(int i = 0; i < userList.size(); i++) {
            if (userId.equals(userList.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    /** find matching entry in catList */
    private int findCatListIndex(CatId catId) {
        for(int i = 0; i < catList.size(); i++) {
            if (catId.equals(catList.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private void populateAutoCompleteAdapter(CatId cat_id) {
        /* cat_id are globally unqiue, so user_id not needed */
        Log.i(GlobalConfig.LOG_TAG, "In populateAutoCompleteAdapter()");
        ExpenseDao dbase;
        try {
            // fetch a list of strings from database
            dbase = new ExpenseDao(this);
            dbase.openReadonly();
            try {
                List<String> list = dbase.getSortedDescValues(cat_id);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_dropdown_item_1line,
                        list);
                tvDesc.setAdapter(adapter);
                tvDesc.setOnItemClickListener(new DescListener());
            } catch(Exception e1) {
                Toast.makeText(this, "Db DESC read failed", Toast.LENGTH_SHORT).show();
            } finally {
                dbase.close();
            }
        } catch(Exception e2) {
            Toast.makeText(this, "Db DESC open failed", Toast.LENGTH_SHORT).show();
        }
    }

    /** populate the userList */
    private List<User> populateUserList() {
        List<User> list = null;
        UserDao dbase;
        try {
            // fetch a list of strings from database
            dbase = new UserDao(this);
            dbase.openReadonly();
            try {
                list = dbase.getAllUsers();
            } catch(Exception e1) {
                Toast.makeText(this, "Db Users read failed", Toast.LENGTH_SHORT).show();
            } finally {
                dbase.close();
            }
        } catch(Exception e2) {
            Toast.makeText(this, "Db Users open failed", Toast.LENGTH_SHORT).show();
        }
        userList.clear();
        if (list != null) {
            userList.addAll(list);
        }
        return userList;
    }

    /** populate the catList. Don't replace list object itself because of adapter. */
    private List<Category> populateCatsList(UserId userId) {
        Log.i(GlobalConfig.LOG_TAG, "In populateCatsList()");
        List<Category> list = null;
        CategoryDao dbase;
        try {
            // fetch a list of strings from database
            dbase = new CategoryDao(this);
            dbase.openReadonly();
            try {
                list = dbase.getCategories(userId);
            } catch(Exception e1) {
                Toast.makeText(this, "Db Cats read failed", Toast.LENGTH_SHORT).show();
            } finally {
                dbase.close();
            }
        } catch(Exception e2) {
            Toast.makeText(this, "Db Cats open failed", Toast.LENGTH_SHORT).show();
        }
        catList.clear();
        if (list != null) {
            catList.addAll(list);
        }
        return catList;
    }

    /** if user is changed, we need to reload the category spinner */
    private class UserListener implements AdapterView.OnItemSelectedListener {
        public void onNothingSelected(AdapterView<?> parent) {
            // Another interface callback
        }
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            Category prevCat = (Category)tvCat.getSelectedItem();
            User user = userList.get(pos);
            populateCatsList(user.getId());
            ArrayAdapter adapter = (ArrayAdapter)tvCat.getAdapter();
            adapter.notifyDataSetChanged();
            // try to select cat with same name else leave selection at position 0
            String prevCatName = prevCat.getName();
            for(int i = 0; i < catList.size(); i++) {
                if (prevCatName.equals(catList.get(i).getName())) {
                    tvCat.setSelection(i);
                    break;
                }
            }
        }
    }

    /** if new category is selected, we need to reload the auto-complete list */
    private class CategoryListener implements AdapterView.OnItemSelectedListener {
        public void onNothingSelected(AdapterView<?> parent) {
            // Another interface callback
        }
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            Category cat = catList.get(pos);
            populateAutoCompleteAdapter(cat.getId());
        }
    }

    /** if description auto-complete item is selected, we want to populate tvCost */
    private class DescListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
            // this user may not be same as current ViewExpenses user
            User user = (User)tvUser.getSelectedItem();
            ArrayAdapter<String> adapter = (ArrayAdapter)parent.getAdapter();
            String sDesc = adapter.getItem(pos);
            populateCost(user.getId(), sDesc);
        }
    }

    /** populate the tvCost field, but only if it is empty. */
    private void populateCost(UserId userId, String sDesc) {
        if (sDesc.length() == 0 || tvCost.getText().length() > 0) {
            // don't overwrite if user has something there already
            return;
        }

        ExpenseDao dbase;
        int cents = 0;
        try {
            // fetch a list of strings from database
            dbase = new ExpenseDao(this);
            dbase.openReadonly();
            try {
                 cents = dbase.getLastCreatedCostByDesc(userId, sDesc);
            } catch(Exception e1) {
                Toast.makeText(this, "expense db read failed", Toast.LENGTH_SHORT).show();
            } finally {
                dbase.close();
            }
        } catch(Exception e2) {
            Toast.makeText(this, "expense db open failed", Toast.LENGTH_SHORT).show();
        }
        // format as dollars #.## without dollar symbol
        String sCost;
        if ((cents % 100) == 0) {
            sCost = Integer.toString(cents / 100);
        } else {
            sCost = new BigDecimal(cents).movePointLeft(2).toString();
        }
        // we know widget is empty
        tvCost.append(sCost);       // leave cursor at end
    }

    /** called when OK button is clicked */
    private class MyClickListener implements View.OnClickListener {

        public void onClick(View v) {
            // perform checks and add if pass
            String cost = tvCost.getText().toString().trim();
            if (cost.equals("")) { // must not be empty
                tvCost.setError("Please enter a dollar amount.");
            } else if (!Pattern.matches("^(\\d{1,10})?(\\.\\d{0,2})?$", cost)) { // must be $$
                tvCost.setError("Please enter a valid currency amount.");
            } else {
                isValid = true;
                // stop this activity
                finish();
            }
        }
    }

//    // listener for last input field to click OK when "Done" pressed
//    private class MyKeyListener implements TextView.OnEditorActionListener {
//        @Override
//        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
//            if (actionId == EditorInfo.IME_ACTION_DONE) {
//                // click dialog's OK when user presses Done on keyboard
//                button.performClick();
//                return true;
//            }
//            return false;
//        }
//    }

    /** called if user modifies the date. Update both the TextView and the Expense object. */
    public void onDateSet(DatePicker view, int year, int month, int day) {
        String strDate = String.format("%04d-%02d-%02d", year, month+1, day);
        tvDate.setText(strDate);
    }

    /** pop up the calendar to let the user pick a new date */
    private void showDatePicker() {
        // note: DateSelector is a class in this file
        DateSelector dlg = new DateSelector();
        dlg.setArgs(curExp.getDate(), this);
        // call is non-blocking, 2nd arg is just a label we make up
        dlg.show(getFragmentManager(), "configDatePicker");
    }

    /**
     * Static class for the date picker dialog.
     */
    public static class DateSelector extends DialogFragment
             {
        private String date;
        private DatePickerDialog.OnDateSetListener callback;

        public void setArgs(String initialDate, DatePickerDialog.OnDateSetListener callback) {
            this.date = initialDate;
            this.callback = callback;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the date specified in config as the default date in the picker
//            Calendar curDate = selectedDate;
            int year  = Integer.parseInt(date.substring(0,4));
            int month = Integer.parseInt(date.substring(5,7)) - 1;  // zero-based
            int day   = Integer.parseInt(date.substring(8,10));

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), callback, year, month, day);
        }
    } // DateSelector
}
