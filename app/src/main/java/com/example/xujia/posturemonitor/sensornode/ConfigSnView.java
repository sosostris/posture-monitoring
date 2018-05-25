/**
 * Xujia Zhou. Copyright (c) 2018.
 */

package com.example.xujia.posturemonitor.sensornode;

import android.annotation.SuppressLint;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.MySensornode;

import java.util.ArrayList;

/**
 * Fragment class for configuring wearing positions of user-owned sensor nodes.
 */
public class ConfigSnView extends Fragment {

    private static String TAG = "ConfigSnView";

    private ConfigActivity mConfigActivity;
    private Context mContext;
    private SnListAdapter mSnListAdapter;

    private Button mGotoMainBtn;
    private Button mBtnOK;
    private ListView mSnListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Log.i(TAG, "onCreateView");

        // The last two arguments ensure LayoutParams are inflated properly.
        View view = inflater.inflate(R.layout.fragment_body, container, false);

        mConfigActivity = (ConfigActivity) getActivity();
        mContext = mConfigActivity.getApplicationContext();

        // Initialize widgets and ListView adapter
        mBtnOK = (Button) view.findViewById(R.id.btn_ok);
        mGotoMainBtn = (Button) view.findViewById(R.id.main_btn);

        mSnListView = (ListView) view.findViewById(R.id.sn_list);
        mSnListAdapter = new SnListAdapter(mContext, mConfigActivity.getMySensornodes());
        mSnListView.setAdapter(mSnListAdapter);

        return view;
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
        // stopTimer();
        super.onPause();
        // mActivity.unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    public void notifyDataSetChanged() {
        mSnListAdapter.mSensornodes = mConfigActivity.getMySensornodes();
        mSnListAdapter.notifyDataSetChanged();
    }

    /**
     * Inner class adapter to render sensor node list.
     * Each item shows on the left side sensor node information including Id, address and
     * currently configured wearing position. On the right side is a drop down menu where user
     * could pick a new wearing position for the sensor node.
     */
    @SuppressLint("InflateParams")
    class SnListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        private ArrayList<String> mBodyParts;
        private String[] newBodyParts;
        private MySensornode[] mSensornodes;

        public SnListAdapter(Context context, MySensornode[] mySensornodes) {

            mSensornodes = mySensornodes;

            mInflater = LayoutInflater.from(context);
            mBodyParts = new ArrayList<>();
            mBodyParts.add("Please select");
            for (String s : PostureMonitorApplication.BODY_LIST_USER) {
                mBodyParts.add(s);
            }
            newBodyParts = new String[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
            for (int i=0; i<PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
                newBodyParts[i] = PostureMonitorApplication.SN_BODY_LIST[i];
            }

        }

        @Override
        public int getCount() {
            return mSensornodes.length;
        }

        public Object getItem(int position) {
            return mSensornodes[position];
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, final View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) mInflater.inflate(R.layout.element_sensornode, null);
            }

            MySensornode sn = mSensornodes[position];

            String snName = mSensornodes[position].getName();
            String snAddress = mSensornodes[position].getAddress();
            String snBodyPart = mSensornodes[position].getBody();

            String description = snName + "\n" + snAddress + "\n" + "Current spot: " + snBodyPart;
            ((TextView) vg.findViewById(R.id.sn_name)).setText(description);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                    android.R.layout.simple_spinner_item, mBodyParts);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            Spinner spinner = (Spinner) vg.findViewById(R.id.spinner);
            spinner.setAdapter(adapter);

           spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                    String bodyPart = adapterView.getItemAtPosition(pos).toString();
                    newBodyParts[position] = bodyPart;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }

            });

            return vg;
        }

    }

    /**
     * Make the button for going to MainActivity visile to user.
     */
    public void showMainBtn() {
        mGotoMainBtn.setVisibility(View.VISIBLE);
    }

    /**
     * Hide the "OK" button.
     */
    public void hideOKBtn() {
        mBtnOK.setVisibility(View.GONE);
    }

    /**
     * Return the newly picked up wearing positions by the user for each sensor node as a
     * String array.
     */
    public String[] getNewBodyParts() {
        return mSnListAdapter.newBodyParts;
    }



}
