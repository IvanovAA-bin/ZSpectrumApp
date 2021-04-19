package com.zombio_odev.zspectrum.modules.DeviceListView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.welie.blessed.BluetoothPeripheral;

import java.util.ArrayList;

public class DeviceListView extends ScrollView {

    public interface DeviceListCallback {
        void onDevicePicked(String address);
    }

    private ArrayList<String> addressList;
    private DeviceListCallback callback;
    private LinearLayout LL;

    private final OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String chosenAddress = null;
            try {
                chosenAddress = addressList.get(v.getId());
            }
            catch (IndexOutOfBoundsException ex) {
                Toast.makeText(getContext(), "DEVICE LIST ADDRESS ERROR", Toast.LENGTH_LONG).show();
            }
            finally {
                callback.onDevicePicked(chosenAddress);
            }
        }
    };

    public DeviceListView(@NonNull Context context, @NonNull DeviceListCallback _callback) {
        super(context);
        this.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        //this.setBackgroundColor(Color.GRAY);
        LL = new LinearLayout(getContext());
        LL.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        LL.setOrientation(LinearLayout.VERTICAL);
        this.addView(LL);
        addressList = new ArrayList<>();
        callback = _callback;
    }

     private DeviceListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private DeviceListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private DeviceListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public boolean resetAndUpdateView(Intent dataIntent) {
        if (!dataIntent.getAction().equals("local_broadcast_device_list_data"))
            return true;

        int devListSize = dataIntent.getIntExtra("device_list_data", -1);
        if (devListSize == -1)
            return true;

        addressList.clear();
        //this.removeAllViews();
        LL.removeAllViews();

        for (int i = 0; i < devListSize; i++) {
            String devAddress = dataIntent.getStringExtra(i + "_1");
            String devName = dataIntent.getStringExtra( i + "_2");
            if (devAddress == null || devName == null)
                return true;
            addressList.add(devAddress);
            Button btn = new Button(getContext());
            btn.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            btn.setId(i);
            btn.setText(devName + " " + devAddress);
            btn.setOnClickListener(onClickListener);
            //this.addView(btn);
            LL.addView(btn);
        }
        this.invalidate();
        return false;
    }
}
