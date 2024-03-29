package com.example.ble_team3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/*
    Shakhawat Hossain
*/

public class MainActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {



    private static final String TAG = "BLE_TEAM3";

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    public static UUID SERVICE_UUID = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214");
    public static UUID ARDUINO_UUID = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214"); //switchCharacteristic

    public static String address = "CE:2C:01:E0:21:9A";

    private Map<String, Object> bluetoothGatts = new HashMap<>();

    private byte[] valueMoveRight = new byte[]{0x01};
    private byte[] valueMoveLeft = new byte[]{0x02};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connect(address);
//        disconnect(address);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        mBluetoothAdapter.startLeScan(new UUID[]{ARDUINO_UUID}, this);
        Log.i(TAG, "onLeScan started");
    }

    public boolean connect(String address){
        Log.i(TAG, "Connecting to " + address);
        if(mBluetoothAdapter == null || address == null){
            Log.i(TAG, "BluetoothAdapter is not initialized");
            return false;
        }

        // here mobile adapter is finding a bluetooth device based on MAC address
        // creating a GATT connection
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // We want to directly connect to the device, so we are setting the autoConnect parameter to true
        BluetoothGatt bluetoothGatt = device.connectGatt(this, true, mGattCallback);
        Log.i(TAG, "Connected to Arduino....");
        bluetoothGatts.put(address, bluetoothGatt);
        return true;
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            String address = gatt.gatDevice().getAddress();

            if(newState == BluetoothProfile.STATE_CONNECTED){
                gatt.discoverServices();
                Log.i(TAG, "Attempting to start service discovery: " + gatt.discoverServices());
            }else if(newState==BluetoothProfile.STATE_DISCONNECTED){
//                onDisconnected(gatt);
                Log.i(TAG, "Disconnected from GATT server");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status ==BluetoothGatt.GATT_SUCCESS){
                // success, we can communicate with the device
                BluetoothGattCharacteristic characteristic =
                        gatt.getService(SERVICE_UUID).getCharacteristic(ARDUINO_UUID);
                gatt.setCharacteristicNotification(characteristic, true);
                Log.i(TAG, "characteristic: " + characteristic);

                // calling method to insert byte data to move servo
                writeCharacteristic(address, characteristic, valueMoveRight);

                // wait for 5 seconds
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // why this is not working?
                writeCharacteristic(address, characteristic, valueMoveLeft);
            }else {
                // failure
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    // for multiple characterisrics we use this
    // we dont need this here
    // my arduino uuid has only one characteristic
    public BluetoothGattCharacteristic findCharacteristic(String address, UUID characteristicUUID){
        BluetoothGatt bluetoothGatt = (BluetoothGatt) bluetoothGatts.get(address);

        if (bluetoothGatt == null) {
            return null;
        }

        for (BluetoothGattService service : bluetoothGatt.getServices()) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
            if (characteristic != null) {
                Log.i(TAG, "Service: " + characteristic);
                return characteristic;
            }
        }
        return null;
    }


    protected boolean writeCharacteristic(String address, BluetoothGattCharacteristic characteristic, byte[] value){

        BluetoothGatt bluetoothGatt = (BluetoothGatt) bluetoothGatts.get(address);

        // here value controls servo, 1 for left, 2 for right
//        byte[] value = new byte[] {0x02};// value to write;
        Log.i(TAG, "Existing written value: " + characteristic.getValue());
        if (bluetoothGatt != null) {
            Log.i(TAG, "writeCharacteristic sets value");
            characteristic.setValue(value);
            return bluetoothGatt.writeCharacteristic(characteristic);
        }
        return false;
    }

    // method to disconnect Arduino from Phone
    public void disconnect(String address){
        if(mBluetoothAdapter == null){
            Log.i(TAG, "Not connected to Arduino");
        }
        BluetoothGatt bluetoothGatt = (BluetoothGatt) bluetoothGatts.get(address);
        if(bluetoothGatt != null){
            bluetoothGatt.disconnect();
            bluetoothGatts.remove(address);
            Log.i(TAG, "Disconnected from arduino");
        }
    }

    public void moveServo(){
        connect(address);
        disconnect(address);
    }
}
