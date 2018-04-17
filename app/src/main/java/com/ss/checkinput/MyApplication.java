package com.ss.checkinput;

import android.app.Application;
import android.widget.Toast;

/**
 * Created by song on 2016/4/14.
 */
public class MyApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        CheckSoftInputStatusManager.getInstance().init(getApplicationContext());
        CheckSoftInputStatusManager.getInstance().setOnAppStatusListener(new CheckSoftInputStatusManager.OnAppStatusListener() {
            @Override
            public void onSoftKeyboardStatus(boolean isShow) {
                Toast.makeText(getApplicationContext(),isShow ? R.string.soft_input_show: R.string.soft_input_close, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        CheckSoftInputStatusManager.getInstance().release();
    }
}
