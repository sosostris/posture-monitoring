package com.example.xujia.posturemonitor.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.example.xujia.posturemonitor.sensornode.PostureMonitorApplication;

public class BodyDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String message = "";
        for (int i=0; i< PostureMonitorApplication.BODY_LIST_USER.size(); i++) {
            message = (i == PostureMonitorApplication.BODY_LIST_USER.size() - 1) ?
                message + "    " + PostureMonitorApplication.BODY_LIST_USER.get(i) :
                    message + "    " + PostureMonitorApplication.BODY_LIST_USER.get(i) + "\n";
        }
        builder.setTitle("View wearing positions")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
