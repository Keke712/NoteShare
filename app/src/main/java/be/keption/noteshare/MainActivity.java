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
import android.os.Build;
import android.os.Bundle;
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
import androidx.core.content.ContextCompat;

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
    static String toastMessage;
    Context context = MainActivity.this;
    static String message = "";
    boolean isPeerServer;
    TextView error;
    TextView messageLabel;

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

        error = (TextView) findViewById(R.id.ceciestunlabel);
        listPeers = (ListView) findViewById(R.id.list_peers);
        mTextDeviceName = (TextView) findViewById(R.id.text_device_name);
        mTextStatus = (TextView) findViewById(R.id.text_Status);
        mTextGroupOwner = (TextView) findViewById(R.id.text_group_owner);
        mEditText_message = (EditText) findViewById(R.id.edittext_message);
        messageLabel = (TextView) findViewById(R.id.messageLabel);

        requestPermissions();

        listPeers.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // TODO Auto-generated method stub
                connectWithPeers(position);
            }
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.CHANGE_NETWORK_STATE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.UPDATE_DEVICE_STATS
                }, 0);
            }
        }
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
            case R.id.button_disconnect:
                UniversalResolver();
                break;
            default:
                break;
        }

    }

    public void UniversalResolver() {
        mManager.removeGroup(mChannel, null);
    }

    public void discoverPeers() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            error.setText("Perm problem");
        }
        Context need = this;
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                error.setText("Discovering Peers Success..");

                if (mManager != null) {
                    if (ActivityCompat.checkSelfPermission(need, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        error.setText("Perm problem");
                    }
                    mManager.requestPeers(mChannel, MainActivity.this);
                }
            }

            @Override
            public void onFailure(int reasonCode) {
                error.setText("Discovering Peers Fail..");
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
                error.setText("Connection with peer Success..");
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
                error.setText("Connection with peer Failed..");
            }
        });
    }

    public void sendMessage() {
        if (!isPeerServer) {
            //Client Peer
            Toast.makeText(MainActivity.this, "Client sending message", Toast.LENGTH_SHORT).show();
            new SendMessage().execute(MainActivity.this);
        } else {
            //Server Peer
            Toast.makeText(MainActivity.this, "Server sending message", Toast.LENGTH_SHORT).show();
            new FileServerAsyncTask(this, "").execute();
        }

    }

    // CLIENT TO SERVER
    public class SendMessage extends AsyncTask {
        @Override
        protected Object doInBackground(Object... arg0) {
            try {
                if (info.groupOwnerAddress == null) {
                    error.setText("Info: null");
                }
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

                //Get the return message from the server
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String message = br.readLine();

                socket.close();
            }catch(IOException e){
                System.out.println("Exception");
            }finally {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
        }

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        // TODO Auto-generated method stub

        info = wifiP2pInfo;
        try {
            groupOwnerAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        } catch (Exception e) {
            // TODO: handle exception
            error.setText("Owner Info: null");

        }

        if (info.groupFormed && info.isGroupOwner) {
            new FileServerAsyncTask(this, "").execute();
        }
        if (wifiP2pInfo.isGroupOwner) {
            isPeerServer = true;
        } else {
            isPeerServer = false;
        }

        Boolean isGroupOwner = wifiP2pInfo.isGroupOwner;
        Boolean isGroup = wifiP2pInfo.groupFormed;
        if (isGroup == true) {
            if (isGroupOwner == true) {
                mTextGroupOwner.setText(": Owner");
            }else {
                mTextGroupOwner.setText(": Client");
            }
        }else{
            mTextGroupOwner.setText("");
        }

    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        // TODO Auto-generated method stub
        error.setText("PEERS AVAILABLE: "+peerList.getDeviceList().size());

        peers.clear();
        peers.addAll(peerList.getDeviceList());

        String[] devices = new String[peerList.getDeviceList().size()];
        for (int i = 0; i < devices.length; i++) {
            devices[i] = peers.get(i).deviceName.toString();
        }

        try {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, devices);
            listPeers.setAdapter(adapter);
        }catch (Exception e) {
            // TODO: handle exception
            error.setText("EXCEPTION: "+e.toString());

        }

    }

    // SERVER TO CLIENT
    public class FileServerAsyncTask extends AsyncTask<Object, Object, Object> {
        private Context context;
        public String message;
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
                toastMessage = number;

                message = "Message: "+number;
                messageLabel.setText(message);
                error.setText(message);

                //Multiplying the number by 2 and forming the return message
                String returnMessage;
                returnMessage = "Return message from Server";//String.valueOf(returnValue) + "\n";

                //Sending the response back to the client.
                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);
                bw.write(returnMessage);

                bw.flush();

            }catch (Exception e){
                UniversalResolver();
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
            messageLabel.setText("Message: "+toastMessage);
        }

    }

}