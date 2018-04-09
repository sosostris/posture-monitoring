package com.example.xujia.posturemonitor.common;

import java.io.PipedOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.xujia.posturemonitor.sensornode.PostureMonitorApplication;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// import android.util.Log;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    static final String TAG = "BluetoothLeService";

    public final static String ACTION_GATT_CONNECTED = "com.example.xujia.posturemonitor.common.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.xujia.posturemonitor.common.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.xujia.posturemonitor.common.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ = "com.example.xujia.posturemonitor.common.ACTION_DATA_READ";
    public final static String ACTION_DATA_NOTIFY = "com.example.xujia.posturemonitor.common.ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE = "com.example.xujia.posturemonitor.common.ACTION_DATA_WRITE";
    public final static String EXTRA_DATA = "com.example.xujia.posturemonitor.common.EXTRA_DATA";
    public final static String EXTRA_UUID = "com.example.xujia.posturemonitor.common.EXTRA_UUID";
    public final static String EXTRA_STATUS = "com.example.xujia.posturemonitor.common.EXTRA_STATUS";
    public final static String EXTRA_ADDRESS = "com.example.xujia.posturemonitor.common.EXTRA_ADDRESS";
    public final static int GATT_TIMEOUT = 200;

    // BLE
    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothGatt[] mBluetoothGatts = null;
    private static BluetoothLeService mThis = null;

    private final Lock lock = new ReentrantLock();

    private volatile boolean blocking = false;
    private volatile int lastGattStatus = 0; //Success

    private volatile bleRequest curBleRequest = null;

    public enum bleRequestOperation {
        wrBlocking,
        wr,
        rdBlocking,
        rd,
        nsBlocking,
    }

    public enum bleRequestStatus {
        not_queued,
        queued,
        processing,
        timeout,
        done,
        no_such_request,
        failed,
    }

    public class bleRequest {
        public int id;
        public BluetoothGatt gatt;
        public BluetoothGattCharacteristic characteristic;
        public bleRequestOperation operation;
        public volatile bleRequestStatus status;
        public int timeout;
        public int curTimeout;
        public boolean notifyenable;
    }

    // Queuing for fast application response.
    private volatile LinkedList<bleRequest> procQueue;
    private volatile LinkedList<bleRequest> nonBlockQueue;

    /**
     * GATT client callbacks
     */
    public static MyBluetoothGattCallback[] myBluetoothGattCallbacks;
//    private BluetoothGattCallback mGattCallbacks0 = new BluetoothGattCallback() {
//
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//
//            BluetoothDevice device = gatt.getDevice();
//            String address = device.getAddress();
//            Log.d(TAG, "onConnectionStateChange (" + address + ") " + newState + " status: " + status);
//
//            try {
//                switch (newState) {
//                    case BluetoothProfile.STATE_CONNECTED:
//                        broadcastUpdate(ACTION_GATT_CONNECTED, address, status);
//                        break;
//                    case BluetoothProfile.STATE_DISCONNECTED:
//                        broadcastUpdate(ACTION_GATT_DISCONNECTED, address, status);
//                        break;
//                    default:
//                        // Log.e(TAG, "New state not processed: " + newState);
//                        break;
//                }
//            } catch (NullPointerException e) {
//                e.printStackTrace();
//            }
//
//        }
//
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            BluetoothDevice device = gatt.getDevice();
//            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device.getAddress(),
//                    status);
//        }
//
//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt,
//                                            BluetoothGattCharacteristic characteristic) {
//            broadcastUpdate(ACTION_DATA_NOTIFY, characteristic,
//                    BluetoothGatt.GATT_SUCCESS);
//        }
//
//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt,
//                                         BluetoothGattCharacteristic characteristic, int status) {
//            if (blocking)unlockBlockingThread(status);
//            if (nonBlockQueue.size() > 0) {
//                lock.lock();
//                for (int ii = 0; ii < nonBlockQueue.size(); ii++) {
//                    bleRequest req = nonBlockQueue.get(ii);
//                    if (req.characteristic == characteristic) {
//                        req.status = bleRequestStatus.done;
//                        nonBlockQueue.remove(ii);
//                        break;
//                    }
//                }
//                lock.unlock();
//            }
//            broadcastUpdate(ACTION_DATA_READ, characteristic, status);
//        }
//
//        @Override
//        public void onCharacteristicWrite(BluetoothGatt gatt,
//                                          BluetoothGattCharacteristic characteristic, int status) {
//            if (blocking)unlockBlockingThread(status);
//            if (nonBlockQueue.size() > 0) {
//                lock.lock();
//                for (int ii = 0; ii < nonBlockQueue.size(); ii++) {
//                    bleRequest req = nonBlockQueue.get(ii);
//                    if (req.characteristic == characteristic) {
//                        req.status = bleRequestStatus.done;
//                        nonBlockQueue.remove(ii);
//                        break;
//                    }
//                }
//                lock.unlock();
//            }
//            broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);
//        }
//
//        @Override
//        public void onDescriptorRead(BluetoothGatt gatt,
//                                     BluetoothGattDescriptor descriptor, int status) {
//            if (blocking)unlockBlockingThread(status);
//            unlockBlockingThread(status);
//        }
//
//        @Override
//        public void onDescriptorWrite(BluetoothGatt gatt,
//                                      BluetoothGattDescriptor descriptor, int status) {
//            if (blocking)unlockBlockingThread(status);
//            // Log.i(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString());
//        }
//    };

    private void unlockBlockingThread(int status) {
        this.lastGattStatus = status;
        this.blocking = false;
    }

    private void broadcastUpdate(final String action, final String address,
                                 final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    // The address is to show the source of the data
    private void broadcastUpdate(final String action, final String address, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    public boolean checkGatt() {
        if (this.blocking) {
            Log.d(TAG,"Cannot start operation : Blocked");
            return false;
        }
        return true;
    }

    public BluetoothGatt[] getGatts() {
        return mBluetoothGatts;
    }

    /**
     * Manage the BLE service
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular example,
        // close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        mThis = this;
        mBluetoothGatts = new BluetoothGatt[24];

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                // Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBtAdapter = mBluetoothManager.getAdapter();
        if (mBtAdapter == null) {
            // Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        procQueue = new LinkedList<bleRequest>();
        nonBlockQueue = new LinkedList<bleRequest>();


        Thread queueThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    executeQueue();
                    try {
                        Thread.sleep(0,100000);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        queueThread.start();
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Log.i(TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        this.initialize();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothGatts != null) {
            for (BluetoothGatt gatt : mBluetoothGatts) {
                gatt.close();
            }
            mBluetoothGatts = null;
        }
    }

    //
    // GATT API
    //

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic
     *          The characteristic to read from.
     */
    public int readCharacteristic(BluetoothGattCharacteristic characteristic) {
        bleRequest req = new bleRequest();
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.rdBlocking;
        addRequestToQueue(req);
        boolean finished = false;
        while (!finished) {
            bleRequestStatus stat = pollForStatusofRequest(req);
            if (stat == bleRequestStatus.done) {
                finished = true;
                return 0;
            }
            else if (stat == bleRequestStatus.timeout) {
                finished = true;
                return -3;
            }
        }
        return -2;
    }

    public int writeCharacteristic(BluetoothGattCharacteristic characteristic, byte b, int position) {
        byte[] val = new byte[1];
        val[0] = b;
        characteristic.setValue(val);

        bleRequest req = new bleRequest();
        req.gatt = mBluetoothGatts[position];
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.wrBlocking;
        addRequestToQueue(req);
        boolean finished = false;
        while (!finished) {
            bleRequestStatus stat = pollForStatusofRequest(req);
            if (stat == bleRequestStatus.done) {
                finished = true;
                return 0;
            }
            else if (stat == bleRequestStatus.timeout) {
                finished = true;
                return -3;
            }
        }
        return -2;
    }
    public int writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] b, int position) {
        characteristic.setValue(b);
        bleRequest req = new bleRequest();
        req.status = bleRequestStatus.not_queued;
        req.gatt = mBluetoothGatts[position];
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.wrBlocking;
        addRequestToQueue(req);
        boolean finished = false;
        while (!finished) {
            bleRequestStatus stat = pollForStatusofRequest(req);
            if (stat == bleRequestStatus.done) {
                finished = true;
                return 0;
            }
            else if (stat == bleRequestStatus.timeout) {
                finished = true;
                return -3;
            }
        }
        return -2;
    }
    public int writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        bleRequest req = new bleRequest();
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.wrBlocking;
        addRequestToQueue(req);
        boolean finished = false;
        while (!finished) {
            bleRequestStatus stat = pollForStatusofRequest(req);
            if (stat == bleRequestStatus.done) {
                finished = true;
                return 0;
            }
            else if (stat == bleRequestStatus.timeout) {
                finished = true;
                return -3;
            }
        }
        return -2;
    }

    public boolean writeCharacteristicNonBlock(BluetoothGattCharacteristic characteristic) {
        bleRequest req = new bleRequest();
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.wr;
        addRequestToQueue(req);
        return true;
    }

    /**
     * Retrieves the number of GATT services on the connected device. This should
     * be invoked only after {@code BluetoothGatt#discoverServices()} completes
     * successfully.
     *
     * @return A {@code integer} number of supported services.
     */
    public int getNumServices(int position) {
        return mBluetoothGatts[position].getServices().size();
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(int position) {
        return mBluetoothGatts[position].getServices();
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic
     *          Characteristic to act on.
     * @param enable
     *          If true, enable notification. False otherwise.
     */
    public int setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable, int position) {
        bleRequest req = new bleRequest();
        req.gatt = mBluetoothGatts[position];
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.nsBlocking;
        req.notifyenable = enable;
        addRequestToQueue(req);
        boolean finished = false;
        while (!finished) {
            bleRequestStatus stat = pollForStatusofRequest(req);
            if (stat == bleRequestStatus.done) {
                finished = true;
                return 0;
            }
            else if (stat == bleRequestStatus.timeout) {
                finished = true;
                return -3;
            }
        }
        return -2;
    }

    public boolean isNotificationEnabled(
            BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            return false;
        }
        if (!checkGatt())
            return false;

        BluetoothGattDescriptor clientConfig = characteristic
                .getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
        if (clientConfig == null)
            return false;

        return clientConfig.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address
     *          The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The
     *         connection result is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(String address, int position) {
        if (mBtAdapter == null) {
            return false;
        }
        final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device,
                BluetoothProfile.GATT);

        if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {

            // Previously connected device. Try to reconnect.
//            if (mBluetoothGatts[position] != null) {
//                if (mBluetoothGatts[position].connect()) {
//                    return true;
//                } else {
//                    return false;
//                }
//            }

            if (device == null) {
                // Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }

            // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
            Log.d(TAG, "Create a new GATT connection.");

            myBluetoothGattCallbacks[position] = new MyBluetoothGattCallback();
            mBluetoothGatts[position] = device.connectGatt(this, false, myBluetoothGattCallbacks[position]);

//            if (position==0) {
//                mBluetoothGatts[position] = device.connectGatt(this, false, mGattCallbacks0);
//            } else {
//                mBluetoothGatts[position] = device.connectGatt(this, false, mGattCallbacks1);
//            }

        } else {
            // Log.w(TAG, "Attempt to connect in state: " + connectionState);
            return false;
        }
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection.
     * The disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(BluetoothDevice device, int position) {
        if (mBtAdapter == null) {
            // Log.w(TAG, "disconnect: BluetoothAdapter not initialized");
            return;
        }
        int connectionState = mBluetoothManager.getConnectionState(device,
                BluetoothProfile.GATT);

        if (mBluetoothGatts[position] != null) {
            if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
                mBluetoothGatts[position].disconnect();
            } else {
                // Log.w(TAG, "Attempt to disconnect in state: " + connectionState);
            }
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        for (BluetoothGatt gatt : mBluetoothGatts) {
            if (gatt != null) {
                gatt.close();
            }
        }
        mBluetoothGatts = null;
    }

    public int numConnectedDevices() {
        List<BluetoothDevice> devList;
        devList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        return devList.size();
    }

    //
    // Utility functions
    //
    public static BluetoothGatt getBtGatt(int position) {
        return mThis.mBluetoothGatts[position];
    }

    public static BluetoothManager getBtManager() {
        return mThis.mBluetoothManager;
    }

    public static BluetoothLeService getInstance() {
        return mThis;
    }

    public void waitIdle(int timeout) {
        while (timeout-- > 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    public boolean addRequestToQueue(bleRequest req) {
        lock.lock();
        if (procQueue.peekLast() != null) {
            req.id = procQueue.peek().id++;
        }
        else {
            req.id = 0;
            procQueue.add(req);
        }
        lock.unlock();
        return true;
    }

    public bleRequestStatus pollForStatusofRequest(bleRequest req) {
        lock.lock();
        if (req == curBleRequest) {
            bleRequestStatus stat = curBleRequest.status;
            if (stat == bleRequestStatus.done) {
                curBleRequest = null;
            }
            if (stat == bleRequestStatus.timeout) {
                curBleRequest = null;
            }
            lock.unlock();
            return stat;
        }
        else {
            lock.unlock();
            return bleRequestStatus.no_such_request;
        }
    }

    private void executeQueue() {
        // Everything here is done on the queue
        lock.lock();
        if (curBleRequest != null) {
            Log.d(TAG, "executeQueue, curBleRequest running");
            try {
                curBleRequest.curTimeout++;
                if (curBleRequest.curTimeout > GATT_TIMEOUT) {
                    curBleRequest.status = bleRequestStatus.timeout;
                    curBleRequest = null;
                }
                Thread.sleep(10, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            lock.unlock();
            return;
        }
        if (procQueue == null) {
            lock.unlock();
            return;
        }
        if (procQueue.size() == 0) {
            lock.unlock();
            return;
        }
        bleRequest procReq = procQueue.removeFirst();

        switch (procReq.operation) {
            case rd:
                //Read, do non blocking read
                break;
            case rdBlocking:
                //Normal (blocking) read
                if (procReq.timeout == 0) {
                    procReq.timeout = GATT_TIMEOUT;
                }
                procReq.curTimeout = 0;
                curBleRequest = procReq;
                int stat = sendBlockingReadRequest(procReq);
                if (stat == -2) {
                    Log.d(TAG,"executeQueue rdBlocking: error, BLE was busy or device disconnected");
                    lock.unlock();
                    return;
                }
                break;
            case wr:
                //Write, do non blocking write (Ex: OAD)
                nonBlockQueue.add(procReq);
                sendNonBlockingWriteRequest(procReq);
                break;
            case wrBlocking:
                //Normal (blocking) write
                if (procReq.timeout == 0) {
                    procReq.timeout = GATT_TIMEOUT;
                }
                curBleRequest = procReq;
                stat = sendBlockingWriteRequest(procReq);
                if (stat == -2) {
                    Log.d(TAG,"executeQueue wrBlocking: error, BLE was busy or device disconnected");
                    lock.unlock();
                    return;
                }
                break;
            case nsBlocking:
                if (procReq.timeout == 0) {
                    procReq.timeout = GATT_TIMEOUT;
                }
                curBleRequest = procReq;
                stat = sendBlockingNotifySetting(procReq);
                if (stat == -2) {
                    Log.d(TAG,"executeQueue nsBlocking: error, BLE was busy or device disconnected");
                    lock.unlock();
                    return;
                }
                break;
            default:
                break;

        }
        lock.unlock();
    }

    public int sendNonBlockingReadRequest(bleRequest request) {
        request.status = bleRequestStatus.processing;
        if (!checkGatt()) {
            request.status = bleRequestStatus.failed;
            return -2;
        }
        request.gatt.readCharacteristic(request.characteristic);
        return 0;
    }

    public int sendNonBlockingWriteRequest(bleRequest request) {
        request.status = bleRequestStatus.processing;
        if (!checkGatt()) {
            request.status = bleRequestStatus.failed;
            return -2;
        }
        request.gatt.writeCharacteristic(request.characteristic);
        return 0;
    }

    public int sendBlockingReadRequest(bleRequest request) {
        request.status = bleRequestStatus.processing;
        int timeout = 0;
        if (!checkGatt()) {
            request.status = bleRequestStatus.failed;
            return -2;
        }
        request.gatt.readCharacteristic(request.characteristic);
        this.blocking = true; // Set read to be blocking
        while (this.blocking) {
            timeout ++;
            waitIdle(1);
            if (timeout > GATT_TIMEOUT) {this.blocking = false; request.status = bleRequestStatus.timeout; return -1;}  //Read failed TODO: Fix this to follow connection interval !
        }
        request.status = bleRequestStatus.done;
        return lastGattStatus;
    }

    public int sendBlockingWriteRequest(bleRequest request) {
        request.status = bleRequestStatus.processing;
        int timeout = 0;
        if (!checkGatt()) {
            request.status = bleRequestStatus.failed;
            return -2;
        }
        request.gatt.writeCharacteristic(request.characteristic);
        this.blocking = true; // Set read to be blocking
        while (this.blocking) {
            timeout ++;
            waitIdle(1);
            if (timeout > GATT_TIMEOUT) {this.blocking = false; request.status = bleRequestStatus.timeout; return -1;}  //Read failed TODO: Fix this to follow connection interval !
        }
        request.status = bleRequestStatus.done;
        return lastGattStatus;
    }

    public int sendBlockingNotifySetting(bleRequest request) {
        request.status = bleRequestStatus.processing;
        int timeout = 0;
        if (request.characteristic == null) {
            return -1;
        }
        if (!checkGatt())
            return -2;

        if (request.gatt.setCharacteristicNotification(request.characteristic, request.notifyenable)) {

            BluetoothGattDescriptor clientConfig = request.characteristic
                    .getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
            if (clientConfig != null) {

                if (request.notifyenable) {
                    // Log.i(TAG, "Enable notification: " +
                    // characteristic.getUuid().toString());
                    clientConfig
                            .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    // Log.i(TAG, "Disable notification: " +
                    // characteristic.getUuid().toString());
                    clientConfig
                            .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                request.gatt.writeDescriptor(clientConfig);
                // Log.i(TAG, "writeDescriptor: " +
                // characteristic.getUuid().toString());
                this.blocking = true; // Set read to be blocking
                while (this.blocking) {
                    timeout ++;
                    waitIdle(1);
                    if (timeout > GATT_TIMEOUT) {this.blocking = false; request.status = bleRequestStatus.timeout; return -1;}  //Read failed TODO: Fix this to follow connection interval !
                }
                request.status = bleRequestStatus.done;
                return lastGattStatus;
            }
        }
        return -3; // Set notification to android was wrong ...
    }

    public class MyBluetoothGattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice device = gatt.getDevice();
            String address = device.getAddress();
            Log.d(TAG, "onConnectionStateChange (" + address + ") " + newState + " status: " + status);

            try {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        broadcastUpdate(ACTION_GATT_CONNECTED, address, status);
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        broadcastUpdate(ACTION_GATT_DISCONNECTED, address, status);
                        break;
                    default:
                        // Log.e(TAG, "New state not processed: " + newState);
                        break;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothDevice device = gatt.getDevice();
            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device.getAddress(),
                    status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String address = gatt.getDevice().getAddress();
            broadcastUpdate(ACTION_DATA_NOTIFY, address, characteristic,
                    BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (blocking)unlockBlockingThread(status);
            if (nonBlockQueue.size() > 0) {
                lock.lock();
                for (int ii = 0; ii < nonBlockQueue.size(); ii++) {
                    bleRequest req = nonBlockQueue.get(ii);
                    if (req.characteristic == characteristic) {
                        req.status = bleRequestStatus.done;
                        nonBlockQueue.remove(ii);
                        break;
                    }
                }
                lock.unlock();
            }
            broadcastUpdate(ACTION_DATA_READ, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (blocking)unlockBlockingThread(status);
            if (nonBlockQueue.size() > 0) {
                lock.lock();
                for (int ii = 0; ii < nonBlockQueue.size(); ii++) {
                    bleRequest req = nonBlockQueue.get(ii);
                    if (req.characteristic == characteristic) {
                        req.status = bleRequestStatus.done;
                        nonBlockQueue.remove(ii);
                        break;
                    }
                }
                lock.unlock();
            }
            broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            if (blocking)unlockBlockingThread(status);
            unlockBlockingThread(status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            if (blocking)unlockBlockingThread(status);
            // Log.i(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString());
        }
    }

}
