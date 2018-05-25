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

import com.example.xujia.posturemonitor.sensornode.ConfigActivity;
import com.example.xujia.posturemonitor.sensornode.PostureMonitorApplication;
import com.example.xujia.posturemonitor.util.CustomToast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Class that represents the dialog where the user can delete a sensor node.
 */
public class DelSnDialog extends DialogFragment {

    private ConfigActivity mThis;
    PrintWriter mWriter;
    BufferedReader mReader;
    int mSelectedItem;

    @SuppressLint("ValidFragment")
    public DelSnDialog(PrintWriter writer, BufferedReader reader, ConfigActivity activity) {
        mWriter = writer;
        mReader = reader;
        mThis = activity;
    }

    public DelSnDialog() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(mThis);
        builder.setTitle("Choose sensornode to delete")
                .setSingleChoiceItems(PostureMonitorApplication.DEVICE_NAME_LIST, -1,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSelectedItem = which;
                            }
                        })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        updateSnList();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    /**
     * Updates the global sensor node list in application class.
     */
    private void updateSnList() {
        String snToDel = PostureMonitorApplication.DEVICE_NAME_LIST[mSelectedItem];
        String message = "del " + snToDel;
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
                                        CustomToast.middleBottom(mThis, snToDel + " has been deleted!");
                                    }
                                });
                                waiting = false;
                                mThis.getSensornodes();
                                return;
                            } else if (response.equals("BAD")) {
                                mThis.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToast.middleBottom(mThis, "Cannot delete sensornode. Please try again!");
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