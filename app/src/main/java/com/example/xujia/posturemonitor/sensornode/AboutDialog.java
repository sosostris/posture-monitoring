package com.example.xujia.posturemonitor.sensornode;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.example.xujia.posturemonitor.R;

/**
 * Created by xujia on 2018-02-20.
 */

public class AboutDialog extends Dialog {
    // Log
    // private static final String TAG = "AboutDialog";

    private Context mContext;
    private static AboutDialog mDialog;
    private static OkListener mOkListener;

    public AboutDialog(Context context) {
        super(context);
        mContext = context;
        mDialog = this;
        mOkListener = new OkListener();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_about);

        // Header
        Resources res = mContext.getResources();
        String appName = res.getString(R.string.app_name);
        TextView title = (TextView) findViewById(R.id.title);
        title.setText("JiajiaGiraffe " + appName);

        // Application info
        TextView head = (TextView) findViewById(R.id.header);
        String appVersion = "Revision: ";
        try {
            appVersion += mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Log.v(TAG, e.getMessage());
        }
        head.setText(appVersion);

        // Dismiss button
        Button okButton = (Button) findViewById(R.id.buttonOK);
        okButton.setOnClickListener(mOkListener);

        // Device information
        TextView foot = (TextView) findViewById(R.id.footer);
        String txt = Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL + " Android " + Build.VERSION.RELEASE + " " + Build.DISPLAY;

        foot.setText(txt);
    }

    private class OkListener implements android.view.View.OnClickListener {
        @Override
        public void onClick(View v) {
            mDialog.dismiss();
        }
    }
}
