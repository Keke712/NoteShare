package be.keption.noteshare;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import be.keption.noteshare.*;

public class MainActivity extends Activity implements OnClickListener, PeerListListener, ConnectionInfoListener {

    WifiP2pManager mManager;
    Channel mChannel;
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    private BroadcastReceiver receiver = null;
    private WifiP2pInfo info;
    String groupOwnerAddress;

    ListView listPeers;
    TextView mTextDeviceName, mTextStatus, mTextGroupOwner;
    EditText mEditText_message;
    static String toastmessage;
    Context context = MainActivity.this;
    static String message = "";
    boolean isPeerServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiP2PBroadCast(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        findViewById(R.id.button_search).setOnClickListener(this);
        findViewById(R.id.button_send).setOnClickListener(this);

        listPeers = (ListView) findViewById(R.id.list_peers);
        mTextDeviceName = (TextView) findViewById(R.id.text_device_name);
        mTextStatus = (TextView) findViewById(R.id.text_Status);
        mTextGroupOwner = (TextView) findViewById(R.id.text_group_owner);
        mEditText_message = (EditText) findViewById(R.id.edittext_message);

        listPeers.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // TODO Auto-generated method stub
                connectWithPeers(position);
            }
        });
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        receiver = new WifiP2PBroadCast(mManager, mChannel, this);
        registerReceiver(mReceiver, mIntentFilter);
        discoverPeers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public void updateDeviceInfo(WifiP2pDevice device) {
        mTextDeviceName.setText(device.deviceName);
        mTextStatus.setText(getDeviceStatus(device.status));
    }

    private static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.button_search:
                discoverPeers();
                break;
            case R.id.button_send:
                sendMessage();
                break;
            default:
                break;
        }

    }

    public void discoverPeers() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Context need = this;
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e("test", "Discovering Peers Success..");
                if (mManager != null) {
                    if (ActivityCompat.checkSelfPermission(need, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mManager.requestPeers(mChannel, MainActivity.this);
                }
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e("test", "Discovering Peers Fail..");
            }
        });
    }

    public void connectWithPeers(int id) {
        WifiP2pDevice device = peers.get(id);
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mManager.connect(mChannel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                //success logic
                Log.e("test", "Connection with peer Success..");
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
                Log.e("test", "Connection with peer Failed..");
            }
        });
    }

    public void sendMessage() {
        if (!isPeerServer) {
            //Client Peer
            Toast.makeText(MainActivity.this, "client sending message", Toast.LENGTH_SHORT).show();
            new SendMessage().execute(MainActivity.this);
        } else {
            //Server Peer
            Toast.makeText(MainActivity.this, "server sending message", Toast.LENGTH_SHORT).show();
            new FileServerAsyncTask(this, "").execute();
        }

    }

    //Send message form client to server
    public class SendMessage extends AsyncTask {
        @Override
        protected Object doInBackground(Object... arg0) {
            // TODO Auto-generated method stub
            try {
                message = mEditText_message.getText().toString();

                Socket socket = new Socket(info.groupOwnerAddress.getHostAddress(), 8988);
                //Send the message to the server
                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);

                String number = message;

                String sendMessage = number + "\n";
                bw.write(sendMessage);
                bw.flush();
                Log.e("test", "Message sent to the server : "+sendMessage);

                //Get the return message from the server
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String message = br.readLine();
                Log.e("test", "Message received from the server : " +message);
                socket.close();
            }catch(IOException e){
                Log.e("test", "CLIENT to SERVER message sending exception: "+e.getMessage());
            }finally {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            Toast.makeText(MainActivity.this, "MESSAGE: "+message, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        // TODO Auto-generated method stub
        Log.e("test", "..onConnectionInfoAvailable..");
        info = wifiP2pInfo;
        try {
            groupOwnerAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        } catch (Exception e) {
            // TODO: handle exception
            Log.e("test", "Owner Info null");
        }

        if (info.groupFormed && info.isGroupOwner) {
            Log.e("test", "GOT INFORMATION FROM PEERS..");
            new FileServerAsyncTask(this, "").execute();
        }
        if (wifiP2pInfo.isGroupOwner) {
            isPeerServer = true;
        } else {
            isPeerServer = false;
        }
        mTextGroupOwner.setText("Is group owner? ---- "+((wifiP2pInfo.isGroupOwner == true) ? "yes" : "no"));
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        // TODO Auto-generated method stub
        Log.e("test", "NUMBER OF PEERS AVAILABLE: ----- "+peerList.getDeviceList().size());
        peers.clear();
        peers.addAll(peerList.getDeviceList());

        String[] devices = new String[peerList.getDeviceList().size()];
        for (int i = 0; i < devices.length; i++) {
            devices[i] = peers.get(i).deviceName.toString();
        }

        try {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, R.layout.simplerow, devices);
            listPeers.setAdapter(adapter);
        }catch (Exception e) {
            // TODO: handle exception
            Log.e("test", "EXCEPTION: "+e.toString());
        }

    }

    //Send message form server to client
    public class FileServerAsyncTask extends AsyncTask<Object, Object, Object> {
        private Context context;
        String message;
        public FileServerAsyncTask(Context context, String statusText) {
            this.context = context;
        }
        @Override
        protected Object doInBackground(Object... arg0) {
            // TODO Auto-generated method stub
            Socket socket = null;
            try {

                ServerSocket serverSocket = new ServerSocket(8988);
                //Reading the message from the client
                socket = serverSocket.accept();
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String number = br.readLine();
                toastmessage = number;
                Log.e("test", "Message received from client is "+number);

                //Multiplying the number by 2 and forming the return message
                String returnMessage;
                returnMessage = "Return message from Server..";//String.valueOf(returnValue) + "\n";

                //Sending the response back to the client.
                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);
                bw.write(returnMessage);
                Log.e("test", "Message sent to the client is "+returnMessage);
                bw.flush();

            }catch (Exception e){
                Log.e("test", "SERVER to CLIENT message sending exception: "+e.getMessage());
            }finally {
                try{
                    socket.close();
                }
                catch(Exception e){}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            // TODO Auto-generated method stub
            Toast.makeText(MainActivity.this, "Message from Client: "+toastmessage, Toast.LENGTH_SHORT).show();
        }

    }

}