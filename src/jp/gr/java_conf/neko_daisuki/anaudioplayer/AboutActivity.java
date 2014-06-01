package jp.gr.java_conf.neko_daisuki.anaudioplayer;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import jp.gr.java_conf.neko_daisuki.android.util.ActivityUtil;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        showVersion();
    }

    private void showVersion() {
        PackageInfo pi;
        try {
            pi = ActivityUtil.getPackageInfo(this);
        }
        catch (PackageManager.NameNotFoundException e) {
            String msg = "Cannot fetch the package information";
            ActivityUtil.showException(this, msg, e);
            return;
        }

        TextView view = (TextView)findViewById(R.id.version);
        view.setText(pi.versionName);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
