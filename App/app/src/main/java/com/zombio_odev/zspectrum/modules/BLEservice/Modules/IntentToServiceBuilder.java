package com.zombio_odev.zspectrum.modules.BLEservice.Modules;

import android.content.Context;
import android.content.Intent;

public class IntentToServiceBuilder {
    //**************<CONSTANTS>**************** Единственное, что нужно дополнять - список констант
    //***********<MODULE>************
    public static final String KEY_MODULE = "module";
    public static final int MODULE_ID_NONE = -1;
    //-----------------------------------------
    public static final int MODULE_ID_SERVICE = 0;
    public static final int MODULE_ID_BLUETOOTH = 1;
    public static final int MODULE_ID_DEVICE = 2;
    //***********</MODULE>***************

    //************<COMMAND>**************
    public static final String KEY_COMMAND = "command";
    public static final int COMMAND_ID_NONE = -1;
    //-----------------------------------------------
    //************<SERVICE_IDs>*****************
    public static final int COMMAND_ID_SERVICE_START = 0;
    public static final int COMMAND_ID_SERVICE_STOP = 1;
    public static final int COMMAND_ID_SERVICE_TEST = 99;
    //************</SERVICE_IDs>****************
    //-----------------------------------------------
    //***********<BLUETOOTH_IDs>****************
    public static final int COMMAND_ID_BLUETOOTH_START = 0;
    public static final int COMMAND_ID_BLUETOOTH_CONNECT = 1;
    //***********</BLUETOOTH_IDs>***************
    //-----------------------------------------------
    //***********<DEVICE_IDs>****************
    public static final int COMMAND_ID_DEVICE_START_IMPULSE_PROCESSING = 1;
    public static final int COMMAND_ID_DEVICE_STOP_IMPULSE_PROCESSING = 2;
    public static final int COMMAND_ID_DEVICE_SAVE_SPECTROGRAM_AS_TESTCSV = 3;
    public static final int COMMAND_ID_DEVICE_START_CPS_WRITING = 4;
    public static final int COMMAND_ID_DEVICE_STOP_CPS_WRITING = 5;
    public static final int COMMAND_ID_DEVICE_FS_INIT = 6;
    public static final int COMMAND_ID_DEVICE_INVERSE_LED = 7;
    public static final int COMMAND_ID_DEVICE_GET_48_VALS_FROM_SPECT = 8;
    public static final int COMMAND_ID_DEVICE_CLEAR_SPECTROGRAM = 9;

    public static final int COMMAND_ID_DEVICE_SET_CPS_TIME = 0x000A;
    public static final int COMMAND_ID_DEVICE_SAVE_SPECTR_WITH_SET_FILENAME = 0x000B;
    public static final int COMMAND_ID_DEVICE_SET_MEASUREMENT_TIME = 0x000C;
    public static final int COMMAND_ID_DEVICE_SET_DEVICE_ADV_NUMBER = 0x000D;
    public static final int COMMAND_ID_DEVICE_SET_DMA_BUF_SIZE = 0x000E;
    public static final int COMMAND_ID_DEVICE_SET_MIN_FILTRATION_VALUE = 0x000F;
    public static final int COMMAND_ID_DEVICE_SET_MAX_FILTRATION_VALUE = 0x0010;
    public static final int COMMAND_ID_DEVICE_GET_DEVICE_STATUS = 0x0011;
    //***********<DEVICE_IDs>****************
    //-----------------------------------------------
    //************</COMMAND>****************

    //**************<DATA>******************
    public static final String KEY_DATA = "data";
    //**************</DATA>*****************
    //**************</CONSTANTS>****************

    private Context sourceContext = null;
    private Class<?> ServiceClass;
    private int module = MODULE_ID_NONE;
    private int command = COMMAND_ID_NONE;
    private String data = "";

    public static IntentToServiceBuilder getInstance(Context context, Class<?> ServiceClass) {
        return new IntentToServiceBuilder(context, ServiceClass);
    }

    public IntentToServiceBuilder(Context context, Class<?> ServiceClass) {
        this.sourceContext = context;
        this.ServiceClass = ServiceClass;
    }

    public IntentToServiceBuilder setModule(int module_id) {
        this.module = module_id;
        return this;
    }

    public IntentToServiceBuilder setCommand(int command_id) {
        this.command = command_id;
        return this;
    }

    public IntentToServiceBuilder setData(String data) {
        this.data = data;
        return this;
    }

    public Intent build() {
        return new Intent(sourceContext, ServiceClass)
                .putExtra(KEY_MODULE, module)
                .putExtra(KEY_COMMAND, command)
                .putExtra(KEY_DATA, data);
    }


}
