package com.zombio_odev.zspectrum;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.zombio_odev.zspectrum.modules.BLEservice.BleService;
import com.zombio_odev.zspectrum.modules.BLEservice.Modules.IntentToServiceBuilder;
import com.zombio_odev.zspectrum.modules.CpsGraphics.MySurfaceView;
import com.zombio_odev.zspectrum.modules.CpsGraphics.SwitchSetting;
import com.zombio_odev.zspectrum.modules.DeviceListView.DeviceListView;
import com.zombio_odev.zspectrum.modules.SpectrumVeiw.SpectrumView;
import com.zombio_odev.zspectrum.modules.StatusView.StatusView;

public class MainActivity extends Activity {
    public static final String LOG_STR = "MEOW";
    private static final int ACCESS_COARSE_LOCATION_REQUEST = 142;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 512;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1421;


    // Main Layout Items
    private Toolbar toolbar;
    private TextView toolbarTextView;
    private Switch service_switch;
    private LinearLayout data_layout;
    private LinearLayout data_sublayout;
    private Button btn1, btn2, btn3;
    private Menu main_menu;
    private ScrollView scrollView;
    //------------------
    // Functional objects
    private BroadcastReceiver deviceListReceiver;
    private BroadcastReceiver connectionStatusReceiver;
    private BroadcastReceiver mcu_p_CPS_Receiver;
    private BroadcastReceiver mcu_p_spcb_Receiver;
    private BroadcastReceiver mcu_p_data_Receiver;
    //------------------
    // Data Layout Objects
    private SwitchSetting switchSetting;

    private DeviceListView deviceListView = null;
    private MySurfaceView msv;
    private StatusView statusView = null;
    private SpectrumView spectrumView = null;
    //------------------
    // Common objects
    private boolean serviceWorking;
    private boolean isConnectedToDevice = false;
    //------------------
    private final DeviceListView.DeviceListCallback deviceListCallback = new DeviceListView.DeviceListCallback() {
        @Override
        public void onDevicePicked(String address) {
            data_layout.removeView(deviceListView);
            deviceListView = null;
            Toast.makeText(getApplicationContext(), "Connecting to " + address, Toast.LENGTH_SHORT).show();
            if (serviceWorking)
                sendIntentToService(IntentToServiceBuilder.getInstance(getApplicationContext(), BleService.class)
                        .setModule(IntentToServiceBuilder.MODULE_ID_BLUETOOTH)
                        .setCommand(IntentToServiceBuilder.COMMAND_ID_BLUETOOTH_CONNECT)
                        .setData(address)
                        .build());
        }
    };
    //-----------------------------
    // HANDLER
    private final Handler.Callback handlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 11) {
                if (!serviceWorking || !isConnectedToDevice || statusView == null)
                    return true;
                sendIntentToService(IntentToServiceBuilder.getInstance(getApplicationContext(), BleService.class)
                        .setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                        .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_GET_DEVICE_STATUS)
                        .setData("none").build());
                handler.sendEmptyMessageDelayed(11, 750);
                return true;
            }
            return false;
        }
    };
    private final Handler handler = new Handler(handlerCallback);
    //--------------------------
    // ALERT DIALOG
    private int pendingCommand;
    private static final int MODULE_TO_NONE = -1;
    private static final int MODULE_TO_DEVICE = -2;
    private static final int MODULE_TO_CPS_GRAPHIC = -3;
    private int pendingCommandModule; // MODULE_TO
    private EditText editText;
    private AlertDialog dialog;
    //--------------------------

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout); // setting layout
        hasPermissions();
        android10PermisiionsRequests(); // ??????
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) { // check for ble support
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_LONG).show();
            finish();
        }
        initLayoutResources();
        initToolbar();
        initButtons();
        initAlertDialog();
        initBroadcastReceivers();
        service_switch.setChecked(isServiceRunning(BleService.class));
        serviceWorking = isServiceRunning(BleService.class);
        initServiceSwitch();

        Log.d(LOG_STR, "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(connectionStatusReceiver, new IntentFilter("local_broadcast_connection_state"));
        LocalBroadcastManager.getInstance(this).registerReceiver(deviceListReceiver, new IntentFilter("local_broadcast_device_list_data"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mcu_p_CPS_Receiver, new IntentFilter("local_broadcast_mcu_p_cps"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mcu_p_data_Receiver, new IntentFilter("local_broadcast_mcu_p_data"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mcu_p_spcb_Receiver, new IntentFilter("local_broadcast_mcu_p_spcb"));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionStatusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceListReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mcu_p_spcb_Receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mcu_p_data_Receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mcu_p_CPS_Receiver);
        super.onPause();
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Initializes layout resources such as toolbar etc
     */
    protected void initLayoutResources() {
        toolbar = findViewById(R.id.toolbar);
        service_switch = findViewById(R.id.service_switch);
        data_layout = findViewById(R.id.data_layout);
        data_sublayout = findViewById(R.id.data_sub_layout);
        btn1 = findViewById(R.id.button_1);
        btn2 = findViewById(R.id.button_2);
        btn3 = findViewById(R.id.button_3);
        toolbarTextView = findViewById(R.id.toolbar_text_view);
        scrollView = findViewById(R.id.scrollView);
        switchSetting = new SwitchSetting(new SwitchSetting.SwitchCallback() {
            @Override
            public void setAutoscrollToFalse() {
                main_menu.findItem(R.id.g2_s5).setChecked(false);
            }

            @Override
            public void setLockToFalse() {
                main_menu.findItem(R.id.g2_s6).setChecked(false);
            }
        });
    }

    private void showMyDialog(int module, int command, int inputType, String titleText) {
        pendingCommandModule = module;
        pendingCommand = command;
        editText.setInputType(inputType);
        editText.setText("");
        dialog.setTitle(titleText);
        dialog.show();
    }

    /**
     * Inflating menu, initializing menu, setting OnMenuItemClickListener
     * Implementing onMenuItemClickListener
     * NOT FINISHED
     */
    protected void initToolbar() {
        toolbar.inflateMenu(R.menu.main_menu);
        main_menu = toolbar.getMenu();

        //g2_s1 = Dynamic resolution
        //g2_s2 = Dynamic res. threshold value
        //g2_s3 = Warning level value
        //g2_s4 = Critical level value
        //g2_s5 = Autoscroll
        //g2_s6 = Locked
        //g2_s7 = Smoothing
        //g2_s8 = Start from zero
        //g2_s9 = Show CPS graphic

        main_menu.findItem(R.id.g2_s1).setChecked(switchSetting.getDynamicResolution());
        // set popup menu to set dynamic res threshold value
        // set popup menu to set warning level value
        // set popup menu to set critical level value
        main_menu.findItem(R.id.g2_s5).setChecked(switchSetting.getAutoScroll());
        main_menu.findItem(R.id.g2_s6).setChecked(switchSetting.getLocked());
        main_menu.findItem(R.id.g2_s7).setChecked(switchSetting.getSmoothing());
        main_menu.findItem(R.id.g2_s8).setChecked(switchSetting.getStartFromZero());
        main_menu.findItem(R.id.g2_s9).setChecked(switchSetting.getShowCPSgraphic());

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (!serviceWorking)
                    return true;
                IntentToServiceBuilder builder = new IntentToServiceBuilder(getApplicationContext(), BleService.class);
                switch (menuItem.getItemId()) {
                    case R.id.g1_s1: //g1_s1 = Start counting
                        sendIntentToService(builder.setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_START_IMPULSE_PROCESSING)
                                .setData("none").build());
                        break;
                    case R.id.g1_s2: //g1_s2 = Stop counting
                        sendIntentToService(builder.setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_STOP_IMPULSE_PROCESSING)
                                .setData("none").build());
                        break;
                    case R.id.g1_s3: //g1_s3 = Save spectrogram
                        sendIntentToService(builder.setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_SAVE_SPECTROGRAM_AS_TESTCSV)
                                .setData("none").build());
                        break;
                    case R.id.g1_s4: //g1_s4 = Start CPS
                        sendIntentToService(builder.setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_START_CPS_WRITING)
                                .setData("none").build());
                        break;
                    case R.id.g1_s5: //g1_s5 = Stop CPS
                        sendIntentToService(builder.setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_STOP_CPS_WRITING)
                                .setData("none").build());
                        break;
                    case R.id.g1_s6: //g1_s6 = Init FS
                        sendIntentToService(builder.setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_FS_INIT)
                                .setData("none").build());
                        break;
                    case R.id.g1_s7: //g1_s7 = Inverse LED
                        sendIntentToService(builder.setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_INVERSE_LED)
                                .setData("none").build());
                        break;
                    case R.id.g1_s8: //g1_s8 = Get 48 val from spectrogram
                        Toast.makeText(getApplicationContext(), "Can't use this command as user", Toast.LENGTH_SHORT).show();
                        //sendIntentToService(builder.setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                //.setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_GET_48_VALS_FROM_SPECT)
                                //.setData("0").build()); // @CHANGE
                        break;
                    case R.id.g1_s9: //g1_s9 = Clear spectrogram
                        sendIntentToService(builder.setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_CLEAR_SPECTROGRAM)
                                .setData("none").build());
                        break;
                    case R.id.g1_s10: // g1_s10_text = Set CPS time
                        showMyDialog(MODULE_TO_DEVICE, IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_CPS_TIME, InputType.TYPE_CLASS_NUMBER, "Set CPS time");
                        break;
                    case R.id.g1_s11: // g1_s11_text Save spectrum with filename
                        showMyDialog(MODULE_TO_DEVICE, IntentToServiceBuilder.COMMAND_ID_DEVICE_SAVE_SPECTR_WITH_SET_FILENAME, InputType.TYPE_CLASS_TEXT, "Save spectrum as ...");
                        break;
                    case R.id.g1_s12: // g1_s12_text Set measurement time
                        showMyDialog(MODULE_TO_DEVICE, IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_MEASUREMENT_TIME, InputType.TYPE_CLASS_NUMBER, "Set measurement time (min)");
                        break;
                    case R.id.g1_s13: // g1_s13_text Set device advertising number
                        showMyDialog(MODULE_TO_DEVICE, IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_DEVICE_ADV_NUMBER, InputType.TYPE_CLASS_TEXT, "Set device number");
                        break;
                    case R.id.g1_s14: // g1_s14_text Set counts for pulse
                        showMyDialog(MODULE_TO_DEVICE, IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_DMA_BUF_SIZE, InputType.TYPE_CLASS_NUMBER, "Set counts for 1 pulse");
                        break;
                    case R.id.g1_s15: // g1_s15_text Set min filtration value
                        showMyDialog(MODULE_TO_DEVICE, IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_MIN_FILTRATION_VALUE, InputType.TYPE_CLASS_NUMBER, "Set min filtration value");
                        break;
                    case R.id.g1_s16: // g1_s16_text Set max filtration value
                        showMyDialog(MODULE_TO_DEVICE, IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_MAX_FILTRATION_VALUE, InputType.TYPE_CLASS_NUMBER, "Set max filtration value");
                        break;
                    case R.id.g1_s17: // g1_s17_text Get device status
                        Toast.makeText(getApplicationContext(), "Can't get device status as user", Toast.LENGTH_SHORT).show();
                        break;

                        // CPS graphic settings
                    case R.id.g2_s1: //g2_s1 = Dynamic resolution
                        menuItem.setChecked(!menuItem.isChecked());
                        switchSetting.setDynamicResolution(menuItem.isChecked());
                        break;
                    case R.id.g2_s2: //g2_s2 = Dynamic res. threshold value
                        String valStr1 = Integer.toString(switchSetting.getDynamicResolutionThresholdValuee());
                        editText.setText(valStr1, TextView.BufferType.EDITABLE);
                        showMyDialog(MODULE_TO_CPS_GRAPHIC, SwitchSetting.DYNAMIC_THR_VAL_CMD, InputType.TYPE_CLASS_NUMBER, "Dynamic res. threshold value");
                        break;
                    case R.id.g2_s3: //g2_s3 = Warning level value
                        String valStr2 = Integer.toString(switchSetting.getWarningLevelValue());
                        editText.setText(valStr2);
                        showMyDialog(MODULE_TO_CPS_GRAPHIC, SwitchSetting.WARNING_LVL_VAL_CMD, InputType.TYPE_CLASS_NUMBER, "Warning level value");
                        break;
                    case R.id.g2_s4: //g2_s4 = Critical level value
                        String valStr3 = Integer.toString(switchSetting.getCriticalLevelValue());
                        editText.setText(valStr3);
                        showMyDialog(MODULE_TO_CPS_GRAPHIC, SwitchSetting.CRITICAL_LVL_VAL_CMD, InputType.TYPE_CLASS_NUMBER, "Warning level value");
                        break;
                    case R.id.g2_s5: //g2_s5 = Autoscroll
                        menuItem.setChecked(!menuItem.isChecked());
                        switchSetting.setAutoScroll(menuItem.isChecked());
                        break;
                    case R.id.g2_s6: //g2_s6 = Locked
                        menuItem.setChecked(!menuItem.isChecked());
                        switchSetting.setLocked(menuItem.isChecked());
                        break;
                    case R.id.g2_s7: //g2_s7 = Smoothing
                        menuItem.setChecked(!menuItem.isChecked());
                        switchSetting.setSmoothing(menuItem.isChecked());
                        break;
                    case R.id.g2_s8: //g2_s8 = Start from zero
                        menuItem.setChecked(!menuItem.isChecked());
                        switchSetting.setStartFromZero(menuItem.isChecked());
                        break;
                    case R.id.g2_s9: //g2_s9 = Show CPS graphic
                        menuItem.setChecked(!menuItem.isChecked());
                        switchSetting.setShowCPSgraphic(menuItem.isChecked());
                        break;
                }

                return true;
            }
        });
    }

    /**
     * Setting onClickListeners on buttons
     * NOT FINISHED
     */
    protected void initButtons() {
        btn1.setOnClickListener(v -> btn1_Click()); // lambda ?
        btn2.setOnClickListener(v -> btn2_Click());
        btn3.setOnClickListener(v -> btn3_Click());
    }
    void btn1_Click() {
        if (!serviceWorking || !isConnectedToDevice)
            return;
        // clearing data_layout
        data_layout.removeAllViews();
        statusView = null;
        spectrumView = null;
        main_menu.setGroupVisible(R.id.menu_CPS_settings_group, true);
        //-----------------------

        msv = new MySurfaceView(this, switchSetting, 1000);
        data_layout.addView(msv);
    }
    void btn2_Click() {
        if (!serviceWorking || !isConnectedToDevice)
            return;

        // clearing data_layout
        data_layout.removeAllViews();
        msv = null;
        spectrumView = null;
        main_menu.setGroupVisible(R.id.menu_CPS_settings_group, false);
        //-----------------------

        // create status view
        statusView = new StatusView(this);
        data_layout.addView(statusView);

        handler.sendEmptyMessageDelayed(11, 20);
    }
    void btn3_Click() {
        if (!serviceWorking || !isConnectedToDevice)
            return;

        // clearing data_layout
        data_layout.removeAllViews();
        msv = null;
        statusView = null;
        main_menu.setGroupVisible(R.id.menu_CPS_settings_group, false);
        //-----------------------

        spectrumView = new SpectrumView(this);
        data_layout.addView(spectrumView);
        spectrumIndexPending = 0;
        sendIntentToService(IntentToServiceBuilder.getInstance(this, BleService.class)
        .setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
        .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_GET_48_VALS_FROM_SPECT)
        .setData(Integer.toString(spectrumIndexPending)).build());

    }
    //---------------------------------------------------------

    private static int normalizeInt_1(int value) {
        return value & 0x000000FF;
    }

    private static int normalizeInt_2(int value) {
        return value & 0x0000FF00;
    }

    private static int normalizeInt_3(int value) {
        return value & 0x00FF0000;
    }

    private static int normalizeInt_4(int value) {
        return value & 0xFF000000;
    }

    private int spectrumIndexPending = 0;
    /**
     * Initializes broadcast receivers, that handles local messages from service
     * deviceListReceiver
     * NOT FINISHED
     */
    protected void initBroadcastReceivers() {
        deviceListReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (deviceListView != null) {
                    deviceListView.resetAndUpdateView(intent);
                }
                /*else {
                    if (data_layout.getChildCount() == 0) {
                        deviceListView = new DeviceListView(getApplicationContext(), deviceListCallback);
                        data_layout.addView(deviceListView);
                    }

                }*/
            }
        };

        connectionStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals("local_broadcast_connection_state"))
                    return;
                int isConnected = intent.getIntExtra("connected", -1);
                if (isConnected == -1)
                    return;
                if (isConnected == 0) {
                    isConnectedToDevice = false;
                    main_menu.setGroupVisible(R.id.menu_CPS_settings_group, false);
                    main_menu.setGroupVisible(R.id.menu_device_settings_group, false);
                    String text = "Disconnected";
                    toolbarTextView.setText(text);
                }
                else if (isConnected == 1) {
                    isConnectedToDevice = true;
                    main_menu.setGroupVisible(R.id.menu_device_settings_group, true);
                    String name = intent.getStringExtra("name");
                    String address = intent.getStringExtra("addr");
                    if (name == null || address == null) {
                        Log.d("MEOW", "EDQWDQWQWE");
                        return;
                    }
                    toolbarTextView.setText("Connected to: " + name + " " + address);
                }
            }
        };

        mcu_p_spcb_Receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] data = intent.getByteArrayExtra("data");
                if (data == null)
                {
                    Toast.makeText(getApplicationContext(), "Error occured while receiving device callback", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (data.length < 10) // impossible situation, but just in case
                {
                    Toast.makeText(getApplicationContext(), "0x142512432", Toast.LENGTH_SHORT).show();
                    return;
                }
                int cmdResponse = normalizeInt_1(data[0]);
                int cmd = normalizeInt_2(data[1] << 8) + normalizeInt_1(data[2]);
                switch (cmd)
                {
                    case 0x0003:
                        if (cmdResponse == 1)
                            Toast.makeText(getApplicationContext(), "FS is not initialized", Toast.LENGTH_SHORT).show();
                        break;
                    case 0x006:
                        if (cmdResponse == 0x01)
                            Toast.makeText(getApplicationContext(), "uSD detached", Toast.LENGTH_SHORT).show();
                        else if (cmdResponse == 0x02)
                            Toast.makeText(getApplicationContext(), "FS already initialized", Toast.LENGTH_SHORT).show();
                        else if (cmdResponse == 0x03)
                            Toast.makeText(getApplicationContext(), "FS initialization error", Toast.LENGTH_SHORT).show();
                        break;
                    case 0x000A:
                        if (cmdResponse == 0x01)
                            Toast.makeText(getApplicationContext(), "Time parameter can't be less than 50 millis", Toast.LENGTH_SHORT).show();
                        break;
                    case 0x000B:
                        if (cmdResponse == 0x01)
                            Toast.makeText(getApplicationContext(), "Forbidden string length", Toast.LENGTH_SHORT).show();
                        else if (cmdResponse == 0x02)
                            Toast.makeText(getApplicationContext(), "Wrong #...# allocation", Toast.LENGTH_SHORT).show();
                        else if (cmdResponse == 0x03)
                            Toast.makeText(getApplicationContext(), "The string contains forbidden characters", Toast.LENGTH_SHORT).show();
                        else if (cmdResponse == 0x04)
                            Toast.makeText(getApplicationContext(), "uSD is not attached", Toast.LENGTH_SHORT).show();
                        break;
                    case 0x000D:
                        if (cmdResponse == 0x01)
                            Toast.makeText(getApplicationContext(), "The string contains forbidden characters", Toast.LENGTH_SHORT).show();
                        else if (cmdResponse == 0x02)
                            Toast.makeText(getApplicationContext(), "uSD is not attached", Toast.LENGTH_SHORT).show();
                        break;
                    case 0x000E:
                        if (cmdResponse == 0x01)
                            Toast.makeText(getApplicationContext(), "Value is not in [5,1000]", Toast.LENGTH_SHORT).show();
                        else if (cmdResponse == 0x02)
                            Toast.makeText(getApplicationContext(), "uSD is not attached", Toast.LENGTH_SHORT).show();
                        break;
                    case 0x000F: case 0x0010:
                        if (cmdResponse == 0x01)
                            Toast.makeText(getApplicationContext(), "uSD is not attached", Toast.LENGTH_SHORT).show();
                        break;
                    case 0x0011:
                        if (statusView != null)
                            if (statusView.updateStatusData(data))
                                Toast.makeText(getApplicationContext(), "statusError occurred", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
            }
        };

        mcu_p_CPS_Receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] data = intent.getByteArrayExtra("data");
                if (data == null)
                {
                    Toast.makeText(getApplicationContext(), "Error occured while receiving device callback", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (data.length < 10) // impossible situation, but just in case
                {
                    Toast.makeText(getApplicationContext(), "0x142512432", Toast.LENGTH_SHORT).show();
                    return;
                }
                int cpsValue = normalizeInt_4(data[0] << 24) + normalizeInt_3(data[1] << 16) + normalizeInt_2(data[2] << 8) + normalizeInt_1(data[3]);
                if (msv != null)
                    msv.addData(cpsValue);
                // add normal conversion
                //Toast.makeText(getApplicationContext(), cpsValue, Toast.LENGTH_SHORT).show();
            }
        };

        mcu_p_data_Receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] data = intent.getByteArrayExtra("data");
                if (data == null)
                {
                    Toast.makeText(getApplicationContext(), "Error occured while receiving device callback", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (data.length < 10) // impossible situation, but just in case
                {
                    Toast.makeText(getApplicationContext(), "0x142512432", Toast.LENGTH_SHORT).show();
                    return;
                }
                int cmdResponse = normalizeInt_1(data[0]);
                int cmd = normalizeInt_2(data[1] << 8) + normalizeInt_1(data[2]);

                if (cmd == 0x0008 && spectrumView != null && cmdResponse == 0) {
                    int indexRequested = normalizeInt_2(data[3] << 8) + normalizeInt_1(data[4]);
                    int len = normalizeInt_2(data[5] << 8) + normalizeInt_1(data[6]);
                    spectrumIndexPending = indexRequested + len;
                    if (spectrumView != null) {
                        try {
                            int errVal = spectrumView.addReceivedData(data);
                            if (errVal != 0)
                                Toast.makeText(getApplicationContext(), "Error occured, while processing receiver spectrum data: " + errVal, Toast.LENGTH_LONG).show();
                            if (spectrumIndexPending < 4096)
                                sendIntentToService(IntentToServiceBuilder.getInstance(getApplicationContext(), BleService.class)
                                        .setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                        .setCommand(IntentToServiceBuilder.COMMAND_ID_DEVICE_GET_48_VALS_FROM_SPECT)
                                        .setData(Integer.toString(spectrumIndexPending)).build());
                        }
                        catch (Exception ex) {
                            Toast.makeText(getApplicationContext(), "Index requested = " + Integer.toHexString(indexRequested) + " Byte_1 = " + normalizeInt_1(data[3]) + " Byte_2 = " + normalizeInt_1(data[4]), Toast.LENGTH_LONG).show();

                        }
                    }
                }


            }
        };
    }

    /**
     * Creates on OnCheckedChangeListener, that starts or stops service
     */
    protected void initServiceSwitch() {
        service_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!isServiceRunning(BleService.class)) {
                        sendIntentToService(IntentToServiceBuilder.getInstance(getApplicationContext(), BleService.class)
                                .setModule(IntentToServiceBuilder.MODULE_ID_SERVICE)
                                .setCommand(IntentToServiceBuilder.COMMAND_ID_SERVICE_START)
                                .setData("Service started").build());
                        deviceListView = new DeviceListView(getApplicationContext(), deviceListCallback);
                        data_layout.addView(deviceListView);
                        serviceWorking = true;
                    }
                }
                else {
                    if (isServiceRunning(BleService.class)) {
                        stopService(new Intent(MainActivity.this, BleService.class));
                        //sendIntentToService(IntentToServiceBuilder.getInstance(getApplicationContext(), BleService.class)
                                //.setModule(IntentToServiceBuilder.MODULE_ID_SERVICE)
                                //.setCommand(IntentToServiceBuilder.COMMAND_ID_SERVICE_STOP)
                                //.setData("none").build());
                        data_layout.removeAllViews();
                        serviceWorking = false;
                        isConnectedToDevice = false;
                    }
                }
            }
        });
    }

    /**
     * Initializes alert dialog that is used in commands,
     * that have extended format. Creates AlretDialog.Builder,
     * EditText view, onClickListeners
     */
    private void initAlertDialog() {
        editText = new EditText(this);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        pendingCommand = -1;

        dialogBuilder.setTitle("Title")
                .setView(editText)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (pendingCommandModule) {
                            case MODULE_TO_DEVICE:
                                {
                                    if (!isConnectedToDevice || !serviceWorking)
                                        return;

                                    if (pendingCommand == IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_CPS_TIME && msv != null) {
                                        try {
                                            int value = Integer.parseInt(editText.getText().toString());
                                            if (msv.setTimeBetweenMeasurements(value)) {
                                                Toast.makeText(getApplicationContext(), "Value must be: 1000 % val = 0", Toast.LENGTH_LONG).show();
                                                return;
                                            }
                                        }
                                        catch (Exception ex) {
                                            Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                                        }
                                    }

                                    sendIntentToService(IntentToServiceBuilder.getInstance(getApplicationContext(), BleService.class)
                                            .setModule(IntentToServiceBuilder.MODULE_ID_DEVICE)
                                            .setCommand(pendingCommand)
                                            .setData(editText.getText().toString()).build());
                                    pendingCommand = -1;
                                    pendingCommandModule = MODULE_TO_NONE;
                                }
                                break;
                            case MODULE_TO_CPS_GRAPHIC:
                                {
                                    switch (pendingCommand) {
                                        case SwitchSetting.DYNAMIC_THR_VAL_CMD:
                                            {
                                                try {
                                                    int dynamicThrValue = Integer.parseInt(editText.getText().toString());
                                                    switchSetting.setDynamicResolutionThresholdValue(dynamicThrValue);
                                                }
                                                catch (Exception ex) {
                                                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                                                }
                                            }
                                            break;
                                        case SwitchSetting.WARNING_LVL_VAL_CMD:
                                            {
                                                try {
                                                    int warningLvlValue = Integer.parseInt(editText.getText().toString());
                                                    switchSetting.setWarningLevelValue(warningLvlValue);
                                                }
                                                catch (Exception ex) {
                                                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                                                }
                                            }
                                            break;
                                        case SwitchSetting.CRITICAL_LVL_VAL_CMD:
                                            {
                                                try {
                                                    int criticalLvlValue = Integer.parseInt(editText.getText().toString());
                                                    switchSetting.setCriticalLevelValue(criticalLvlValue);
                                                }
                                                catch (Exception ex) {
                                                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                                                }
                                            }
                                            break;
                                        default:
                                            Toast.makeText(getApplicationContext(), "Unknown command", Toast.LENGTH_SHORT).show();
                                            break;
                                    }
                                }
                                break;
                            default:
                                Toast.makeText(getApplicationContext(), "Unknown module id", Toast.LENGTH_SHORT).show();
                                break;
                        }
                        pendingCommand = 0;
                        pendingCommandModule = MODULE_TO_NONE;

                    }
                })
                .setNegativeButton("BACK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pendingCommand = 0;
                        pendingCommandModule = MODULE_TO_NONE;
                    }
                });
        dialog = dialogBuilder.create();
    }

    /**
     * This function can check if some service is currently
     * running on device
     * @param ServiceClass service class, that should be checked
     * @return true if service is running, false in other case
     */
    protected boolean isServiceRunning(Class<?> ServiceClass) { // https://www.cyberforum.ru/android-dev/thread1313536.html COMPLETED
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (ServiceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param intent must be created only from IntentToServiceBuilder class
     * This functions sends commands as intents to service
     */
    protected void sendIntentToService(Intent intent) { // COMPLETED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent);
        else
            startService(intent);
    }


    /**
     * Function also requests permission and then processes it in
     * onRequestPermissionsResult callback
     * @return value depends on ACCESS_COARSE_LOCATION permission
     */
    private boolean hasPermissions() { // https://habr.com/ru/post/536392/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, ACCESS_COARSE_LOCATION_REQUEST);
                return false;
            }
        }
        return true;
    }

    /**
     * @param requestCode = ACCESS_COARSE_LOCATION_REQUEST
     * @param permissions = android.permission.ACCESS_COARSE_LOCATION
     * @param grantResults 0 - OK. -1 - permisiion denied
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ACCESS_COARSE_LOCATION_REQUEST) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals("android.permission.ACCESS_COARSE_LOCATION")) {
                    if (grantResults[i] == -1) {
                        Toast.makeText(this, "You should grant permission", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
            }
        }
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    void android10PermisiionsRequests() { // https://stackoverflow.com/questions/32708374/bluetooth-le-scan-fails-in-the-background-permissions/32730190#32730190 && https://habr.com/ru/post/536392/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("This app needs background location access");
                        builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                            @TargetApi(23)
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        PERMISSION_REQUEST_BACKGROUND_LOCATION);
                            }

                        });
                        builder.show();
                    }
                    else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Functionality limited");
                        builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.");
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }

                        });
                        builder.show();
                    }

                }
            } else {
                if (!this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            PERMISSION_REQUEST_FINE_LOCATION);
                }
                else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location access to this app.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }

            }
        }
    }
}

