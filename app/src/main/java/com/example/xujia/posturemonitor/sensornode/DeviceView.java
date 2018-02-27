package com.example.xujia.posturemonitor.sensornode;

/**
 * Created by xujia on 2018-02-25.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xujia.posturemonitor.R;

// Fragment for Device View
public class DeviceView extends Fragment {

    public static DeviceView mInstance;

    // GUI
    private View view;
    public TextView mBarometerValues;
    public boolean first = true;

    // House-keeping
    private DeviceActivity mActivity;

    public DeviceView() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInstance = this;
        mActivity = (DeviceActivity) getActivity();

        view = inflater.inflate(R.layout.fragment_device, container,false);
        mBarometerValues = (TextView) view.findViewById(R.id.mBarometerText);
        mBarometerValues.setText("Miao miao miao!");

        // Notify activity that UI has been inflated
        mActivity.onViewInflated(view);

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

}
