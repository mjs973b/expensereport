package name.mjs001.expensereport;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
 * Activity to display the list of users.
 */
public class ViewUsers extends ListActivity {

    // non-persisent shared config
    private GlobalConfig gc;

    /** User data source. */
    private UserDao uSource;

    /** Action mode for the context menu. */
    private ActionMode aMode;

    /** Call back methods for the context menu. */
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        /** To temporarily store listener when removed. */
        private AdapterView.OnItemClickListener lstn;

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_users, menu);

            // disable listeners here
            ListView lv = getListView();
            lstn = lv.getOnItemClickListener();
            lv.setOnItemClickListener(null);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // disable other listeners temporarily to prevent multiple actions
            // disable on item click which would start expenses activity
//            ListView lv = getListView();
//            lstn = lv.getOnItemClickListener();
//            lv.setOnItemClickListener(null);
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_edit:
                    // edit a user's name
                    editUser();
                    mode.finish(); // close the CAB
                    return true;
                case R.id.action_del:
                    // delete selected user
                    deleteUser();
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
            // calling from new thread as a workaround
            lv.post(new Runnable() {
                @Override
                public void run() {
                    lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
                }
            });
            aMode = null;

            // restore listeners
            getListView().setOnItemClickListener(lstn);
        }
    };

    /**
     * Class to asynchronously retrieve users from database.
     */
    private void populateListView() {
        List<User> list = gc.getUserList();
        final ArrayAdapter<User> adapter = new ArrayAdapter<>(ViewUsers.this,
                android.R.layout.simple_list_item_activated_1, list);

        setListAdapter(adapter);

        final ListView lv = getListView();
        // set item onclick listener to each item in list, to launch categories activity
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // retrieve selected user
                User us = adapter.getItem(i);

                // remember selected user in config
                gc.setCurUser(us);

                // start ViewExpensesByTime activity
                Intent intent = new Intent(ViewUsers.this, ViewExpensesByTime.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        // set long click listener, to display CAB
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
                aMode = ViewUsers.this.startActionMode(mActionModeCallback);
                return true;
            }
        });

        // prompt user for name if no users exist
        if (list.size() == 0) {
            addUser();
        }
    }

    /**
     * Class to asynchronously add new user to database.
     */
    private class AddUser extends AsyncTask<String, Void, User> {
        @Override
        protected User doInBackground(String... params) {
            // add row to table
            User newU = uSource.newUser(params[0]);
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
                // get adapter
                @SuppressWarnings("unchecked")
                ArrayAdapter<User> adapter = (ArrayAdapter<User>) getListAdapter();
                adapter.notifyDataSetChanged();

                // on first user create, click the item
                List<User> users = gc.getUserList();
                for (int i = 0; i < users.size(); i++) {
                    if (result.getId().equals(users.get(i).getId())) {
                        // click on it
                        getListView().performItemClick(null, i, adapter.getItemId(i));
                        break;
                    }
                }
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
            User user = uSource.editUser(params[0]);
            gc.refreshUserCache();
            return user;
        }

        @Override
        protected void onPostExecute(User result) {
            // refresh view
            @SuppressWarnings("unchecked")
            ArrayAdapter<User> aa = (ArrayAdapter<User>) getListAdapter();
            aa.notifyDataSetChanged();
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
            status = uSource.deleteUser(user);
            if (status) {
                gc.refreshUserCache();
            }
            return user;
        }

        @Override
        protected void onPostExecute(User result) {
            if (status) {
                @SuppressWarnings("unchecked")
                ArrayAdapter<User> aa = (ArrayAdapter<User>) getListAdapter();
                aa.notifyDataSetChanged(); // update view
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
                } else if (uSource.exists(username)) { // must not exist
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
    private void editUser() {
        // retrieve adapter and retrieve selected user
        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        final ArrayAdapter<User> aa = (ArrayAdapter<User>) getListAdapter();
        final User userToEdi = aa.getItem(lv.getCheckedItemPosition()); // get item at checked pos

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
                } else if (uSource.exists(username)) { // must not exist
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

    /**
     * Method to delete a user, called when the Delete button in the context menu is clicked.
     */
    private void deleteUser() {
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

        // get list view and list adapter
        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        final ArrayAdapter<User> aa = (ArrayAdapter<User>) getListAdapter();
        final User userToDel = aa.getItem(lv.getCheckedItemPosition()); // get item at checked pos

        // override onclick for OK button; must be done after show()ing to retrieve OK button
        dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DeleteUser().execute(userToDel); // delete user from db
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

//        // set selected user in config
//        GlobalConfig gc = (GlobalConfig) getApplication();
//        if (gc.getCurUser() != null) {
//            new Handler().post(new Runnable() {
//                @Override
//                public void run() {
//                    // start ViewCategories activity
//                    Intent intent = new Intent(ViewUsers.this, ViewExpensesByTime.class);
//                    startActivity(intent);
//                }
//            });
//        }

        gc = (GlobalConfig)getApplication();

        // open data source
        uSource = new UserDao(this);
//        uSource.open();
//        // retrieve users asynchronously
//        new GetUsers().execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        uSource.open();
        populateListView();
    }

    @Override
    protected void onPause() {
        uSource.close();
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

}
