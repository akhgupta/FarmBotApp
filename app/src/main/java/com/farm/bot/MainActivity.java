package com.farm.bot;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.akhgupta.easylocation.EasyLocationAppCompatActivity;
import com.akhgupta.easylocation.EasyLocationRequest;
import com.akhgupta.easylocation.EasyLocationRequestBuilder;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.gms.location.LocationRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends EasyLocationAppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private UsbManager usbManager;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private UsbSerialDevice serialPort;
    private DatabaseReference firebaseDatabase;
    private TextView helpText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getApplicationContext().getSystemService(USB_SERVICE);

        setupUsb();
        requestLocation();
        setupConnectionStatus();
    }

    private void setupConnectionStatus() {
        firebaseDatabase = FirebaseDatabase.getInstance().getReference("robot/1");

        firebaseDatabase.child("connection_status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    boolean value = (boolean) dataSnapshot.getValue();
                    helpText.setText("connection status :"+value);
                    if (value) {
                        Intent intent = new Intent(getApplicationContext(), LiveVideoActivity.class);
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void requestLocation() {
        LocationRequest locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(5000);

        EasyLocationRequest easyLocationRequest = new EasyLocationRequestBuilder()
                .setLocationRequest(locationRequest)
                .setFallBackToLastLocationTime(3000)
                .build();
        requestLocationUpdates(easyLocationRequest);
    }

    private void setupUsb() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        StringBuilder stringBuilder = new StringBuilder();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            Log.d(TAG, device.getDeviceName());
            stringBuilder.append(device.getDeviceName() + "\n");
            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            manager.requestPermission(device, mPermissionIntent);
            break;
            //your code
        }
        helpText = ((TextView) findViewById(R.id.helpText));
        helpText.setText(stringBuilder.toString());

    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mUsbReceiver);

    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbDeviceConnection connection = usbManager.openDevice(device);


                            connection = usbManager.openDevice(device);
                            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                            if (serialPort != null) {
                                if (serialPort.open()) { //Set Serial Connection Parameters.
                                    serialPort.setBaudRate(9600);
                                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
//                                    serialPort.read(mCallback); //
                                    Log.d(TAG, "Serial Connection Opened!\n");
                                    String string = "w";
                                    serialPort.write(string.getBytes());


                                } else {
                                    Log.d("SERIAL", "PORT NOT OPEN");
                                }
                            } else {
                                Log.d("SERIAL", "PORT IS NULL");
                            }

                            //call method to set up device communication
                        }
                    }
                }
            }
        }
    };

    public void onClickTest(View view) {
        if (serialPort != null) {
            serialPort.write("w".getBytes());
            serialPort.write("a".getBytes());
            serialPort.write("s".getBytes());
            serialPort.write("d".getBytes());
        }
    }

    @Override
    public void onLocationPermissionGranted() {

    }

    @Override
    public void onLocationPermissionDenied() {

    }

    @Override
    public void onLocationReceived(Location location) {
        if(location!=null)
        Log.d(TAG,location.getLatitude()+","+location.getLongitude());
        firebaseDatabase.child("location/lat").setValue(location.getLatitude());
        firebaseDatabase.child("location/long").setValue(location.getLongitude());
    }

    @Override
    public void onLocationProviderEnabled() {

    }

    @Override
    public void onLocationProviderDisabled() {

    }
}
