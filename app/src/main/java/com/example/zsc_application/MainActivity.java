package com.example.zsc_application;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Binder;
import android.os.Bundle;

import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    Set<Button> buttons;
    private static final int activeButtonColor = 0xFFA4A4A4;
    private static final int baseButtonColor = 0x9668ED;

    private BluetoothAdapter mBTAdapter;

    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttons = new ArraySet<>();
        Button forwardButton = (Button) findViewById(R.id.FORWARD);
        Button backwardButton = (Button) findViewById(R.id.BACKWARD);
        Button lf = (Button) findViewById(R.id.LF);
        Button lb = (Button) findViewById(R.id.LB);
        Button rf = (Button) findViewById(R.id.RF);
        Button rb = (Button) findViewById(R.id.RB);
        Button stop = (Button) findViewById(R.id.STOP);
        buttons.add(forwardButton);
        buttons.add(backwardButton);
        buttons.add(lf);
        buttons.add(rf);
        buttons.add(rb);
        buttons.add(lb);
        buttons.add(stop);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
        new Thread(() -> {
            boolean fail = false;

            BluetoothDevice device = mBTAdapter.getRemoteDevice("00:21:06:BE:88:21");

            try {
                mBTSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                fail = true;
//                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
            }
            // Establish the Bluetooth socket connection.
            try {
                while (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                mBTSocket.connect();

            } catch (IOException e) {
                try {
                    fail = true;
                    mBTSocket.close();

                } catch (IOException e2) {
                    //insert code to deal with this
                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                }
            }
            if (!fail) {
                mConnectedThread = new ConnectedThread(mBTSocket);
                mConnectedThread.start();
            }
        }).start();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);


        if (mBTAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Bluetooth device not found!", Toast.LENGTH_SHORT).show();
        } else {

            forwardButton.setOnClickListener(v -> {
                changeActiveButton((Button) v);
                if (mConnectedThread != null)
                    mConnectedThread.write(128 + 64);
            });
            backwardButton.setOnClickListener(v -> {
                changeActiveButton((Button) v);
                if (mConnectedThread != null)
                    mConnectedThread.write(128);
            });
            rf.setOnClickListener(v -> {
                changeActiveButton((Button) v);
                if (mConnectedThread != null)
                    mConnectedThread.write(128+64+32);
            });
            lf.setOnClickListener(v -> {
                changeActiveButton((Button) v);
                if (mConnectedThread != null)
                    mConnectedThread.write(128+64+32+16);
            });
            rb.setOnClickListener(v -> {
                changeActiveButton((Button) v);
                if (mConnectedThread != null)
                    mConnectedThread.write(128+32);
            });
            lb.setOnClickListener(v -> {
                changeActiveButton((Button) v);
                if (mConnectedThread != null)
                    mConnectedThread.write(128+32+16);
            });
            stop.setOnClickListener(v -> {
                buttons.forEach(button -> button.setBackgroundColor(MainActivity.baseButtonColor));
                v.setBackgroundColor(MainActivity.activeButtonColor);
                if (mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write(0);
            });
        }
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }

        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }
    private void changeActiveButton(Button activeButton){
        buttons.forEach(button -> {
            button.setBackgroundColor(activeButton.getId() != button.getId() ? MainActivity.baseButtonColor : MainActivity.activeButtonColor);
        });
    }
}
