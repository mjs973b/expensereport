package name.mjs001.expensereport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Launcher activity.
 */
public class Splash extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // handler to start the either users or categories activity
        new Handler().post(new MyRun());
    }

    private class MyRun implements Runnable {
        @Override
        public void run() {
            // if there are 0 or more than one users, start user activity to allow selection
            GlobalConfig gc = (GlobalConfig) getApplication();
            if (gc.getCurUser() == null) {
                Intent it = new Intent(Splash.this, ViewUsers.class);
                startActivity(it);
                overridePendingTransition(0, 0); // no animation
            } else {
                Intent it = new Intent(Splash.this, ViewExpensesByTime.class);
                startActivity(it);
                overridePendingTransition(0, 0); // no animation
            }
            Splash.this.finish();
        }
    }
}
