package com.suraksha.shaurya;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.suraksha.shaurya.db.AppDB;
import com.suraksha.shaurya.db.AppEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static io.nlopez.smartlocation.SmartLocation.with;

public class HomeActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private final String TAG = HomeActivity.class.getSimpleName();

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView mReadBuffer;
    private Button mScanBtn, enableBT;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private ListView mDevicesListView;
    private CheckBox mLED1;

    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;

    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private LinearLayout mainParent;
    private ConstraintLayout secondaryParent;

    private PreferenceHelper preferenceHelper;
    private AppDB database;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        preferenceHelper = new PreferenceHelper(this);
        database = AppDB.createDatabase(this);

        Toolbar mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mActionBarToolbar);
        getSupportActionBar().setTitle("");

        mainParent = findViewById(R.id.mainParent);
        secondaryParent = findViewById(R.id.secondaryParent);

        mBluetoothStatus = (TextView) findViewById(R.id.bluetooth_status);
        mReadBuffer = (TextView) findViewById(R.id.read_buffer);

        mScanBtn = (Button) findViewById(R.id.scan);
        enableBT = findViewById(R.id.enableBT);
        mOffBtn = (Button) findViewById(R.id.off);
        mDiscoverBtn = (Button) findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button) findViewById(R.id.paired_btn);
        mLED1 = (CheckBox) findViewById(R.id.checkbox_led_1);

        mBTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        if (mBTAdapter.isEnabled()) {
            mainParent.setVisibility(View.VISIBLE);
            secondaryParent.setVisibility(View.GONE);
        } else {
            mainParent.setVisibility(View.GONE);
            secondaryParent.setVisibility(View.VISIBLE);
        }

        mDevicesListView = (ListView) findViewById(R.id.devices_list_view);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Ask for location permission if not already allowed
        if (!Constants.hasBothPermissions(this)) {
            Constants.requestBothPermission(this);
        } else {
            boolean status = with(this).location().state().isGpsAvailable();
            if (!status) {
                CustomDialog.showAlertDialog(HomeActivity.this,
                        "Please enable the GPS", "ENABLE GPS",
                        "", new CustomDialog.DialogActionListener() {
                            @Override
                            public void onPositiveButton() {
                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }

                            @Override
                            public void onNegativeButton() {

                            }
                        });
            }
        }
        /*if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);*/


        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ) {
                    // message received from aurdrino
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    mReadBuffer.setText(readMessage);
                    if (readMessage.contains("b")) {
                        sendSMS();
                    }
                }

                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + msg.obj);
                    else
                        mBluetoothStatus.setText("Connection Failed");
                }
            }
        };

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(this, "Bluetooth device not found!", Toast.LENGTH_SHORT).show();
        } else {

            mLED1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mConnectedThread != null) //First check to make sure thread created
                        mConnectedThread.write("1");
                }
            });


            enableBT.setOnClickListener(v -> {
                bluetoothOn();
            });

            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn();
                }
            });

            mOffBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOff();
                }
            });

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listPairedDevices();
                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    discover();
                }
            });
        }

        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, filter1);

        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED);
        registerReceiver(locationSwitchStateReceiver, filter);
    }

    private boolean firstConnect = true;
    private final BroadcastReceiver locationSwitchStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("Location status changed");

            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {

                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (!isGpsEnabled && !isNetworkEnabled) {
                    //location is disabled
                    showDialogHandler.removeMessages(1);
                    showDialogHandler.sendEmptyMessageDelayed(1, 2000);
                } else {
                    //location is enable
                }
            }
        }
    };

    private final Handler showDialogHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            if (message.what == 1) {
                showDialogHandler.removeMessages(1);
                // do your action
                CustomDialog.showAlertDialog(HomeActivity.this,
                        "Please enable the GPS", "ENABLE GPS",
                        "", new CustomDialog.DialogActionListener() {
                            @Override
                            public void onPositiveButton() {
                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }

                            @Override
                            public void onNegativeButton() {

                            }
                        });
            }
            return true;
        }
    });

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        System.out.println("BT is off now ");

                        CustomDialog.showAlertDialog(HomeActivity.this,
                                "Please enable the Bluetooth", "ENABLE Bluetooth",
                                "", new CustomDialog.DialogActionListener() {
                                    @Override
                                    public void onPositiveButton() {
                                        bluetoothOn();
                                    }

                                    @Override
                                    public void onNegativeButton() {

                                    }
                                });
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:

                        break;
                    case BluetoothAdapter.STATE_ON:
                        System.out.println("BT is on now ");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:

                        break;
                }

            }
        }
    };

    private void bluetoothOn() {
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Enabling Bluetooth...");

            Toast.makeText(getApplicationContext(), "Enabling Bluetooth...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Bluetooth Enabled");

                mainParent.setVisibility(View.VISIBLE);
                secondaryParent.setVisibility(View.GONE);
                mScanBtn.setVisibility(View.GONE);
                mOffBtn.setVisibility(View.VISIBLE);
            } else {
                mBluetoothStatus.setText("Bluetooth Disabled");

                mainParent.setVisibility(View.GONE);
                secondaryParent.setVisibility(View.VISIBLE);
            }
        }
    }

    private void bluetoothOff() {
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth Disabled");
        mScanBtn.setVisibility(View.VISIBLE);
        mOffBtn.setVisibility(View.GONE);
        Toast.makeText(getApplicationContext(), "Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover() {
        // Check if the device is already discovering
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
        } else {
            if (mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            } else {
                Toast.makeText(getApplicationContext(), "Please switch on the bluetooth first", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices() {
        mBTArrayAdapter.clear();
        mPairedDevices = mBTAdapter.getBondedDevices();
        if (mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

        } else
            Toast.makeText(getApplicationContext(), "Please switch on the bluetooth first", Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (!mBTAdapter.isEnabled()) {
                Toast.makeText(getApplicationContext(), "Please switch on the bluetooth first", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) view).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread() {
                @Override
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getApplicationContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getApplicationContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (!fail) {
                        mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }

        if (item.getItemId() == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
//            sendSMS();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (Constants.hasLocationPermission(this) && Constants.hasSMSPermission(this)) {
            boolean status = with(this).location().state().isGpsAvailable();
            if (!status) {
                CustomDialog.showAlertDialog(HomeActivity.this,
                        "Please enable the GPS", "ENABLE GPS",
                        "", new CustomDialog.DialogActionListener() {
                            @Override
                            public void onPositiveButton() {
                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }

                            @Override
                            public void onNegativeButton() {

                            }
                        });
            }
        } else {
            Constants.requestBothPermission(this);
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        } else {
            CustomDialog.showAlertDialog(
                    this, "Please provide the mandatory location permission",
                    "Allow", "Cancel", new CustomDialog.DialogActionListener() {
                        @Override
                        public void onPositiveButton() {
                            Constants.requestBothPermission(HomeActivity.this);
                        }

                        @Override
                        public void onNegativeButton() {
                            Constants.requestBothPermission(HomeActivity.this);
                        }
                    }
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private AlertDialog mDialog;
    private MediaPlayer mp;

    private void sendSMS() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(runnable, 15000);

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mp = MediaPlayer.create(this, notification);
        mp.setLooping(true);
        mp.start();


        mDialog = CustomDialog.showAlertDialog(
                this, "Sending SMS automatically after 15 seconds. Press STOP to cancel",
                "STOP", "", new CustomDialog.DialogActionListener() {
                    @Override
                    public void onPositiveButton() {
                        if (mp != null && mp.isPlaying()) {
                            mp.stop();
                        }
                        handler.removeCallbacks(runnable);
                        if (mDialog != null) {
                            mDialog.dismiss();
                        }

                    }

                    @Override
                    public void onNegativeButton() {

                    }
                }
        );

    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mp != null && mp.isPlaying()) {
                mp.stop();
            }

            String user = preferenceHelper.getUserName();
            String phoneNo = "+91" + preferenceHelper.getRelativeNumber();

            LocationParams params = new LocationParams.Builder().setAccuracy(LocationAccuracy.HIGH).build();
            SmartLocation.with(HomeActivity.this).location()
                    .config(params).oneFix().start(location -> {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                String mapLocation = "http://maps.google.com/maps?q=loc:" + latitude + "," + longitude;

                SimpleDateFormat sdfDate = new SimpleDateFormat("MMM dd,yyyy");
                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");

                Date humanReadableDate = new Date(System.currentTimeMillis());
                Date humanReadableTime = new Date(System.currentTimeMillis());

                String todayDate = sdfDate.format(humanReadableDate);
                String todayTime = sdfTime.format(humanReadableTime);


                String message = user + " has been involved in an accident on " + todayDate + " at " + todayTime + " and requires medical assistance. \n" + mapLocation;

                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    ArrayList<String> msgArray = smsManager.divideMessage(message);

                    smsManager.sendMultipartTextMessage(phoneNo, null, msgArray, null, null);
                    Toast.makeText(getApplicationContext(), "Message Sent",
                            Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), ex.getMessage().toString(),
                            Toast.LENGTH_LONG).show();
                    ex.printStackTrace();
                }

                AppEntity entity = new AppEntity();
                entity.setTime(System.currentTimeMillis());
                entity.setMessage(message);
                entity.setReceiverNumber(phoneNo);

                database.getDao().insertMessage(entity);

                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }


            });

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(locationSwitchStateReceiver);
    }
}
