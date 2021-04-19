/*
 *   Copyright (c) 2021 Martijn van Welie
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 *
 */

package com.welie.blessed;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.zombio_odev.zspectrum.App;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

//import timber.log.Timber;

import static com.welie.blessed.BluetoothBytesParser.bytes2String;
import static com.welie.blessed.BluetoothBytesParser.mergeArrays;

/**
 * This class represent a peripheral running on the local phone
 */
@SuppressWarnings("UnusedReturnValue")
public class BluetoothPeripheralManager {

    protected static final UUID CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final String CONTEXT_IS_NULL = "context is null";
    private static final String BLUETOOTH_MANAGER_IS_NULL = "BluetoothManager is null";
    private static final String SERVICE_IS_NULL = "service is null";
    private static final String CHARACTERISTIC_IS_NULL = "characteristic is null";
    private static final String DEVICE_IS_NULL = "device is null";
    private static final String CHARACTERISTIC_VALUE_IS_NULL = "characteristic value is null";
    private static final String CENTRAL_IS_NULL = "central is null";
    private static final String ADDRESS_IS_NULL = "address is null";

    private @NonNull final Context context;
    private @NonNull final Handler mainHandler = new Handler(Looper.getMainLooper());
    private @NonNull final BluetoothManager bluetoothManager;
    private @NonNull final BluetoothAdapter bluetoothAdapter;
    private @NonNull final BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private @NonNull final BluetoothGattServer bluetoothGattServer;
    private @NonNull final BluetoothPeripheralManagerCallback callback;
    protected @NonNull final Queue<Runnable> commandQueue = new ConcurrentLinkedQueue<>();
    private @NonNull final HashMap<BluetoothGattCharacteristic, byte[]> writeLongCharacteristicTemporaryBytes = new HashMap<>();
    private @NonNull final HashMap<BluetoothGattDescriptor, byte[]> writeLongDescriptorTemporaryBytes = new HashMap<>();
    private @NonNull final Map<String, BluetoothCentral> connectedCentralsMap = new ConcurrentHashMap<>();
    private @Nullable BluetoothGattCharacteristic currentNotifyCharacteristic = null;
    private @NonNull byte[] currentNotifyValue = new byte[0];
    private volatile boolean commandQueueBusy = false;

    protected final BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothDevice device, final int status, final int newState) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // Call connect() even though we are already connected
                    // It basically tells Android we will really use this connection
                    // If we don't do this, then cancelConnection won't work
                    // See https://issuetracker.google.com/issues/37127644
                    if (connectedCentralsMap.containsKey(device.getAddress())) {
                        return;
                    } else {
                        // This will lead to onConnectionStateChange be called again
                        bluetoothGattServer.connect(device, false);
                    }

                    handleDeviceConnected(device);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // Deal is double disconnect messages
                    if (!connectedCentralsMap.containsKey(device.getAddress())) return;

                    handleDeviceDisconnected(device);
                }
            } else {
                //Timber.i("Device '%s' disconnected with status %d", device.getName(), status);
                Log.d(App.BLESSING_LOG_STRING, "Device " + device.getName() + " disconnected with status " + status);
                handleDeviceDisconnected(device);
            }
        }

        private void handleDeviceConnected(@NonNull final BluetoothDevice device) {
            //Timber.i("Central '%s' (%s) connected", device.getName(), device.getAddress());
            Log.d(App.BLESSING_LOG_STRING, "Central " + device.getName() + " " + device.getAddress() + " connected");
            final BluetoothCentral bluetoothCentral = new BluetoothCentral(device.getAddress(), device.getName());
            connectedCentralsMap.put(bluetoothCentral.getAddress(), bluetoothCentral);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onCentralConnected(bluetoothCentral);
                }
            });
        }

        private void handleDeviceDisconnected(@NonNull final BluetoothDevice device) {
            final BluetoothCentral bluetoothCentral = getCentral(device);
            //Timber.i("Central '%s' (%s) disconnected", bluetoothCentral.getName(), bluetoothCentral.getAddress());
            Log.d(App.BLESSING_LOG_STRING, "Central " + device.getName() + " " + device.getAddress() + " disconnected");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onCentralDisconnected(bluetoothCentral);
                }
            });
            removeCentral(device);
        }

        @Override
        public void onServiceAdded(final int status, @NonNull final BluetoothGattService service) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onServiceAdded(GattStatus.fromValue(status), service);
                }
            });
            completedCommand();
        }

        @Override
        public void onCharacteristicReadRequest(@NonNull final BluetoothDevice device, final int requestId, final int offset, @NonNull final BluetoothGattCharacteristic characteristic) {
            //Timber.i("read request for characteristic <%s> with offset %d", characteristic.getUuid(), offset);
            Log.d(App.BLESSING_LOG_STRING, "read request for characteristic <" + characteristic.getUuid().toString() + "> with offset " + offset);

            final BluetoothCentral bluetoothCentral = getCentral(device);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Call onCharacteristic before any responses are sent, even if it is a long read
                    if (offset == 0) {
                        callback.onCharacteristicRead(bluetoothCentral, characteristic);
                    }

                    // If data is longer than MTU - 1, cut the array. Only ATT_MTU - 1 bytes can be sent in Long Read.
                    final byte[] value = copyOf(nonnullOf(characteristic.getValue()), offset, bluetoothCentral.getCurrentMtu() - 1);

                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            });
        }

        @Override
        public void onCharacteristicWriteRequest(@NonNull final BluetoothDevice device, final int requestId, @NonNull final BluetoothGattCharacteristic characteristic, final boolean preparedWrite, final boolean responseNeeded, final int offset, @Nullable final byte[] value) {
            //Timber.i("write characteristic %s request <%s> offset %d for <%s>", responseNeeded ? "WITH_RESPONSE" : "WITHOUT_RESPONSE", bytes2String(value), offset, characteristic.getUuid());
            Log.d(App.BLESSING_LOG_STRING, "write characteristic " + (responseNeeded ? "WITH_RESPONSE" : "WITHOUT_RESPONSE") + " request <" + bytes2String(value) + "> offset " + offset + " for <" + characteristic.getUuid() + ">");

            final byte[] safeValue = nonnullOf(value);
            final BluetoothCentral bluetoothCentral = getCentral(device);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    GattStatus status = GattStatus.SUCCESS;
                    if (!preparedWrite) {
                        status = callback.onCharacteristicWrite(bluetoothCentral, characteristic, safeValue);

                        if (status == GattStatus.SUCCESS) {
                            characteristic.setValue(safeValue);
                        }
                    } else {
                        if (offset == 0) {
                            writeLongCharacteristicTemporaryBytes.put(characteristic, safeValue);
                        } else {
                            final byte[] temporaryBytes = writeLongCharacteristicTemporaryBytes.get(characteristic);
                            if (temporaryBytes != null && offset == temporaryBytes.length) {
                                writeLongCharacteristicTemporaryBytes.put(characteristic, mergeArrays(temporaryBytes, safeValue));
                            } else {
                                status = GattStatus.INVALID_OFFSET;
                            }
                        }
                    }

                    if (responseNeeded) {
                        bluetoothGattServer.sendResponse(device, requestId, status.value, offset, safeValue);
                    }
                }
            });
        }

        @Override
        public void onDescriptorReadRequest(@NonNull final BluetoothDevice device, final int requestId, final int offset, @NonNull final BluetoothGattDescriptor descriptor) {
            //Timber.i("read request for descriptor <%s> with offset %d", descriptor.getUuid(), offset);
            Log.d(App.BLESSING_LOG_STRING, "read request for descriptor <" + descriptor.getUuid() + "> with offset " + offset);

            final BluetoothCentral bluetoothCentral = getCentral(device);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Call onDescriptorRead before any responses are sent, even if it is a long read
                    if (offset == 0) {
                        callback.onDescriptorRead(bluetoothCentral, descriptor);
                    }

                    // If data is longer than MTU - 1, cut the array. Only ATT_MTU - 1 bytes can be sent in Long Read.
                    final byte[] value = copyOf(nonnullOf(descriptor.getValue()), offset, bluetoothCentral.getCurrentMtu() - 1);

                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            });
        }

        @Override
        public void onDescriptorWriteRequest(@NonNull final BluetoothDevice device, final int requestId, @NonNull final BluetoothGattDescriptor descriptor, final boolean preparedWrite, final boolean responseNeeded, final int offset, @Nullable final byte[] value) {
            final byte[] safeValue = nonnullOf(value);
            final BluetoothGattCharacteristic characteristic = Objects.requireNonNull(descriptor.getCharacteristic(), "Descriptor does not have characteristic");

            //Timber.i("write descriptor %s request <%s> offset %d for <%s>", responseNeeded ? "WITH_RESPONSE" : "WITHOUT_RESPONSE", bytes2String(value), offset, descriptor.getUuid());
            Log.d(App.BLESSING_LOG_STRING, "write descriptor " + (responseNeeded ? "WITH_RESPONSE" : "WITHOUT_RESPONSE") + " request <" + bytes2String(value) + "> offset " + offset + " for <" + descriptor.getUuid() + ">");

            final BluetoothCentral bluetoothCentral = getCentral(device);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    GattStatus status = GattStatus.SUCCESS;
                    if (descriptor.getUuid().equals(CCC_DESCRIPTOR_UUID)) {
                        status = checkCccDescriptorValue(safeValue, characteristic);
                    } else {
                        if (!preparedWrite) {
                            // Ask callback if value is ok or not
                            status = callback.onDescriptorWrite(bluetoothCentral, descriptor, safeValue);
                        } else {
                            if (offset == 0) {
                                writeLongDescriptorTemporaryBytes.put(descriptor, safeValue);
                            } else {
                                final byte[] temporaryBytes = writeLongDescriptorTemporaryBytes.get(descriptor);
                                if (temporaryBytes != null && offset == temporaryBytes.length) {
                                    writeLongDescriptorTemporaryBytes.put(descriptor, mergeArrays(temporaryBytes, safeValue));
                                } else {
                                    status = GattStatus.INVALID_OFFSET;
                                }
                            }
                        }
                    }

                    if (status == GattStatus.SUCCESS && !preparedWrite) {
                        descriptor.setValue(safeValue);
                    }

                    if (responseNeeded) {
                        bluetoothGattServer.sendResponse(device, requestId, status.value, offset, safeValue);
                    }

                    if (status == GattStatus.SUCCESS && descriptor.getUuid().equals(CCC_DESCRIPTOR_UUID)) {
                        if (Arrays.equals(safeValue, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                                || Arrays.equals(safeValue, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                            //Timber.i("notifying enabled for <%s>", characteristic.getUuid());
                            Log.d(App.BLESSING_LOG_STRING, "notifying enabled for <" + characteristic.getUuid() + ">");
                            callback.onNotifyingEnabled(bluetoothCentral, characteristic);
                        } else {
                            //Timber.i("notifying disabled for <%s>", characteristic.getUuid());
                            Log.d(App.BLESSING_LOG_STRING, "notifying disabled for <" + characteristic.getUuid() + ">");
                            callback.onNotifyingDisabled(bluetoothCentral, characteristic);
                        }
                    }
                }
            });
        }

        // Check value to see if it is valid and if matches the characteristic properties
        private GattStatus checkCccDescriptorValue(@NonNull final byte[] safeValue, @NonNull final BluetoothGattCharacteristic characteristic) {
            GattStatus status = GattStatus.SUCCESS;

            if (safeValue.length != 2) {
                status = GattStatus.INVALID_ATTRIBUTE_VALUE_LENGTH;
            } else if (!(Arrays.equals(safeValue, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                    || Arrays.equals(safeValue, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    || Arrays.equals(safeValue, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE))) {
                status = GattStatus.VALUE_NOT_ALLOWED;
            } else if (!supportsIndicate(characteristic) && Arrays.equals(safeValue, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                status = GattStatus.REQUEST_NOT_SUPPORTED;
            } else if (!supportsNotify(characteristic) && Arrays.equals(safeValue, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                status = GattStatus.REQUEST_NOT_SUPPORTED;
            }
            return status;
        }

        @Override
        public void onExecuteWrite(@NonNull final BluetoothDevice device, final int requestId, final boolean execute) {
            final BluetoothCentral bluetoothCentral = getCentral(device);
            if (execute) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        GattStatus status = GattStatus.SUCCESS;
                        if (!writeLongCharacteristicTemporaryBytes.isEmpty()) {
                            final BluetoothGattCharacteristic characteristic = writeLongCharacteristicTemporaryBytes.keySet().iterator().next();
                            if (characteristic != null) {
                                // Ask callback if value is ok or not
                                status = callback.onCharacteristicWrite(bluetoothCentral, characteristic, writeLongCharacteristicTemporaryBytes.get(characteristic));

                                if (status == GattStatus.SUCCESS) {
                                    characteristic.setValue(writeLongCharacteristicTemporaryBytes.get(characteristic));
                                    writeLongCharacteristicTemporaryBytes.clear();
                                }
                            }
                        } else if (!writeLongDescriptorTemporaryBytes.isEmpty()) {
                            final BluetoothGattDescriptor descriptor = writeLongDescriptorTemporaryBytes.keySet().iterator().next();
                            if (descriptor != null) {
                                // Ask callback if value is ok or not
                                status = callback.onDescriptorWrite(bluetoothCentral, descriptor, writeLongDescriptorTemporaryBytes.get(descriptor));

                                if (status == GattStatus.SUCCESS) {
                                    descriptor.setValue(writeLongDescriptorTemporaryBytes.get(descriptor));
                                    writeLongDescriptorTemporaryBytes.clear();
                                }
                            }
                        }
                        bluetoothGattServer.sendResponse(device, requestId, status.value, 0, null);
                    }
                });
            } else {
                // Long write was cancelled, clean up already received bytes
                writeLongCharacteristicTemporaryBytes.clear();
                writeLongDescriptorTemporaryBytes.clear();
                bluetoothGattServer.sendResponse(device, requestId, GattStatus.SUCCESS.value, 0, null);
            }
        }

        @Override
        public void onNotificationSent(@NonNull final BluetoothDevice device, final int status) {
            final BluetoothCentral bluetoothCentral = getCentral(device);
            final @NonNull BluetoothGattCharacteristic characteristic = Objects.requireNonNull(currentNotifyCharacteristic);
            final @NonNull byte[] value = Objects.requireNonNull(currentNotifyValue);
            currentNotifyValue = new byte[0];
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onNotificationSent(bluetoothCentral, value, characteristic, GattStatus.fromValue(status));
                }
            });
            completedCommand();
        }

        @Override
        public void onMtuChanged(@NonNull final BluetoothDevice device, final int mtu) {
            //Timber.i("new MTU: %d", mtu);
            Log.d(App.BLESSING_LOG_STRING, "new MTU: " + mtu);
            BluetoothCentral bluetoothCentral = getCentral(device);
            bluetoothCentral.setCurrentMtu(mtu);
        }

        @Override
        public void onPhyUpdate(@NonNull final BluetoothDevice device, final int txPhy, final int rxPhy, final int status) {
            super.onPhyUpdate(device, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(@NonNull final BluetoothDevice device, final int txPhy, final int rxPhy, final int status) {
            super.onPhyRead(device, txPhy, rxPhy, status);
        }
    };

    protected final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(@NonNull final AdvertiseSettings settingsInEffect) {
            //Timber.i("advertising started");
            Log.d(App.BLESSING_LOG_STRING, "advertising started");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onAdvertisingStarted(settingsInEffect);
                }
            });
        }

        @Override
        public void onStartFailure(final int errorCode) {
            final AdvertiseError advertiseError = AdvertiseError.fromValue(errorCode);
            //Timber.e("advertising failed with error '%s'", advertiseError);
            Log.d(App.BLESSING_LOG_STRING, "advertising failed with error " + advertiseError);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onAdvertiseFailure(advertiseError);
                }
            });
        }
    };

    protected void onAdvertisingStopped() {
        //Timber.i("advertising stopped");
        Log.d(App.BLESSING_LOG_STRING, "advertising stopped");
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onAdvertisingStopped();
            }
        });
    }

    /**
     * Create a BluetoothPeripheralManager
     *
     * @param context the application context
     * @param bluetoothManager a valid BluetoothManager
     * @param callback an instance of BluetoothPeripheralManagerCallback where the callbacks will be handled
     */
    public BluetoothPeripheralManager(@NonNull final Context context, @NonNull final BluetoothManager bluetoothManager, @NonNull final BluetoothPeripheralManagerCallback callback) {
        this.context = Objects.requireNonNull(context, CONTEXT_IS_NULL);
        this.callback = Objects.requireNonNull(callback, "Callback is null");
        this.bluetoothManager = Objects.requireNonNull(bluetoothManager, BLUETOOTH_MANAGER_IS_NULL);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        this.bluetoothGattServer = bluetoothManager.openGattServer(context, bluetoothGattServerCallback);

        // Register for broadcasts on BluetoothAdapter state change
        final IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(adapterStateReceiver, filter);
    }

    /**
     * Close the BluetoothPeripheralManager
     *
     * Application should call this method as early as possible after it is done with
     * this BluetoothPeripheralManager.
     *
     */
    public void close() {
        stopAdvertising();
        context.unregisterReceiver(adapterStateReceiver);
        bluetoothGattServer.close();
    }

    /**
     * Start Bluetooth LE Advertising. The {@code advertiseData} will be broadcasted if the
     * operation succeeds. The {@code scanResponse} is returned when a scanning device sends an
     * active scan request. This method returns immediately, the operation status is delivered
     * through {@link BluetoothPeripheralManagerCallback#onAdvertisingStarted(AdvertiseSettings)} or {@link BluetoothPeripheralManagerCallback#onAdvertiseFailure(AdvertiseError)}.
     *
     * @param settings the AdvertiseSettings
     * @param advertiseData the AdvertiseData
     * @param scanResponse the ScanResponse
     */
    public void startAdvertising(@NonNull final AdvertiseSettings settings, @NonNull final AdvertiseData advertiseData, @NonNull final AdvertiseData scanResponse) {
        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            //Timber.e("device does not support advertising");
            Log.d(App.BLESSING_LOG_STRING, "device does not support advertising");
        } else {
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback);
        }
    }

    /**
     * Stop advertising
     */
    public void stopAdvertising() {
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        onAdvertisingStopped();
    }

    /**
     * Add a service to the peripheral
     *
     * <p>Once a service has been added to the list, the service and its
     * included characteristics will be provided by the local peripheral.
     *
     * <p>If the local peripheral has already exposed services when this function
     * is called, a service update notification will be sent to all clients.
     *
     * A callback on {@link BluetoothPeripheralManagerCallback#onServiceAdded)} will be received when this operation has completed
     *
     * @param service the service to add
     * @return true if the operation was enqueued, false otherwise
     */
    public boolean add(@NonNull final BluetoothGattService service) {
        Objects.requireNonNull(service, SERVICE_IS_NULL);

        final boolean result = commandQueue.add(new Runnable() {
            @Override
            public void run() {
                if (!bluetoothGattServer.addService(service)) {
                    //Timber.e("adding service %s failed", service.getUuid());
                    Log.d(App.BLESSING_LOG_STRING, "adding service " + service.getUuid() + " failed");
                    completedCommand();
                }
            }
        });

        if (result) {
            nextCommand();
        } else {
            //Timber.e("could not enqueue add service command");
            Log.d(App.BLESSING_LOG_STRING, "could not enqueue add service command");
        }
        return result;
    }

    /**
     * Remove a service
     *
     * @param service the service to remove
     * @return true if the service was removed, otherwise false
     */
    public boolean remove(@NonNull final BluetoothGattService service) {
        Objects.requireNonNull(service, SERVICE_IS_NULL);

        return bluetoothGattServer.removeService(service);
    }

    /**
     * Remove all services
     */
    public void removeAllServices() {
        bluetoothGattServer.clearServices();
    }

    /**
     * Get a list of the all advertised services of this peripheral
     *
     * @return a list of zero or more services
     */
    @NonNull
    public List<BluetoothGattService> getServices() {
        return bluetoothGattServer.getServices();
    }

    /**
     * Send a notification or indication that a local characteristic has been
     * updated
     *
     * <p>A notification or indication is sent to all remote centrals to signal
     * that the characteristic has been updated.
     *
     * @param characteristic the characteristic for which to send a notification
     * @return true if the operation was enqueued, otherwise false
     */
    public boolean notifyCharacteristicChanged(@NonNull final byte[] value, @NonNull final BluetoothGattCharacteristic characteristic) {
        Objects.requireNonNull(value, CHARACTERISTIC_VALUE_IS_NULL);
        Objects.requireNonNull(characteristic, CHARACTERISTIC_IS_NULL);

        if (doesNotSupportNotifying(characteristic)) return false;

        boolean result = true;
        for (BluetoothDevice device : getConnectedDevices()) {
            if (!notifyCharacteristicChanged(copyOf(value), device, characteristic)) {
                result = false;
            }
        }
        return result;
    }

    private boolean notifyCharacteristicChanged(@NonNull final byte[] value, @NonNull final BluetoothDevice bluetoothDevice, @NonNull final BluetoothGattCharacteristic characteristic) {
        Objects.requireNonNull(value, CHARACTERISTIC_VALUE_IS_NULL);
        Objects.requireNonNull(bluetoothDevice, DEVICE_IS_NULL);
        Objects.requireNonNull(characteristic, CHARACTERISTIC_IS_NULL);
        Objects.requireNonNull(characteristic.getValue(), CHARACTERISTIC_VALUE_IS_NULL);

        if (doesNotSupportNotifying(characteristic)) return false;

        final boolean confirm = supportsIndicate(characteristic);
        final boolean result = commandQueue.add(new Runnable() {
            @Override
            public void run() {
                currentNotifyValue = value;
                currentNotifyCharacteristic = characteristic;
                characteristic.setValue(value);
                if (!bluetoothGattServer.notifyCharacteristicChanged(bluetoothDevice, characteristic, confirm)) {
                    //Timber.e("notifying characteristic changed failed for <%s>", characteristic.getUuid());
                    Log.d(App.BLESSING_LOG_STRING, "notifying characteristic changed failed for <" + characteristic.getUuid() + ">");
                    BluetoothPeripheralManager.this.completedCommand();
                }
            }
        });

        if (result) {
            nextCommand();
        } else {
            //Timber.e("could not enqueue notify command");
            Log.d(App.BLESSING_LOG_STRING, "could not enqueue notify command");
        }
        return result;
    }

    /**
     * Cancel a connection to a Central
     *
     * @param bluetoothCentral the Central
     */
    public void cancelConnection(@NonNull final BluetoothCentral bluetoothCentral) {
        Objects.requireNonNull(bluetoothCentral, CENTRAL_IS_NULL);
        cancelConnection(bluetoothAdapter.getRemoteDevice(bluetoothCentral.getAddress()));
    }

    private void cancelConnection(@NonNull final BluetoothDevice bluetoothDevice) {
        Objects.requireNonNull(bluetoothDevice, DEVICE_IS_NULL);

        //Timber.i("cancelConnection with '%s' (%s)", bluetoothDevice.getName(), bluetoothDevice.getAddress());
        Log.d(App.BLESSING_LOG_STRING, "cancelConnection with " + bluetoothDevice.getName() + " ("+bluetoothDevice.getAddress()+")");
        bluetoothGattServer.cancelConnection(bluetoothDevice);
    }

    private @NonNull List<BluetoothDevice> getConnectedDevices() {
        return bluetoothManager.getConnectedDevices(BluetoothGattServer.GATT);
    }

    /**
     * Get the set of connected Centrals
     *
     * @return a set with zero or more connected Centrals
     */
    public @NonNull Set<BluetoothCentral> getConnectedCentrals() {
        Set<BluetoothCentral> bluetoothCentrals = new HashSet<>(connectedCentralsMap.values());
        return Collections.unmodifiableSet(bluetoothCentrals);
    }

    /**
     * The current command has been completed, move to the next command in the queue (if any)
     */
    private void completedCommand() {
        commandQueue.poll();
        commandQueueBusy = false;
        nextCommand();
    }

    /**
     * Execute the next command in the subscribe queue.
     * A queue is used because some calls have to be executed sequentially.
     */
    private void nextCommand() {
        synchronized (this) {
            // If there is still a command being executed, then bail out
            if (commandQueueBusy) return;

            // Check if there is something to do at all
            final Runnable bluetoothCommand = commandQueue.peek();
            if (bluetoothCommand == null) return;

            // Execute the next command in the queue
            commandQueueBusy = true;
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        bluetoothCommand.run();
                    } catch (Exception ex) {
                        //Timber.e(ex, "command exception");
                        Log.d(App.BLESSING_LOG_STRING, ex + "command exception");
                        BluetoothPeripheralManager.this.completedCommand();
                    }
                }
            });
        }
    }

    @Nullable
    public BluetoothCentral getCentral(@NonNull final String address) {
        Objects.requireNonNull(address, ADDRESS_IS_NULL);
        return connectedCentralsMap.get(address);
    }

    @NonNull
    private BluetoothCentral getCentral(@NonNull final BluetoothDevice device) {
        Objects.requireNonNull(device, DEVICE_IS_NULL);

        BluetoothCentral result = connectedCentralsMap.get(device.getAddress());
        if (result == null) {
            result = new BluetoothCentral(device.getAddress(), device.getName());
        }
        return result;
    }

    private void removeCentral(@NonNull final BluetoothDevice device) {
        Objects.requireNonNull(device, DEVICE_IS_NULL);

        connectedCentralsMap.remove(device.getAddress());
    }

    private final BroadcastReceiver adapterStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null) return;

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                handleAdapterState(state);
            }
        }
    };

    private void handleAdapterState(final int state) {
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                //Timber.d("bluetooth turned off");
                Log.d(App.BLESSING_LOG_STRING, "bluetooth turned off");
                cancelAllConnectionsWhenBluetoothOff();
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                //Timber.d("bluetooth turning off");
                Log.d(App.BLESSING_LOG_STRING, "bluetooth turning off");
                break;
            case BluetoothAdapter.STATE_ON:
                //Timber.d("bluetooth turned on");
                Log.d(App.BLESSING_LOG_STRING, "bluetooth turned on");
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                //Timber.d("bluetooth turning on");
                Log.d(App.BLESSING_LOG_STRING, "bluetooth turning on");
                break;
        }
    }

    private void cancelAllConnectionsWhenBluetoothOff() {
        final Set<BluetoothCentral> bluetoothCentrals = getConnectedCentrals();
        for (BluetoothCentral bluetoothCentral : bluetoothCentrals) {
            bluetoothGattServerCallback.onConnectionStateChange(bluetoothAdapter.getRemoteDevice(bluetoothCentral.getAddress()), 0, BluetoothProfile.STATE_DISCONNECTED);
        }
        onAdvertisingStopped();
    }

    private @NonNull byte[] copyOf(@NonNull final byte[] source, final int offset, final int maxSize) {
        if (source.length > maxSize) {
            final int chunkSize = Math.min(source.length - offset, maxSize);
            return Arrays.copyOfRange(source, offset, offset + chunkSize);
        }
        return Arrays.copyOf(source, source.length);
    }

    /**
     * Make a safe copy of a nullable byte array
     *
     * @param source byte array to copy
     * @return non-null copy of the source byte array or an empty array if source was null
     */
    @NonNull
    byte[] copyOf(@Nullable final byte[] source) {
        return (source == null) ? new byte[0] : Arrays.copyOf(source, source.length);
    }

    /**
     * Make a byte array nonnull by either returning the original byte array if non-null or an empty bytearray
     *
     * @param source byte array to make nonnull
     * @return the source byte array or an empty array if source was null
     */
    @NonNull
    private byte[] nonnullOf(@Nullable final byte[] source) {
        return (source == null) ? new byte[0] : source;
    }

    private boolean supportsNotify(@NonNull final BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0;
    }

    private boolean supportsIndicate(@NonNull final BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0;
    }

    private boolean doesNotSupportNotifying(@NonNull final BluetoothGattCharacteristic characteristic) {
        return !(supportsIndicate(characteristic) || supportsNotify(characteristic));
    }
}
