package jp.gr.java_conf.neko_daisuki.android.util;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

public class ActivityUtil {

    private static final String TAG = "activity";

    public static void showException(Activity activity,
                                     String msg,
                                     Throwable e) {
        e.printStackTrace();

        String s = String.format("%s: %s", msg, e.getMessage());
        showToast(activity, s);
        Log.e(TAG, s);
    }

    public static PackageInfo getPackageInfo(Activity activity) throws PackageManager.NameNotFoundException {
        PackageManager pm = activity.getPackageManager();
        String name = activity.getPackageName();
        return pm.getPackageInfo(name, PackageManager.GET_INSTRUMENTATION);
    }

    public static void showToast(Activity activity, String msg) {
        PackageInfo pi;
        try {
            pi = getPackageInfo(activity);
        }
        catch (PackageManager.NameNotFoundException e) {
            showException(activity, "Cannot fetch the package information", e);
            return;
        }

        int resId = pi.applicationInfo.labelRes;
        String name = activity.getResources().getString(resId);
        String s = String.format("%s: %s", name, msg);
        Toast.makeText(activity, s, Toast.LENGTH_LONG).show();
    }
}