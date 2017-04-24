package com.farm.bot;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.akhgupta.easylocation.EasyLocationAppCompatActivity;
import com.akhgupta.easylocation.EasyLocationRequest;
import com.felhr.usbserial.UsbSerialDevice;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.SinchClientListener;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.video.VideoCallListener;
import com.sinch.android.rtc.video.VideoController;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends EasyLocationAppCompatActivity implements SinchClientListener, ServiceConnection {

    private static final String TAG = MainActivity.class.getSimpleName();
    private UsbManager usbManager;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private UsbSerialDevice serialPort;
    private DatabaseReference robotDatabase;
    private TextView helpText;
    private SinchService.SinchServiceInterface mSinchServiceInterface;
    private boolean mRemoteVideoViewAdded = false;
    private String mCallId;
    private boolean mAddedListener;
    private DatabaseReference controllerDatabase;
    private View qrCode;
    private View animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        usbManager = (UsbManager) getApplicationContext().getSystemService(USB_SERVICE);
        EventBus.getInstance().getBus().register(this);
        getApplicationContext().bindService(new Intent(this, SinchService.class), this,
                BIND_AUTO_CREATE);
        registerReceiver(mUsbReceiver, filter);
        setupUsb();
        setupConnectionStatus();
        EasyLocationRequest easyLocationRequest = LocationUtil.requestLocation();
        requestLocationUpdates(easyLocationRequest);
        animation = findViewById(R.id.animation_view);
        qrCode = findViewById(R.id.qrcodeImageView);
    }

    private void setupConnectionStatus() {
        robotDatabase = FirebaseDatabase.getInstance().getReference("robot/1");
        controllerDatabase = FirebaseDatabase.getInstance().getReference("controller/1");

        robotDatabase.child("connection_status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    boolean value = (boolean) dataSnapshot.getValue();
                    if (value) {
                        obeyController();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void obeyController() {
        qrCode.setVisibility(View.GONE);
        animation.setVisibility(View.VISIBLE);
        controllerDatabase.child("direction").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String direction = (String) dataSnapshot.getValue();
                if (direction != null && serialPort != null) {
                    serialPort.write(direction.getBytes());
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        controllerDatabase.child("spray").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    boolean value = (boolean) dataSnapshot.getValue();
                    if (value && serialPort != null){
                        serialPort.write("y".getBytes());
                    };
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mUsbReceiver);
        removeVideoViews();
    }

    private void removeVideoViews() {
        if (getSinchServiceInterface() == null) {
            return; // early
        }

        VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            LinearLayout view = (LinearLayout) findViewById(R.id.remoteVideo);
            view.removeView(vc.getRemoteView());
            mRemoteVideoViewAdded = false;
        }
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbDeviceConnection connection = usbManager.openDevice(device);
                            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                            if (serialPort != null) {
                                if (serialPort.open()) {
                                    serialPort = UsbUtil.setupSerial(serialPort);


                                } else {
                                    Log.d("SERIAL", "PORT NOT OPEN");
                                }
                            } else {
                                Log.d("SERIAL", "PORT IS NULL");
                            }
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
        if (location != null)
            Log.d(TAG, location.getLatitude() + "," + location.getLongitude());
        robotDatabase.child("location/lat").setValue(location.getLatitude());
        robotDatabase.child("location/long").setValue(location.getLongitude());
    }

    @Override
    public void onLocationProviderEnabled() {

    }

    @Override
    public void onLocationProviderDisabled() {

    }

    @Override
    public void onClientStarted(SinchClient sinchClient) {

    }

    @Override
    public void onClientStopped(SinchClient sinchClient) {

    }

    @Override
    public void onClientFailed(SinchClient sinchClient, SinchError sinchError) {

    }

    @Override
    public void onRegistrationCredentialsRequired(SinchClient sinchClient, ClientRegistration clientRegistration) {

    }

    @Override
    public void onLogMessage(int i, String s, String s1) {

    }

    @Subscribe
    public void onIncomingCall(InComingCallEvent inComingCallEvent) {
        mCallId = inComingCallEvent.getCallId();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            if (!mAddedListener) {
                call.addCallListener(new MainActivity.SinchCallListener());
                mAddedListener = true;
            }
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (SinchService.class.getName().equals(componentName.getClassName())) {
            mSinchServiceInterface = (SinchService.SinchServiceInterface) iBinder;
        }
        if (!mSinchServiceInterface.isStarted())
            mSinchServiceInterface.startClient("robotapp");

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        if (SinchService.class.getName().equals(componentName.getClassName())) {
            mSinchServiceInterface = null;
        }
    }

    protected SinchService.SinchServiceInterface getSinchServiceInterface() {
        return mSinchServiceInterface;
    }

    private void endCall() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        finish();
    }


    private class SinchCallListener implements VideoCallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended. Reason: " + cause.toString());
            String endMsg = "Call ended: " + call.getDetails().toString();
            endCall();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> list) {

        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
            Log.d(TAG, "Call offered video: " + call.getDetails().isVideoOffered());
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
        }

        @Override
        public void onVideoTrackAdded(Call call) {
            Log.d(TAG, "Video track added");
            addRemoteView();
        }

        @Override
        public void onVideoTrackPaused(Call call) {

        }

        @Override
        public void onVideoTrackResumed(Call call) {

        }

        private void addRemoteView() {
            if (mRemoteVideoViewAdded || getSinchServiceInterface() == null) {
                return; //early
            }
            final VideoController vc = getSinchServiceInterface().getVideoController();
            if (vc != null) {
                LinearLayout view = (LinearLayout) findViewById(R.id.remoteVideo);
                view.addView(vc.getRemoteView());
                mRemoteVideoViewAdded = true;
            }
        }
    }

}
