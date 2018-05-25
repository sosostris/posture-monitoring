/**
 * Xujia Zhou. Copyright (c) 2018.
 */

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

/**
 * Represents the dialog in ConfigActivity that adds a new wearing position.
 */
public class AddBodyDialog extends DialogFragment{

    private ConfigActivity mThis;
    private PrintWriter mWriter;
    private BufferedReader mReader;
    private String mBody;

    @SuppressLint("ValidFragment")
    public AddBodyDialog(PrintWriter writer, BufferedReader reader, ConfigActivity activity) {
        mWriter = writer;
        mReader = reader;
        mThis = activity;
    }

    public AddBodyDialog() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mThis);
        // Get the layout inflater
        LayoutInflater inflater = mThis.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_body, null);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                .setTitle("Add new wearing position")
                // Add action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mBody = ((EditText) view.findViewById(R.id.newBody)).getText().toString();
                        addNewBody();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddBodyDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    /**
     * Add a new wearing position.
     */
    private void addNewBody() {
        String message = "nbd " + PostureMonitorApplication.USERNAME + " " + mBody;
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
                                updateUserBodyList();
                                mThis.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToast.middleBottom(mThis, "New wearing position has been added!");
                                    }
                                });
                                waiting = false;
                                return;
                            } else if (response.equals("BAD")) {
                                mThis.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToast.middleBottom(mThis, "Cannot add new wearing position. Please try again!");
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

    /**
     * Update the global variable.
     */
    private void updateUserBodyList() {
        PostureMonitorApplication.BODY_LIST_USER.add(mBody);
    }

}
