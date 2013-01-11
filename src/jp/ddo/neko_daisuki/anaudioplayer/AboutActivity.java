package jp.ddo.neko_daisuki.anaudioplayer;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.about);
        this.showVersion();
    }

    private void showVersion() {
        PackageManager pm = this.getPackageManager();
        String name = this.getPackageName();
        int flags = PackageManager.GET_INSTRUMENTATION;

        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(name, flags);
        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }

        TextView view = (TextView)this.findViewById(R.id.version);
        view.setText(pi.versionName);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
