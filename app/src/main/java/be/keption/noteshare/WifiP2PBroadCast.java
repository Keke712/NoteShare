package be.keption.noteshare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import be.keption.noteshare.MainActivity;

public class WifiP2PBroadCast extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity activity;

    public WifiP2PBroadCast(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity) {
        // TODO Auto-generated constructor stub
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            mManager.requestConnectionInfo(mChannel, activity);

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            activity.updateDeviceInfo((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }
    }
}