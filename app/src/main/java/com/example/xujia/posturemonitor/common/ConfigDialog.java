package com.example.xujia.posturemonitor.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.example.xujia.posturemonitor.sensornode.PostureMonitorApplication;

public class ConfigDialog extends DialogFragment {

    int mSelectedItem;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] choices = {"1", "2", "3"};
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Configure number of sensornodes:")
                .setSingleChoiceItems(choices, -1,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSelectedItem = which;
                            }
                        })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PostureMonitorApplication.NUMBER_OF_SENSORNODE = Integer.valueOf(choices[mSelectedItem]);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
