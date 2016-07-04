package name.mjs001.expensereport;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Activity to manage the list of users and select the current User.
 */
public class ViewUsers extends Activity {

    // non-persisent shared config
    private GlobalConfig gc;

    /** User data source. */
    private UserDao uSource;

    // index in UserList
    private int longClickIdx = -1;

    /**
     * Draw the radio buttons, one per user, with the current user selected.
     */
    private void populateView() {
        int pad_px = dpToPx(10);
        List<User> list = gc.getUserList();
        UserId curId = gc.getCurUser().getId();

        RadioGroup group = (RadioGroup)findViewById(R.id.radioGroup);
        // clean up existing radio buttons
        for(int i = 0; i < group.getChildCount(); i++) {
            RadioButton b = (RadioButton)group.getChildAt(i);
            b.setOnClickListener(null);
            b.setOnLongClickListener(null);
            b.setLongClickable(false);
        }
        group.removeAllViews();

        // create new radio buttons
        for(int i = 0; i < list.size(); i++) {
            User u = list.get(i);
            RadioButton button = new RadioButton(this);
            button.setPadding(pad_px, pad_px, pad_px, pad_px);
            button.setTextSize(25);   // scaled pixels
            //button.setTextAppearance(this, android.R.style.TextAppearance_Holo_Medium);
            button.setId(i);
            button.setText( u.getName() );

            button.setChecked(curId.equals(u.getId()));

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int idx = v.getId();
                    User u2 = gc.getUserList().get(idx);
                    gc.setCurUser(u2.getId());
                }
            });
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    longClickIdx = v.getId();
                    return false;    /* do NOT consume long click so it bubbles up to RadioGroup View */
                }
            });
            button.setLongClickable(true);

            group.addView(button);
        }

        // prompt user for name if no users exist
        if (list.size() == 0) {
            addUser();
        }
    }

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        @SuppressWarnings("UnnecessaryLocalVariable")
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    /**
     * Class to asynchronously add new user to database.
     */
    private class AddUser extends AsyncTask<String, Void, User> {
        @Override
        protected User doInBackground(String... params) {
            uSource.open();
            // add row to table
            User newU = uSource.newUser(params[0]);
            uSource.close();
            if (newU == null) {
                return null;
            }
            gc.refreshUserCache();

            // create a default category
            // open data source for categories
            CategoryDao cSource = new CategoryDao(ViewUsers.this);
            cSource.open();
            cSource.newCategory("Cash", newU);
            cSource.close();

            return newU;
        }

        @Override
        protected void onPostExecute(User result) {
            if (result != null) {
                populateView();
            } else {
                Toast t = Toast.makeText(ViewUsers.this, "Create failed", Toast.LENGTH_SHORT);
                t.show();
            }
        }
    }

    /**
     * Class to asynchronously edit a user's name in database.
     */
    private class EditUser extends AsyncTask<User, Void, User> {
        @Override
        protected User doInBackground(User... params) {
            // change table
            uSource.open();
            User user = uSource.editUser(params[0]);
            uSource.close();
            gc.refreshUserCache();
            return user;
        }

        @Override
        protected void onPostExecute(User result) {
            populateView();
        }
    }

    /**
     * Class to asynchronously delete a user from database. Attempting to delete the last user
     * will fail.
     */
    private class DeleteUser extends AsyncTask<User, Void, User> {
        private boolean status = false;

        @Override
        protected User doInBackground(User... params) {
            User user = params[0];
            // delete expenses,categories,user in database
            uSource.open();
            status = uSource.deleteUser(user);
            uSource.close();
            if (status) {
                gc.refreshUserCache();
            }
            return user;
        }

        @Override
        protected void onPostExecute(User result) {
            if (status) {
                populateView();
            } else {
                Toast t = Toast.makeText(ViewUsers.this, "Delete failed", Toast.LENGTH_SHORT);
                t.show();
            }
        }
    }

    /**
     * Method to add a new user, called when the Add button the action bar is clicked.
     */
    private void addUser() {
        // build dialog to ask for name of user
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create user");
        builder.setMessage("Please enter your name.");

        // construct input field
        final EditText enterName = new EditText(this);
        enterName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS); // capitalized words
        enterName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
        builder.setView(enterName);

        // add ok and cancel buttons
        builder.setPositiveButton(R.string.ok, null);
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
                String username = enterName.getText().toString().trim();

                // perform checks and add only if pass
                if (username.equals("")) { // must not be empty
                    enterName.setError("Please enter a name.");
                } else if (username.indexOf('"') >= 0) {
                    enterName.setError("Double-quote character not permitted");
                } else if (userNameExists(username)) { // must not exist
                    enterName.setError("This user already exists.");
                } else {
                    // can be added
                    new AddUser().execute(username);
                    dia.dismiss();
                }
            }
        });
    }

    /**
     * Method to edit a user's name, called when the Edit button in the context menu is clicked.
     */
    private void editUser(final User userToEdi) {
        // show dialog to enter new name
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit user");
        builder.setMessage("Please enter a new name.");

        // construct input field
        final EditText enterName = new EditText(this);
        enterName.setText(userToEdi.getName()); // prepopulate with user's current name
        enterName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS); // capitalized words
        enterName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
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
                String username = enterName.getText().toString().trim();

                // perform checks and add only if pass
                if (username.equals("")) { // must not be empty
                    enterName.setError("Please enter a name.");
                } else if (username.indexOf('"') >= 0) {
                    enterName.setError("Double-quote character not permitted");
                } else if (userNameExists(username)) { // must not exist
                    enterName.setError("This user already exists.");
                } else {
                    // can be changed
                    userToEdi.setName(username);
                    new EditUser().execute(userToEdi); // change name and add it back
                    dia.dismiss();
                }
            }
        });
    }

    /** @return true if name already exists */
    private boolean userNameExists(String name) {
        List<User> users = gc.getUserList();
        for(int i = 0; i < users.size(); i++) {
            if (name.equals(users.get(i).getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method to delete a user, called when the Delete button in the context menu is clicked.
     */
    private void deleteUser(final User userToDel) {
        // show dialog confirming deletion
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete user");
        builder.setMessage("Are you sure? All expenses for this user will be deleted.");

        // add ok and cancel buttons
        builder.setPositiveButton(R.string.conf, null);
        builder.setNegativeButton(R.string.cancel, null);

        // create dialog
        final AlertDialog dia = builder.create(); // does not show it yet

        dia.show(); // show dialog

        // override onclick for OK button; must be done after show()ing to retrieve OK button
        dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // delete user from db. Will fail if last user.
                new DeleteUser().execute(userToDel);
                dia.dismiss(); // close dialog
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_users);

        // add "<-" icon to action bar
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setHomeAsUpIndicator(R.drawable.ic_back_dark);
            bar.setDisplayHomeAsUpEnabled(true);
        }

        // long-press will invoke context menu
        RadioGroup grpView = (RadioGroup)findViewById(R.id.radioGroup);
        registerForContextMenu(grpView);

        gc = (GlobalConfig)getApplication();

        // object used to access data source
        uSource = new UserDao(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateView();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu_view_users, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            addUser();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** called when the user long-presses a row. v is the RadioGroup View. menuInfo is null. */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.menu_exp_title);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_expenses, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // position in UserList
        int userIdx = longClickIdx;
        longClickIdx = -1;
        List<User> users = gc.getUserList();
        if (userIdx >= 0 && userIdx < users.size()) {
            User sel = users.get(userIdx);
            // examine menu item id
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                editUser(sel);
                return true;
            } else if (id == R.id.action_del) {
                deleteUser(sel);
                return true;
            }
        }
        // not handled by us
        return super.onContextItemSelected(item);
    }
}
