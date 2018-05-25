/**
 * Xujia Zhou. Copyright (c) 2018-02-25.
 */

package com.example.xujia.posturemonitor.sensornode;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.util.CustomToast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Fragment class for ConfigActivity where user can configure communication servers,
 * get authenticate, add and delete sensor nodes, view and add wearing positison.
 */
public class ConfigView extends Fragment {

    public static ConfigView mInstance;

    // GUI components
    private View view;
    private TextView mLoginText;
    private Button mConfigBtn;
    private Button mConnectBtn;
    private Button mLoginBtn;
    private Button mGetnodesBtn;
    private Button mSnConfigBtn;
    private Button mAddSnBtn;
    private Button mDelSnBtn;
    private Button mViewBodyBtn;
    private Button mAddBodyBtn;
    private Button mGotoMainBtn;
    private EditText mUsername;
    private EditText mPassword;

    // House-keeping
    private ConfigActivity mActivity;

    public ConfigView() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInstance = this;
        mActivity = (ConfigActivity) getActivity();

        view = inflater.inflate(R.layout.fragment_config, container,false);
        mLoginText = (TextView) view.findViewById(R.id.login_status);
        mConfigBtn = (Button) view.findViewById(R.id.config_server_btn);
        mConnectBtn = (Button) view.findViewById(R.id.connect_btn);
        mLoginBtn = (Button) view.findViewById(R.id.login_btn);
        mGetnodesBtn = (Button) view.findViewById(R.id.getnodes_btn);
        mSnConfigBtn = (Button) view.findViewById(R.id.confignodes_btn);
        mAddSnBtn = (Button) view.findViewById(R.id.add_sensornode);
        mDelSnBtn = (Button) view.findViewById(R.id.del_sensornode);
        mViewBodyBtn = (Button) view.findViewById(R.id.view_body);
        mAddBodyBtn = (Button) view.findViewById(R.id.add_body);
        mGotoMainBtn = (Button) view.findViewById(R.id.main_btn);
        mUsername = (EditText) view.findViewById(R.id.username);
        mPassword = (EditText) view.findViewById(R.id.password);

        if (PostureMonitorApplication.USERNAME != null) {
            mLoginText.setText("You are logged in as " + PostureMonitorApplication.USERNAME + " :)");
            showBtn("add");
            showBtn("del");
            showBtn("config");
            showBtn("viewBd");
            showBtn("addBd");
            showBtn("main");
            mActivity.initMySensornodes();
        } else {
            mLoginText.setText("Welcome to Giraffe posture monitoring system!");
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Change the visibility of the Login-related UI components from "gone" to "visible".
     */
    public void showLogin() {
        mLoginBtn.setVisibility(View.VISIBLE);
        mUsername.setVisibility(View.VISIBLE);
        mPassword.setVisibility(View.VISIBLE);
    }

    /**
     * Change the visibility of the Login-related UI components from "visible" to "gone".
     */
    public void hideLogin() {
        mLoginBtn.setVisibility(View.GONE);
        mUsername.setVisibility(View.GONE);
        mPassword.setVisibility(View.GONE);
    }

    /**
     * Hide the "Get sensornodes" button.
     */
    public void hideGetSensornodeBtn() {
        mGetnodesBtn.setVisibility(View.GONE);
    }

    /**
     * Make a button visible.
     * @param btn the button to be shown visible.
     */
    public void showBtn(String btn) {
        if (btn.equals("main")) {
            mGotoMainBtn.setVisibility(View.VISIBLE);
        } else if (btn.equals("config")) {
            mSnConfigBtn.setVisibility(View.VISIBLE);
        } else if (btn.equals("add")) {
            mAddSnBtn.setVisibility(View.VISIBLE);
        } else if (btn.equals("del")) {
            mDelSnBtn.setVisibility(View.VISIBLE);
        } else if (btn.equals("viewBd")) {
            mViewBodyBtn.setVisibility(View.VISIBLE);
        } else if (btn.equals("addBd")) {
            mAddBodyBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Handles user login by communicating with the Java TCP server with the right protocol commands.
     */
    public void login(PrintWriter writer, BufferedReader reader) {
        final String username = mUsername.getText().toString();
        final String password = mPassword.getText().toString();
        writer.println("lg" + " " + username + " " + password);
        Thread worker = new Thread(new Runnable() {
            boolean waitingOK = true;
            boolean waitingBdAll = true;
            String userId;
            @Override
            public void run() {
                while (waitingOK || waitingBdAll) {
                    try {
                        String response = reader.readLine();
                        if (response != null) {
                            if (response.contains("OK")) {
                                String[] parts = response.toString().split(" ");
                                mActivity.mAuthenticated = true;
                                userId = parts[1];
                                waitingOK = false;
                            } else if (response.equals("BAD")){
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToast.middleBottom(mActivity, "Wrong username or password!");
                                    }
                                });
                                return;
                            } else {
                                String[] parts = response.split(" ");
                                if (parts[0].equals("bdAll")) {
                                    PostureMonitorApplication.BODY_LIST_USER = new ArrayList<>();
                                    for (int i=0; i<parts.length-1; i++) {
                                        PostureMonitorApplication.BODY_LIST_USER.add(parts[i+1]);
                                    }
                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            hideLogin();
                                            PostureMonitorApplication.USERNAME = username;
                                            PostureMonitorApplication.USER_ID = userId;
                                            mLoginText.setText("You are logged in as " + username + " :)");
                                            CustomToast.middleBottom(mActivity, "Login successful!");
                                        }
                                    });
                                }
                                waitingBdAll = false;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                getSensornodes(reader);
            }
        });
        worker.start();
    }

    /**
     * The same method as getSensornodes() ConfigActivity.
     */
    private void getSensornodes(BufferedReader reader) {
        boolean waitingForNr = true;
        boolean waitingForId = true;
        boolean waitingForAd = true;
        boolean waitingForBd = true;
        boolean waitingForTy = true;
        while (waitingForNr || waitingForId || waitingForAd || waitingForBd || waitingForTy) {
            try {
                String response = reader.readLine();
                if (response != null) {
                    if (response.equals("BAD")) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                CustomToast.middleBottom(mActivity, "You have no sensornodes!");
                            }
                        });
                    } else {
                        String[] parts = response.split(" ");
                        if (parts[0].equals("nr")) {
                            PostureMonitorApplication.NUMBER_OF_SENSORNODE = Integer.valueOf(parts[1]);
                            waitingForNr = false;
                        } else if (parts[0].equals("id")) {
                            PostureMonitorApplication.DEVICE_NAME_LIST = new String[parts.length - 1];
                            for (int i = 1; i < parts.length; i++) {
                                PostureMonitorApplication.DEVICE_NAME_LIST[i - 1] = parts[i];
                            }
                            waitingForId = false;
                        } else if (parts[0].equals("ad")) {
                            PostureMonitorApplication.DEVICE_ADDRESS_LIST = new String[parts.length - 1];
                            for (int i = 1; i < parts.length; i++) {
                                PostureMonitorApplication.DEVICE_ADDRESS_LIST[i - 1] = parts[i];
                            }
                            waitingForAd = false;
                        } else if (parts[0].equals("bd")) {
                            PostureMonitorApplication.SN_BODY_LIST = new String[parts.length - 1];
                            for (int i = 1; i < parts.length; i++) {
                                PostureMonitorApplication.SN_BODY_LIST[i - 1] = parts[i];
                            }
                            waitingForBd = false;
                        } else if (parts[0].equals("ty")) {
                            PostureMonitorApplication.DEVICE_TYPE_LIST = new String[parts.length - 1];
                            for (int i = 1; i < parts.length; i++) {
                                PostureMonitorApplication.DEVICE_TYPE_LIST[i - 1] = parts[i];
                            }
                            waitingForTy = false;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Generate a toast message to tell user about his/her sensor node information
        String message = "You have " + PostureMonitorApplication.NUMBER_OF_SENSORNODE + " sensornodes.\n";
        for (int i = 0; i < PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
            message = message + PostureMonitorApplication.DEVICE_NAME_LIST[i] + " is at " + PostureMonitorApplication.SN_BODY_LIST[i] + "\n";
        }

        // Update global variables in application class and mySensornodes in ConfigActivity
        PostureMonitorApplication.generateDeviceMap();
        mActivity.initMySensornodes();
        mActivity.mSensornodeAcquired = true;

        // Use UI thread to show the toast message and make the other configuration-related buttons visible
        final String toastMessage = message;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity, toastMessage, Toast.LENGTH_LONG).show();
                showBtn("config");
                showBtn("add");
                showBtn("del");
                showBtn("viewBd");
                showBtn("addBd");
                if (mActivity.allConfigured()) {
                    showBtn("main");
                }
            }
        });
    }

}
