package jp.gr.java_conf.neko_daisuki.android.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

public class ContextUtil {

    private static final String TAG = "context_util";

    public static void showException(Context context, String msg, Throwable e) {
        e.printStackTrace();

        String s = String.format("%s: %s", msg, e.getMessage());
        showToast(context, s);
        Log.e(TAG, s);
    }

    public static PackageInfo getPackageInfo(Context context) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        String name = context.getPackageName();
        return pm.getPackageInfo(name, PackageManager.GET_INSTRUMENTATION);
    }

    public static void showToast(Context context, String msg) {
        PackageInfo pi;
        try {
            pi = getPackageInfo(context);
        }
        catch (PackageManager.NameNotFoundException e) {
            showException(context, "Cannot fetch the package information", e);
            return;
        }

        int resId = pi.applicationInfo.labelRes;
        String name = context.getResources().getString(resId);
        String s = String.format("%s: %s", name, msg);
        Toast.makeText(context, s, length).show();
    }
}