package name.mjs001.expensereport;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold shared global app settings while the app exists in RAM
 */
public class GlobalConfig extends Application {
    public static final String LOG_TAG = "Expenses";
    public static final String PREF_NAME = "com.mjs001.expensereport.prefs";
    public static final String PREF_KEY_USERID = "userId";
    public static final String PREF_KEY_CATID_VIEW = "catId";
    public static final String PREF_KEY_CATID_ADD = "catAddId";
    public static final String PREF_KEY_RECENT = "curView";
    public static final int VIEW_RECENT = 0;
    public static final int VIEW_ALL = 1;

    /** Current app user, null on app first use.*/
    private UserId curUser;

    /** current category on expense list activity */
    private CatId curViewCatId;

    /** initial category for add expense activity */
    private CatId curAddCatId;

    /** view filter for expense list: 0=recent, 1=all */
    private int curViewRecentExp;

    /** cached list of all users */
    private List<User> userList = new ArrayList<User>();

    /**** getters and setters ****/

    /** if returns null, the caller should prompt user to choose from list */
    public User getCurUser() {
        for(int i = 0; i < userList.size(); i++) {
            // curUser may be null
            if (userList.get(i).getId().equals(curUser)) {
                return userList.get(i);
            }
        }
        return null;
    }

    /** load or reload our cache from the database */
    public void refreshUserCache() {
        UserDao uSource = new UserDao(this);
        uSource.open();
        List<User> tmpList = uSource.getAllUsers();
        uSource.close();
        // re-use original userList object for adapters
        userList.clear();
        userList.addAll(tmpList);

        // verify that the curUser still exists in userList (may have just been deleted)
        User user = findUserById(curUser);
        if (user == null) {
            // fix by choosing first entry in list
            setCurUser(userList.get(0).getId());
        }
    }

    public List<User> getUserList() {
        return userList;
    }

    /**
     * Change the current expense user. this setting persists across restarts.
     * Verify the validity of the new userId.
     *
     * @param newUserId  the new id
     */
    public void setCurUser(UserId newUserId) {
        // verify that the new id is valid
        User user = findUserById(newUserId);
        if (user == null) {
            Log.e(LOG_TAG, "setCurUser: invalid userId = " + newUserId.toInt());
            return;
        }
        // persist only if curUser changed
        if (curUser == null || !curUser.equals(newUserId)) {
            curUser = newUserId;
            savePrefUser();
        }
        Log.i(LOG_TAG, "Current user is " + user.getName());
    }

    /**
     * Find the user. null may be passed.
     * @return User or null if not found.
     */
    private User findUserById(UserId userId) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getId().equals(userId)) {
                return userList.get(i);
            }
        }
        return null;
    }

    public void setViewCatId(CatId cat) {
        if (curViewCatId.toInt() != cat.toInt()) {
            curViewCatId = cat;
            savePrefViewCat();
        }
    }

    /** current show category on expense list activity */
    public CatId getViewCatId() {
        return curViewCatId;
    }

//    /** last used category for the expense add activity (s/b per user) */
//    public void setAddCatId(CatId cat) {
//        if (curAddCatId.toInt() != cat.toInt()) {
//            curAddCatId = cat;
//            savePrefAddCat();
//        }
//    }

    /** last used category for expense add activity (s/b per user) */
    public CatId getAddCatId() {
        return curAddCatId;
    }

    /**
     * Filter for the expense list: all items or recent items.
     * @return VIEW_RECENT or VIEW_ALL
     */
    public int getCurView() {
        return curViewRecentExp;
    }

    /**
     * Filter for the expense list: all items or recent items
     * @param v  0=recent 1=all
     */
    public void setCurView(int v) {
        if (curViewRecentExp != v) {
            curViewRecentExp = v;
            savePrefRecent();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        refreshUserCache();
        // assumes that UserCache is already loaded
        loadFromPersistentStorage();
     }

    /** initialize this object from persistent storage */
    private void loadFromPersistentStorage() {
        // this persists across app runs, 'private' means only this app can r/w
        SharedPreferences sharedPref = this.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE);
        // first valid user_id is 1
        int userId = sharedPref.getInt(PREF_KEY_USERID, 0);
        if (userId > 0) {
            setCurUser(new UserId(userId));
        }

        // TODO: confirm is valid for this user
        int cat_id = sharedPref.getInt(PREF_KEY_CATID_VIEW, Category.SHOW_ALL_CAT_ID);
        curViewCatId = new CatId(cat_id);

        // TODO: confirm is valid for this user
        cat_id = sharedPref.getInt(PREF_KEY_CATID_ADD, 0);
        curAddCatId = new CatId(cat_id);

        curViewRecentExp = sharedPref.getInt(PREF_KEY_RECENT, VIEW_RECENT);
    }

    private void savePrefUser() {
        SharedPreferences prefs = this.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_KEY_USERID, curUser.toInt());
        editor.commit();
    }

    /** for filtering the expense list */
    private void savePrefViewCat() {
        SharedPreferences prefs = this.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_KEY_CATID_VIEW, curViewCatId.toInt());
        editor.commit();
    }

    /** add new expense activity */
    private void savePrefAddCat() {
        SharedPreferences prefs = this.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_KEY_CATID_ADD, curAddCatId.toInt());
        editor.commit();
    }

    private void savePrefRecent() {
        SharedPreferences prefs = this.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_KEY_RECENT, curViewRecentExp);
        editor.commit();
    }
}
