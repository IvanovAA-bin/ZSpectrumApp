package com.zombio_odev.zspectrum.modules.BLEservice;

import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.icu.text.UnicodeSetSpanner;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.ConnectionState;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.PhyType;
import com.welie.blessed.ScanFailure;
import com.welie.blessed.ScanMode;
import com.welie.blessed.WriteType;
import com.zombio_odev.zspectrum.modules.BLEservice.Modules.IntentToServiceBuilder;
import com.zombio_odev.zspectrum.modules.BLEservice.Modules.ServiceNotificationManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class BleService extends Service {

    /**
     * Used to manage notifications (create, update)
     */
    private ServiceNotificationManager notificationManager;

    //******<BLE MODULES>********
    private final UUID service_UUID =    UUID.fromString("0000ad01-cc7a-482a-984a-7f2ed5b3e58f");
    private final UUID p_mcu_cmd_UUID =  UUID.fromString("0000ad02-8e22-4541-9d4c-21edae82ed19");
    private final UUID p_mcu_data_UUID = UUID.fromString("0000ad03-8e22-4541-9d4c-21edae82ed19");
    private final UUID mcu_p_CPS_UUID =  UUID.fromString("0000ad04-8e22-4541-9d4c-21edae82ed19");
    private final UUID mcu_p_data_UUID = UUID.fromString("0000ad05-8e22-4541-9d4c-21edae82ed19");
    private final UUID mcu_p_spcb_UUID = UUID.fromString("0000ad06-8e22-4541-9d4c-21edae82ed19");

    private Handler bleHandler;
    private final Handler.Callback bleHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 10) {
                Intent intent = new Intent("local_broadcast_connection_state");
                if (spectrometer == null)
                    intent.putExtra("connected", 0);
                else {
                    intent.putExtra("connected", 1);
                    intent.putExtra("name", spectrometer.getName());
                    intent.putExtra("addr", spectrometer.getAddress());
                    //intent.putExtra("state", spectrometer.getState());
                }
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                bleHandler.sendEmptyMessageDelayed(10, 1000);
                return true;
            }
            if (msg.what == 12) {
                ArrayList<BluetoothPeripheral> devList = new ArrayList<>(bleDeviceList.values());
                Iterator<BluetoothPeripheral> iter = devList.iterator();
                int counter = 0;
                Intent intent = new Intent("local_broadcast_device_list_data");
                //intent.putExtra("data_type", "device_list_data");
                intent.putExtra("device_list_data", devList.size());
                while (iter.hasNext()) {
                    BluetoothPeripheral device = iter.next();
                    intent.putExtra(counter + "_1", device.getAddress())
                        .putExtra(counter + "_2", device.getName());
                    counter++;
                }
                //!!!!!!!!!!!!!!!!!!!!!
                bleDeviceList.clear();
                //!!!!!!!!!!!!!!!!!!!!!
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                if (spectrometer == null)
                    bleHandler.sendEmptyMessageDelayed(12, 3005);
                return true;
            }
            if (msg.what == 13) { // UNUSED
                if (bleManager.isScanning())
                    Toast.makeText(getApplicationContext(), "BLE scanning", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getApplicationContext(), "BLE isn't scanning", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        }
    };
    private HashMap<String, BluetoothPeripheral> bleDeviceList;
    private BluetoothCentralManager bleManager; // manage connections, receive advertising packets
    private BluetoothPeripheral spectrometer = null; // device  (with gatt service role), that will be connected to phone
    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallbackmc = new BluetoothCentralManagerCallback() {
        @Override
        public void onConnectedPeripheral(@NonNull BluetoothPeripheral peripheral) {
            bleHandler.removeMessages(12);
            spectrometer = peripheral;
            if (!spectrometer.requestMtu(256))
                Toast.makeText(getApplicationContext(), "Request MTU error", Toast.LENGTH_SHORT).show();
            if (!spectrometer.setNotify(service_UUID, mcu_p_CPS_UUID, true)
                || !spectrometer.setNotify(service_UUID, mcu_p_data_UUID, true)
                || !spectrometer.setNotify(service_UUID, mcu_p_spcb_UUID, true))
                Toast.makeText(getApplicationContext(), "Notify enabling error", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onConnectionFailed(@NonNull BluetoothPeripheral peripheral, @NonNull HciStatus status) {
            spectrometer = null;
            Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_LONG).show();
            //super.onConnectionFailed(peripheral, status);
        }

        @Override
        public void onDisconnectedPeripheral(@NonNull BluetoothPeripheral peripheral, @NonNull HciStatus status) {
            //if (peripheral == spectrometer)
                //spectrometer = null;
            spectrometer = null;
            List<ScanFilter> _scanFilters = new ArrayList<>();
            ScanFilter _sf1 = new ScanFilter.Builder()
                    .setManufacturerData(0xFFFF, new byte[] {(byte)0xCA, (byte)0xFE}).build();
            _scanFilters.add(_sf1);
            bleManager.scanForPeripheralsUsingFilters(_scanFilters); // start scanning
            //super.onDisconnectedPeripheral(peripheral, status);
        }

        @Override
        public void onDiscoveredPeripheral(@NonNull BluetoothPeripheral peripheral, @NonNull ScanResult scanResult) {
            bleDeviceList.put(peripheral.getAddress(), peripheral);
            //bleHandler.removeMessages(13);
            //Toast.makeText(getApplicationContext(), peripheral.getName() + " " + peripheral.getAddress(), Toast.LENGTH_SHORT).show();
            //super.onDiscoveredPeripheral(peripheral, scanResult);
        }

        @Override
        public void onScanFailed(@NonNull ScanFailure scanFailure) {
            Toast.makeText(getApplicationContext(), "onScanFailed", Toast.LENGTH_SHORT).show();
            //super.onScanFailed(scanFailure);
        }

        @Override
        public void onBluetoothAdapterStateChanged(int state) {
            super.onBluetoothAdapterStateChanged(state);
        }
    };
    BluetoothPeripheralCallback bluetoothPeripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(@NonNull BluetoothPeripheral peripheral) {
            super.onServicesDiscovered(peripheral);
        }

        @Override
        public void onNotificationStateUpdate(@NonNull BluetoothPeripheral peripheral, @NonNull BluetoothGattCharacteristic characteristic, @NonNull GattStatus status) {
            super.onNotificationStateUpdate(peripheral, characteristic, status);
        }

        @Override
        public void onCharacteristicUpdate(@NonNull BluetoothPeripheral peripheral, @NonNull byte[] value, @NonNull BluetoothGattCharacteristic characteristic, @NonNull GattStatus status) {
            if (status != GattStatus.SUCCESS) {
                Toast.makeText(getApplicationContext(), "Char read/notify error", Toast.LENGTH_SHORT).show();
                return;
            }
            if (characteristic.getUuid().equals(mcu_p_spcb_UUID)) {
                Intent intent = new Intent("local_broadcast_mcu_p_spcb")
                        .putExtra("data", value);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
            else if (characteristic.getUuid().equals(mcu_p_CPS_UUID)) {
                Intent intent = new Intent("local_broadcast_mcu_p_cps")
                        .putExtra("data", value);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
            else if (characteristic.getUuid().equals(mcu_p_data_UUID)) {
                Intent intent = new Intent("local_broadcast_mcu_p_data")
                        .putExtra("data", value);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        }

        @Override
        public void onCharacteristicWrite(@NonNull BluetoothPeripheral peripheral, @NonNull byte[] value, @NonNull BluetoothGattCharacteristic characteristic, @NonNull GattStatus status) {
            super.onCharacteristicWrite(peripheral, value, characteristic, status);
        }

        @Override
        public void onDescriptorRead(@NonNull BluetoothPeripheral peripheral, @NonNull byte[] value, @NonNull BluetoothGattDescriptor descriptor, @NonNull GattStatus status) {
            super.onDescriptorRead(peripheral, value, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(@NonNull BluetoothPeripheral peripheral, @NonNull byte[] value, @NonNull BluetoothGattDescriptor descriptor, @NonNull GattStatus status) {
            super.onDescriptorWrite(peripheral, value, descriptor, status);
        }

        @Override
        public void onBondingStarted(@NonNull BluetoothPeripheral peripheral) {
            super.onBondingStarted(peripheral);
        }

        @Override
        public void onBondingSucceeded(@NonNull BluetoothPeripheral peripheral) {
            super.onBondingSucceeded(peripheral);
        }

        @Override
        public void onBondingFailed(@NonNull BluetoothPeripheral peripheral) {
            super.onBondingFailed(peripheral);
        }

        @Override
        public void onBondLost(@NonNull BluetoothPeripheral peripheral) {
            super.onBondLost(peripheral);
        }

        @Override
        public void onReadRemoteRssi(@NonNull BluetoothPeripheral peripheral, int rssi, @NonNull GattStatus status) {
            super.onReadRemoteRssi(peripheral, rssi, status);
        }

        @Override
        public void onMtuChanged(@NonNull BluetoothPeripheral peripheral, int mtu, @NonNull GattStatus status) {
            //super.onMtuChanged(peripheral, mtu, status);
            //Toast.makeText(getApplicationContext(), "MTU size: " + mtu, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPhyUpdate(@NonNull BluetoothPeripheral peripheral, @NonNull PhyType txPhy, @NonNull PhyType rxPhy, @NonNull GattStatus status) {
            super.onPhyUpdate(peripheral, txPhy, rxPhy, status);
        }

        @Override
        public void onConnectionUpdated(@NonNull BluetoothPeripheral peripheral, int interval, int latency, int timeout, @NonNull GattStatus status) {
            super.onConnectionUpdated(peripheral, interval, latency, timeout, status);
        }
    };
    //******</BLE MODULES>*******

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = new ServiceNotificationManager(this);

        bleDeviceList = new HashMap<>();
        bleHandler = new Handler(bleHandlerCallback);
        bleManager = new BluetoothCentralManager(this, bluetoothCentralManagerCallbackmc, new Handler(Looper.getMainLooper()));
        startForeground(ServiceNotificationManager.CONSTANT_NOTIFICATION_ID, notificationManager.getConstantNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int inputModule  = intent.getIntExtra(IntentToServiceBuilder.KEY_MODULE, IntentToServiceBuilder.MODULE_ID_NONE);
        int inputCommand = intent.getIntExtra(IntentToServiceBuilder.KEY_COMMAND, IntentToServiceBuilder.COMMAND_ID_NONE);
        String data      = intent.getStringExtra(IntentToServiceBuilder.KEY_DATA);

        if (inputModule == IntentToServiceBuilder.MODULE_ID_SERVICE) {
            switch (inputCommand) {
                case IntentToServiceBuilder.COMMAND_ID_SERVICE_START: // START COMMAND
                    notificationManager.updateConstantNotificationText(ServiceNotificationManager.CONSTANT_NOTIFICATION_ID, data);

                    if (!bleManager.isBluetoothEnabled()) {
                        Toast.makeText(this, "Bluetooth is not turned on", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        List<ScanFilter> _scanFilters = new ArrayList<>();
                        ScanFilter _sf1 = new ScanFilter.Builder()
                                .setManufacturerData(0xFFFF, new byte[] {(byte)0xCA, (byte)0xFE}).build();
                        _scanFilters.add(_sf1);
                        bleManager.scanForPeripheralsUsingFilters(_scanFilters); // start scanning
                        if (!bleHandler.hasMessages(10)) {
                            bleHandler.sendEmptyMessageDelayed(10, 1000); // every 1 second send broadcast message to main activity
                        }
                        if (!bleHandler.hasMessages(12)) {
                            bleHandler.sendEmptyMessageDelayed(12, 3005); // every 2 second send broadcast message to main activity
                        }
                        //if (!bleHandler.hasMessages(13))
                            //bleHandler.sendEmptyMessageDelayed(13, 10000);
                        //bleHandler.sendEmptyMessageDelayed(13, 10000); // every 10 second delete all bleDeviceList data
                    }
                    break;

                case IntentToServiceBuilder.COMMAND_ID_SERVICE_STOP:
                    stopForeground(true);
                    stopSelf();
                    break;
                case IntentToServiceBuilder.COMMAND_ID_SERVICE_TEST:
                    Intent InT = new Intent("local_broadcast_manager_string");
                    InT.putExtra("data", 228);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(InT);
                    break;
                default:
                    Toast.makeText(this, "Unsupproted command id in service module", Toast.LENGTH_LONG).show();
                    break;
            }
        }
        else if (inputModule == IntentToServiceBuilder.MODULE_ID_BLUETOOTH) {
            switch (inputCommand) {
                case IntentToServiceBuilder.COMMAND_ID_BLUETOOTH_START:
                    break;
                case IntentToServiceBuilder.COMMAND_ID_BLUETOOTH_CONNECT:
                    try {
                        BluetoothPeripheral connectTo = bleDeviceList.get(data);
                        if (connectTo != null) {
                            bleManager.stopScan();
                            bleManager.connectPeripheral(connectTo, bluetoothPeripheralCallback);
                        }
                    }
                    catch ( ClassCastException | NullPointerException ex) {
                        Toast.makeText(getApplicationContext(), "Can't get device from device list (service)", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
        else if (inputModule == IntentToServiceBuilder.MODULE_ID_DEVICE) {
            switch (inputCommand) {
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_START_IMPULSE_PROCESSING:
                    sendCmd(0x0001);
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_STOP_IMPULSE_PROCESSING:
                    sendCmd(0x0002);
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_SAVE_SPECTROGRAM_AS_TESTCSV:
                    sendCmd(0x0003);
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_START_CPS_WRITING:
                    sendCmd(0x0004);
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_STOP_CPS_WRITING:
                    sendCmd(0x0005);
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_FS_INIT:
                    sendCmd(0x0006);
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_INVERSE_LED:
                    sendCmd(0x0007);
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_GET_48_VALS_FROM_SPECT:
                    {
                        try {
                            int index = Integer.parseInt(data);
                            sendCmd_uint16_t(0x0008, index);
                        }
                        catch (NumberFormatException ex) {
                            Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_CLEAR_SPECTROGRAM:
                    sendCmd(0x0009);
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_CPS_TIME:
                    try {
                        int time = Integer.parseInt(data);
                        sendCmd_uint16_t(0x000A, time);
                    }
                    catch (NumberFormatException ex) {
                        Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                    }
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_SAVE_SPECTR_WITH_SET_FILENAME:
                    try {
                        if (data.length() > 64) {
                            Toast.makeText(getApplicationContext(), "File string length should not exceed 64 chars", Toast.LENGTH_LONG).show();
                            break;
                        }
                        //byte[] fileNameAsBytes = data.getBytes(StandardCharsets.US_ASCII);
                        int len = data.length();
                        byte[] finalData = new byte[len + 3];
                        char c = '#';
                        finalData[0] = (byte)len;
                        finalData[1] = (byte)c;
                        for (int i = 0; i < len; i++)
                            finalData[2 + i] = (byte)data.charAt(i);
                        finalData[2 + len] = (byte)c;
                        if (sendData(0x000B, finalData) == 0x02)
                            Toast.makeText(getApplicationContext(), "p_mcu_data data len > 235", Toast.LENGTH_LONG).show();

                    }
                    catch (Exception ex) {
                        Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                    }
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_MEASUREMENT_TIME:
                    try {
                        int value = Integer.parseInt(data);
                        byte[] convertedValue = new byte[4];
                        convertedValue[0] = (byte)((int)value >> 24);
                        convertedValue[1] = (byte)((int)value >> 16);
                        convertedValue[2] = (byte)((int)value >> 8);
                        convertedValue[3] = (byte)value;
                        sendCmd(0x000C, convertedValue);
                    }
                    catch (Exception ex) {
                        Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                    }
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_DEVICE_ADV_NUMBER:
                    try {
                        if (data.length() != 2) {
                            Toast.makeText(getApplicationContext(), "In device number should be only 2 numbers", Toast.LENGTH_LONG).show();
                            break;
                        }
                        byte[] dataAsBytes = data.getBytes(StandardCharsets.US_ASCII);
                        sendCmd(0x000D, dataAsBytes);
                    }
                    catch (Exception ex) {
                        Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                    }
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_DMA_BUF_SIZE:
                    try {
                        int value = Integer.parseInt(data);
                        sendCmd_uint16_t(0x000E, value);
                    }
                    catch (Exception ex) {
                        Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                    }
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_MIN_FILTRATION_VALUE:
                    try {
                        int value = Integer.parseInt(data);
                        sendCmd_uint16_t(0x000F, value);
                    }
                    catch (Exception ex) {
                        Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                    }
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_SET_MAX_FILTRATION_VALUE:
                    try {
                        int value = Integer.parseInt(data);
                        sendCmd_uint16_t(0x0010, value);
                    }
                    catch (Exception ex) {
                        Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                    }
                    break;
                case IntentToServiceBuilder.COMMAND_ID_DEVICE_GET_DEVICE_STATUS:
                    sendCmd(0x0011);
                    break;
            }
        }

        return START_NOT_STICKY;
    }


    private void sendCmd(int cmd) {
        if (spectrometer == null)
            return;
        byte[] value = new byte[20];
        value[0] = (byte)0xCA;
        value[1] = (byte)((int)cmd >> 8);
        value[2] = (byte)cmd;
        value[3] = (byte)0xFE;
        spectrometer.writeCharacteristic(service_UUID, p_mcu_cmd_UUID, value, WriteType.WITHOUT_RESPONSE);
    }

    private void sendCmd(int cmd, @NonNull byte[] additionalData) {
        if (spectrometer == null)
            return;
        byte[] value = new byte[20];
        value[0] = (byte)0xCA;
        value[1] = (byte)((int)cmd >> 8);
        value[2] = (byte)cmd;
        value[3] = (byte)0xFE;

        int index = 4;
        for (byte adData : additionalData) {
            value[index] = adData;
            index = (index + 1) % 20;
        }
        spectrometer.writeCharacteristic(service_UUID, p_mcu_cmd_UUID, value, WriteType.WITHOUT_RESPONSE);
    }

    private void sendCmd_uint16_t(int cmd, int uint16_t_value) {
        if (spectrometer == null)
            return;
        byte[] value = new byte[20];
        value[0] = (byte)0xCA;
        value[1] = (byte)((int)cmd >> 8);
        value[2] = (byte)cmd;
        value[3] = (byte)0xFE;

        value[4] = (byte)((int)uint16_t_value >> 8);
        value[5] = (byte)uint16_t_value;
        spectrometer.writeCharacteristic(service_UUID, p_mcu_cmd_UUID, value, WriteType.WITHOUT_RESPONSE);
    }

    private int sendData(int cmd, @NonNull byte[] data) {
        if (spectrometer == null)
            return 0x01;
        byte[] value = new byte[data.length + 4];
        value[0] = (byte) 0xCA;
        value[1] = (byte) ((int) cmd >> 8);
        value[2] = (byte) cmd;
        value[3] = (byte) 0xFE;

        if (data.length > 235)
            return 0x02;
        System.arraycopy(data, 0, value, 4, data.length);
        spectrometer.writeCharacteristic(service_UUID, p_mcu_data_UUID, value, WriteType.WITHOUT_RESPONSE);

        return 0x00;
    }

    @Override
    public void onDestroy() {
        if (spectrometer != null)
            bleManager.cancelConnection(spectrometer);
        bleManager.close();
        super.onDestroy();
    }


















}
