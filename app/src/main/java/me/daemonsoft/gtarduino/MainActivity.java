package me.daemonsoft.gtarduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.controlwear.virtual.joystick.android.JoystickView;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 200;
    Button buttonUp;
    Button buttonDown;
    Button buttonLeft;
    Button buttonRight;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice car;
    BluetoothSocket socket;
    InputStream inputStream;
    OutputStream outputStream;
    private ConnectedThread mConnectedThread;
    RequestQueue queue;
    int status = 0;
    String url = "https://api.thingspeak.com/update?api_key=G3RLK3G695C9RQNN&field1=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        JoystickView joystick = findViewById(R.id.joystickView);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        queue = Volley.newRequestQueue(this);
        if (!bluetoothAdapter.isEnabled()) {

            Toast.makeText(getApplicationContext(), "Encienda el bluetooth", Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        }
        car = bluetoothAdapter.getRemoteDevice("20:16:09:18:83:93");

        if (null != car) {
            try {
                socket = car.createRfcommSocketToServiceRecord(car.getUuids()[0].getUuid());
                socket.connect();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                mConnectedThread = new ConnectedThread(socket);
                mConnectedThread.start();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "No se encontro el vehiculo", Toast.LENGTH_SHORT).show();
                finish();
            } catch (NullPointerException npe) {
                Toast.makeText(getApplicationContext(), "No se encontro el vehiculo", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if (null == socket) {
            Toast.makeText(getApplicationContext(), "No se encontro el vehiculo", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Vehiculo conectado", Toast.LENGTH_SHORT).show();
            joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
                @Override
                public void onMove(int angle, int strength) {
                    // do whatever you want
                    Log.d("Movement", "" + angle);
                    if (angle > 340 && angle < 30) {
                        status = 4;
                    } else if (angle > 60 && angle < 120) {
                        status = 1;
                    } else if (angle > 260 && angle < 290) {
                        status = 2;
                    } else if (angle > 140 && angle < 220) {
                        status = 3;
                    } else {
                        status = 0;
                    }
                    mConnectedThread.write(status);
                }
            });
            buttonUp = findViewById(R.id.buttonUp);
            buttonUp.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mConnectedThread.write(1); // UP
                            Log.d("MOV", "UP pressed ");
                            break;
                        case MotionEvent.ACTION_UP:
                            mConnectedThread.write(0);
                            Log.d("MOV", "UP released");
                    }
                    return true;
                }
            });

            buttonLeft = findViewById(R.id.buttonLeft);
            buttonLeft.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mConnectedThread.write(3);
                            Log.d("MOV", "LEFT pressed");
                            break;
                        case MotionEvent.ACTION_UP:
                            mConnectedThread.write(0);
                            Log.d("MOV", "LEFT released");
                    }
                    return true;
                }


            });

            buttonDown = findViewById(R.id.buttonDown);
            buttonDown.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mConnectedThread.write(2);
                            Log.d("MOV", "DOWN pressed");
                            break;
                        case MotionEvent.ACTION_UP:

                            mConnectedThread.write(0);
                            Log.d("MOV", "DOWN released");
                    }
                    return true;
                }


            });
            buttonRight = findViewById(R.id.buttonRight);
            buttonRight.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mConnectedThread.write(4);
                            Log.d("MOV", "RIGHT pressed");
                            break;
                        case MotionEvent.ACTION_UP:
                            mConnectedThread.write(0);
                            Log.d("MOV", "RIGHTreleased");
                    }
                    return true;
                }
            });

        }
    }

    @Override
    protected void onDestroy() {
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Intent intent = getIntent();
                overridePendingTransition(0, 0);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                finish();
                overridePendingTransition(0, 0);
                startActivity(intent);

            } else
                Toast.makeText(getApplicationContext(), "Encienda el bluetooth", Toast.LENGTH_LONG).show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    //ByteBuffer wrapped = ByteBuffer.wrap(buffer);
                    // Send the obtained bytes to the UI activity
                    String readMessage = new String(buffer, 0, bytes);
                    if (!"#".equals(readMessage.trim())) {
                        Log.d("Message from Ardu ", readMessage);
                        // Request a string response from the provided URL.
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, url + readMessage,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        // Display the first 500 characters of the response string.
                                        Log.d("Response is: ", response.substring(0, (response.length() < 500) ? response.length() : 500));
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("Response is: ", "That didn't work!");
                            }
                        });

                        // Add the request to the RequestQueue.
                        queue.add(stringRequest);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        public void write(int i) {
            try {
                mmOutStream.write(i);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}

