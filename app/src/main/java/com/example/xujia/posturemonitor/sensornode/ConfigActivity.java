package com.example.xujia.posturemonitor.sensornode;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.AddBodyDialog;
import com.example.xujia.posturemonitor.common.AddSnDialog;
import com.example.xujia.posturemonitor.common.BluetoothLeService;
import com.example.xujia.posturemonitor.common.BodyDialog;
import com.example.xujia.posturemonitor.common.DelSnDialog;
import com.example.xujia.posturemonitor.common.MySensornode;
import com.example.xujia.posturemonitor.util.CustomToast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConfigActivity extends ViewPagerActivity {

    private static final String TAG = "ConfigActivity";

    // TCP connection with Java Bigtable server
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    public boolean mSensornodeAcquired = false;
    public boolean mAuthenticated = false;
    private boolean mConnectedToServer = false;
    private boolean mConfigured = false;

    // GUI
    private ConfigActivity mThis;
    private ConfigView mConfigView;
    private ConfigSnView mConfigSnView;
    private Intent mMainIntent;

    private MySensornode[] mySensornodes;

    public ConfigActivity() {
        mThis = this;
        mResourceFragmentPager = R.layout.fragment_pager;
        mResourceIdPager = R.id.pager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the fragments and add them to the view pager and tabs
        mConfigView = new ConfigView();
        mSectionsPagerAdapter.addSection(mConfigView, "Configuration");

    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        // mActivity.registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        closeConnection();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        closeConnection();
        super.onDestroy();
    }

    public void configServers(View view) {
        closeConnection();
        configServers();
    }

    private void configServers() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_server);
        dialog.setTitle("Config servers");
        Button okButton = dialog.findViewById(R.id.ok_btn);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostureMonitorApplication.JAVA_IP = ((EditText) dialog.findViewById(R.id.java_ip)).getText().toString();
                PostureMonitorApplication.MATLAB_IP = ((EditText) dialog.findViewById(R.id.matlab_ip)).getText().toString();
                mConfigured = true;
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void connectToServer(View view) {
        if (!mConfigured && PostureMonitorApplication.USERNAME==null) {
            CustomToast.middleBottom(this, "Please config servers first!");
            return;
        }
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(PostureMonitorApplication.JAVA_IP, PostureMonitorApplication.JAVA_PORT_GENERAL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (socket != null) {
                    try {
                        writer = new PrintWriter(socket.getOutputStream(), true);
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        Log.d(TAG, "Connected with Bigtable Java TCP server");
                        mConnectedToServer = true;
                        mThis.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                CustomToast.middleBottom(mThis, "Connected to server!");
                                if (PostureMonitorApplication.USERNAME==null) {
                                    mConfigView.showLogin();
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        worker.start();
    }

    public void login(View view) {
        mConfigView.login(writer, reader);
    }

    public void getSensornodes(View view) {
        getSensornodes();
    }

    public void getSensornodes() {
        Thread worker = new Thread(new Runnable() {
            boolean waitingForNr = true;
            boolean waitingForId = true;
            boolean waitingForAd = true;
            boolean waitingForBd = true;
            boolean waitingForTy = true;
            @Override
            public void run() {
                writer.println("sn" + " " + PostureMonitorApplication.USERNAME);
                while (waitingForNr || waitingForId || waitingForAd || waitingForBd || waitingForTy) {
                    try {
                        String response = reader.readLine();
                        if (response != null) {
                            if (response.equals("BAD")) {
                                mThis.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToast.middleBottom(mThis, "You have no sensornodes!");
                                    }
                                });
                                return;
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
                String message = "You have " + PostureMonitorApplication.NUMBER_OF_SENSORNODE + " sensornodes.\n";
                for (int i=0; i<PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
                    message = message + PostureMonitorApplication.DEVICE_NAME_LIST[i] + " is at " + PostureMonitorApplication.SN_BODY_LIST[i] + "\n";
                }
                PostureMonitorApplication.generateDeviceList();
                mSensornodeAcquired = true;
                final String toastMessage = message;
                mThis.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initMySensornodes();
                        Toast.makeText(mThis, toastMessage, Toast.LENGTH_LONG).show();
                        if (allConfigured()) {
                            mConfigView.showBtn("main");
                        }
                    }
                });
            }
        });
        worker.start();
    }

    public MySensornode[] getMySensornodes() {
        return mySensornodes;
    }

    public void initMySensornodes() {
        mySensornodes = new MySensornode[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        for (int i=0; i<PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
            mySensornodes[i] = new MySensornode(PostureMonitorApplication.DEVICE_ADDRESS_LIST[i],
                    PostureMonitorApplication.DEVICE_NAME_LIST[i], PostureMonitorApplication.DEVICE_TYPE_LIST[i],
                    PostureMonitorApplication.SN_BODY_LIST[i]);
        }
        if (mConfigSnView != null) {
            mConfigSnView.notifyDataSetChanged();
        }
    }

    public void gotoSnConfigFrag(View view) {
        if (writer == null || reader == null) {
            CustomToast.middleBottom(mThis, "Please connect to server first!");
            return;
        }
        if (mConfigSnView == null) {
            mConfigSnView = new ConfigSnView();
            mSectionsPagerAdapter.addSection(mConfigSnView, "Config sensornodes");
        }
        loadFragment(1);
    }

    public void gotoMain(View view) {
        BluetoothLeService.myBluetoothGattCallbacks = new BluetoothLeService.MyBluetoothGattCallback[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        mMainIntent = new Intent(this, MainActivity.class);
        startActivityForResult(mMainIntent, -1);
    }

    private void closeConnection() {
        if (writer != null) {
            writer.println("ex");
            writer.close();
            writer = null;
        }
        if (reader != null) {
            try {
                reader.close();
                reader = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket != null) {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean allConfigured() {
        for (int i=0; i<PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
            if (PostureMonitorApplication.SN_BODY_LIST[i] == null) {
                return false;
            }
        }
        return true;
    }

    public void uploadSnConfig(View view) {
        Thread worker = new Thread(new Runnable() {
            boolean waiting = true;
            String message = "sn-bd ";
            String[] newBodyParts = mConfigSnView.getNewBodyParts();
            @Override
            public void run() {
                for (int i=0; i<PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
                    message = message + PostureMonitorApplication.DEVICE_NAME_LIST[i] + " ";
                    message = message + newBodyParts[i] + " ";
                }
                writer.println(message);
                while (waiting) {
                    try {
                        String response = reader.readLine();
                        if (response != null) {
                            if (response.equals("OK")) {
                                updateSnBodyList();
                                updateMySnBodyList();
                                mThis.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mThis, "New sensornode body configuration has been uploaded!\n" +
                                                "Please try to wear the sensornodes always at the same body spot!", Toast.LENGTH_LONG).show();
                                        mConfigSnView.showMainBtn();
                                        mConfigSnView.notifyDataSetChanged();
                                    }
                                });
                                waiting = false;
                                return;
                            } else if (response.equals("BAD")) {
                                mThis.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToast.middleBottom(mThis, "New sensornode body configuration failed to upload.\n" +
                                                "Please try again later!");
                                    }
                                });
                                waiting = false;
                                return;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        worker.start();
    }

    public void addSensornode(View view) {
        if (writer == null || reader == null) {
            CustomToast.middleBottom(mThis, "Please connect to server first!");
            return;
        }
        AddSnDialog snDialog = new AddSnDialog(writer, reader, mThis);
        snDialog.show(getFragmentManager(), TAG);
    }

    public void delSensornode(View view) {
        if (writer == null || reader == null) {
            CustomToast.middleBottom(mThis, "Please connect to server first!");
            return;
        }
        DelSnDialog delSnDialog = new DelSnDialog(writer, reader, mThis);
        delSnDialog.show(getFragmentManager(), TAG);
    }

    public void viewBody(View view) {
        BodyDialog bodyDialog = new BodyDialog();
        bodyDialog.show(getFragmentManager(), TAG);
    }

    public void addBody(View view) {
        if (writer == null || reader == null) {
            CustomToast.middleBottom(mThis, "Please connect to server first!");
            return;
        }
        AddBodyDialog addBodyDialog = new AddBodyDialog(writer, reader, mThis);
        addBodyDialog.show(getFragmentManager(), TAG);
    }

    private void updateSnBodyList() {
        String[] newBodyParts = mConfigSnView.getNewBodyParts();
        PostureMonitorApplication.ADDRESS_BODY_MAP.clear();
        for (int i=0; i<PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
            PostureMonitorApplication.SN_BODY_LIST[i] = newBodyParts[i];
            PostureMonitorApplication.ADDRESS_BODY_MAP.put(PostureMonitorApplication.DEVICE_ADDRESS_LIST[i],
                    PostureMonitorApplication.SN_BODY_LIST[i]);
        }
    }

    private void updateMySnBodyList() {
        for (int i=0; i<PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
            mySensornodes[i].setBody(PostureMonitorApplication.SN_BODY_LIST[i]);
        }
    }

}
