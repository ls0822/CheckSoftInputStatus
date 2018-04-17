package com.ss.checkinput;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.EditText;

import java.util.HashMap;
import java.util.List;

/**
 * Created by song on 2018/4/17.
 */

public class CheckSoftInputStatusManager {

    private static CheckSoftInputStatusManager mInstance;

    /**
     * 获取CCHelperManager单例对象
     *
     * @return 返回单例对象
     */
    public static CheckSoftInputStatusManager getInstance() {
        synchronized (CheckSoftInputStatusManager.class) {
            if (mInstance == null) {
                mInstance = new CheckSoftInputStatusManager();
            }

            return mInstance;
        }
    }

    private CheckSoftInputStatusManager() {
    }



    public interface OnAppStatusListener {

        public void onSoftKeyboardStatus(boolean isShow);
    }

    private Context mContext;
    private boolean initialized = false;

    private static final String TAG = "CheckSoftInput";
    private boolean isOpenKeyboard = false;
    private Views view = new Views();
    private HashMap<String, GlobalLayoutListener> mGLLIsteners;

    private OnAppStatusListener mAppStatusListener;


    public void init(Context mContext) {
        if (!initialized) {
            this.mContext = mContext;
            ((Application) mContext).registerActivityLifecycleCallbacks(mALCallbacks);
            initialized = true;
        }
    }

    public void release() {
        if (initialized) {
            ((Application) mContext).unregisterActivityLifecycleCallbacks(mALCallbacks);
            if (mGLLIsteners != null && !mGLLIsteners.isEmpty()) {
                mGLLIsteners.clear();
                mGLLIsteners = null;
            }
            isOpenKeyboard = false;
            initialized = false;
            mAppStatusListener = null;
            this.mContext = null;
        }
    }


    public void setOnAppStatusListener(OnAppStatusListener mAppStatusListener) {
        this.mAppStatusListener = mAppStatusListener;
    }

    public boolean isOpenSoftKeyboard() {
        return isOpenKeyboard;
    }

    private void Logd(String msg){
        Log.d(TAG, "appStatusManager-"+msg);
    }

    /**
     * 该接口只能在Android4.0以上的版本中才能够使用，因此该SDK暂时只支持Android4.0（包括）以上的版本。
     * 该接口主要用来监控Activity的生命周期。 创建该接口实例，并实现该接口。
     */
    private Application.ActivityLifecycleCallbacks mALCallbacks = new Application.ActivityLifecycleCallbacks() {

        @Override
        public void onActivityStopped(Activity activity) {
            Logd("onActivityStop===>>" + activity.getLocalClassName());

            if (mGLLIsteners != null) {
                GlobalLayoutListener mLayoutChangeListener = mGLLIsteners.get(activity.getLocalClassName());
                if (mLayoutChangeListener != null) {
                    if (mLayoutChangeListener.getImmStatus()) {
                        sendSoftInputStatus(false);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        activity.getWindow().getDecorView().getViewTreeObserver()
                                .removeOnGlobalLayoutListener(mLayoutChangeListener);
                    } else {
                        activity.getWindow().getDecorView().getViewTreeObserver()
                                .removeGlobalOnLayoutListener(mLayoutChangeListener);
                    }
                    mGLLIsteners.remove(activity.getLocalClassName());
                }
            }
        }

        @Override
        public void onActivityStarted(final Activity activity) {
            Logd("onActivityStarted====>>"+activity.getLocalClassName());
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            Logd("onActivityResumed===>>" + activity.getLocalClassName());
            // 添加输入框是否弹出的监听
            addCallback(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            Logd("onActivityPaused===>>" + activity.getLocalClassName());

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            Logd( "onActivityDestroyed===>>" + activity.getLocalClassName());

        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }
    };


    private void addCallback(Activity activity) {
        // Log.d("dxt", "addCallback");
        Window w = activity.getWindow();
        if (w == null) {
            return;
        }
        try {
            // flagHashcode = -1;
            View decor = w.getDecorView();
            ViewGroup root = (ViewGroup) decor.getRootView();
            List<EditText> list = view.find(root, EditText.class);
            int size = list.size();
            if (size > 0) {
                if (mGLLIsteners == null) {
                    mGLLIsteners = new HashMap<String, GlobalLayoutListener>();
                }
                GlobalLayoutListener mLayoutChangeListener = mGLLIsteners.get(activity.getLocalClassName());
                if (mLayoutChangeListener == null) {
                    mLayoutChangeListener = new GlobalLayoutListener();
                    mLayoutChangeListener.setWindow(activity.getWindow());
                    mGLLIsteners.put(activity.getLocalClassName(), mLayoutChangeListener);
                }
                // 先移除监听，避免重复添加两次
                if (mLayoutChangeListener != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        activity.getWindow().getDecorView().getViewTreeObserver()
                                .removeOnGlobalLayoutListener(mLayoutChangeListener);
                    } else {
                        activity.getWindow().getDecorView().getViewTreeObserver()
                                .removeGlobalOnLayoutListener(mLayoutChangeListener);
                    }
                }
                activity.getWindow().getDecorView().getViewTreeObserver()
                        .addOnGlobalLayoutListener(mLayoutChangeListener);
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

    }



    // 监控窗口布局的变化
    class GlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        private Window mWindow;
        private boolean isOpen = false;

        public void setWindow(Window mWindow) {
            this.mWindow = mWindow;
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onGlobalLayout() {
            // 判断窗口可见区域大小
            Rect r = new Rect();
            mWindow.getDecorView().getWindowVisibleDisplayFrame(r);
            int screenHeight = mWindow.getWindowManager().getDefaultDisplay().getHeight();
            // 如果屏幕高度和Window可见区域高度差值大于整个屏幕高度的1/6，则表示软键盘显示中，否则软键盘为隐藏状态。
            int heightDifference = screenHeight - (r.bottom - r.top);
            isOpenKeyboard = heightDifference > screenHeight / 6;
            if (Boolean.compare(isOpenKeyboard, isOpen) != 0) {
                sendSoftInputStatus(isOpenKeyboard);
            }
            isOpen = isOpenKeyboard;
        }

        public boolean getImmStatus() {
            return isOpen;
        }
    };

    /**
     * 发送当前输入框状态
     *
     * @param status
     *            true 弹起 ；false 关闭
     */
    private void sendSoftInputStatus(boolean status) {
        // CCHelperController.getInstance().sendSoftInputStatus(status);
        if (mAppStatusListener != null) {
            mAppStatusListener.onSoftKeyboardStatus(status);
        }
    }
}
