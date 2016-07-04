package name.mjs001.expensereport;

import android.app.Activity;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * routine to export data to the sdcard, which user can obtain thru USB.
 */
public class DataExport implements Runnable {
    private final Activity act;

    public DataExport(Activity act) {
        this.act = act;
    }

    public void run() {
        writeFile();
    }

    /**
     * Write a CSV file named "expense/expense.csv" in UTF-8 format.
     *
     * @return true on success, false on error
     */
    public boolean writeFile() {

        /* Checks if external storage is available for read and write */
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) == false) {
            // user may have folder open over USB
            showMessage("Can't access sdcard right now");
            return false;
        }

        File dir = Environment.getExternalStorageDirectory();
        dir = new File(dir, "expense");
        if (dir.isDirectory() == false && dir.mkdirs() == false) {
            Log.e(GlobalConfig.LOG_TAG, "Can't create directory: " + dir.getPath());
            showMessage("Can't create directory 'expense'");
            return false;
        }
        File file = new File(dir, "expense.csv");
        BufferedWriter writer = null;
        int row_cnt = 0;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), 4096);
            row_cnt = dumpData(writer);
        } catch(Exception e) {
            Log.e(GlobalConfig.LOG_TAG, e.getMessage());
            showMessage("Data export failed.");
            return false;
        } finally {
            if (writer != null) {
                try { writer.close(); } catch(Exception ignored) { }
            }
        }
        String msg = String.format("Exported %d records", row_cnt);
        showMessage(msg);
        return true;
    }

    private void showMessage(String msg) {
        act.runOnUiThread(new MsgWrapper(msg));
    }

    private class MsgWrapper implements Runnable {
        private final String msg;
        public MsgWrapper(String msg) {
            this.msg = msg;
        }
        public void run() {
            Toast.makeText(act, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private int dumpData(BufferedWriter wr) {
        ExpenseDao db = new ExpenseDao(act);
        int row_cnt = 0;
        Cursor cursor = null;
        int row_id, user_id, cost;
        String str_vdate, cat_name, str_description;
        try {
            db.openReadonly();
            cursor = db.getCursorExpensesAllRows();
            // columns:  _id,user_id,vdate,cat_name,description,cost
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                row_id = cursor.getInt(0);
                user_id = cursor.getInt(1);
                str_vdate = cursor.getString(2);
                cat_name = cursor.getString(3);
                str_description = cursor.getString(4);
                cost = cursor.getInt(5);

                String s = String.format("%d,%s,%d,\"%s\",\"%s\",%d\n",
                        user_id, str_vdate, row_id, cat_name, str_description, cost);
                wr.append(s);
                row_cnt++;
                cursor.moveToNext();
            }
        } catch(Exception e) {
            Log.e(GlobalConfig.LOG_TAG, e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return row_cnt;
    }
}
