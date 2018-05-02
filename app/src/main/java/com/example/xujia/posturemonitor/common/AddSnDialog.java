package com.example.xujia.posturemonitor.common;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.sensornode.ConfigActivity;
import com.example.xujia.posturemonitor.sensornode.PostureMonitorApplication;
import com.example.xujia.posturemonitor.util.CustomToast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class AddSnDialog extends DialogFragment {

    private ConfigActivity mThis;
    private PrintWriter mWriter;
    private BufferedReader mReader;
    private String mSnId;
    private String mBodyPart;

    @SuppressLint("ValidFragment")
    public AddSnDialog(PrintWriter writer, BufferedReader reader, ConfigActivity activity) {
        mWriter = writer;
        mReader = reader;
        mThis = activity;
    }
    
    public AddSnDialog() {
        
    }

    // private static String[] types = {"Select sensornode type", "CC2650 Sensortag", "BLE113 Sensornode"};

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mThis);
        // Get the layout inflater
        LayoutInflater inflater = mThis.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_sn, null);

//        ArrayAdapter<String> adapterType = new ArrayAdapter<String>(mThis, android.R.layout.simple_spinner_item, types);
//        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        Spinner spinnerType = (Spinner) view.findViewById(R.id.type_spinner);
//        spinnerType.setAdapter(adapterType);

        ArrayAdapter<String> adapterBody = new ArrayAdapter<String>(mThis, android.R.layout.simple_spinner_item, PostureMonitorApplication.BODY_LIST_USER);
        adapterBody.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinnerBody = (Spinner) view.findViewById(R.id.body_spinner);
        spinnerBody.setAdapter(adapterBody);

//       spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
//                String type = adapterView.getItemAtPosition(pos).toString();
//                if (type.contains("CC")) {
//                    mType = "CC";
//                } else if (type.contains("BLE")) {
//                    mType = "BLE";
//                }
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//            }
//        });

        spinnerBody.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                mBodyPart = adapterView.getItemAtPosition(pos).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                .setTitle("Add new sensornode")
                // Add action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mSnId = ((EditText) view.findViewById(R.id.name)).getText().toString();
                        // mMACAddress = ((EditText) view.findViewById(R.id.address)).getText().toString();
                        addSensornode();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddSnDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    private void addSensornode() {
        String message = "add " + mSnId + " " + PostureMonitorApplication.USERNAME + " " + mBodyPart;
        Thread worker = new Thread(new Runnable() {
            boolean waiting = true;
            @Override
            public void run() {
                mWriter.println(message);
                while (waiting) {
                    try {
                        String response = mReader.readLine();
                        if (response != null) {
                            if (response.equals("OK")) {
                                mThis.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToast.middleBottom(mThis, "New sensornode has been added!");
                                    }
                                });
                                waiting = false;
                                mThis.getSensornodes();
                                return;
                            } else if (response.equals("BAD")) {
                                mThis.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToast.middleBottom(mThis, "Cannot add new sensornode. Please try again!");
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

}
