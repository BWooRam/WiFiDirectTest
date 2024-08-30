package com.geekstudio.wifidirecttest

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView

class WiFiDirectActivity : AppCompatActivity(R.layout.main) {
    private val TAG = "WiFiDirectActivity Log"
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var adapter: WiFiDirectAdapter
    private var receiver: WiFiDirectBroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        //
        channel = manager.initialize(this, mainLooper) {
            Log.d(TAG, "WifiP2pManager onChannelDisconnected")
        }
        findViewById<Button>(R.id.btDiscoverPeers).setOnClickListener { initDiscoverPeers() }
        findViewById<Button>(R.id.btWifiDevicePeerList).setOnClickListener { initWifiDevicePeerList() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = WiFiDirectAdapter()
        recyclerView.adapter = adapter
    }

    private fun initDiscoverPeers() {
        if (!checkWiFiPermission()) {
            Log.d(TAG, "initDiscoverPeers checkWiFiPermission fail")
            return
        }

        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank. Code for peer discovery goes in the
                // onReceive method, detailed below.
                Log.d(TAG, "discoverPeers onSuccess")
            }

            override fun onFailure(reasonCode: Int) {
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
                Log.d(TAG, "discoverPeers onFailure reasonCode = $reasonCode")
            }
        })
    }

    private fun initWifiDevicePeerList() {
        if (!checkWiFiPermission()) {
            Log.d(TAG, "initWifiDevicePeerList checkWiFiPermission fail")
            return
        }

        val peerListListener = WifiP2pManager.PeerListListener { peerList ->
            val peers = mutableListOf<WifiP2pDevice>()
            val refreshedPeers = peerList.deviceList
            if (refreshedPeers != peers) {
                peers.clear()
                peers.addAll(refreshedPeers)
            }

            if (peers.isEmpty()) {
                Log.d(TAG, "initWifiDevicePeerList No devices found")
            } else {
                Log.d(TAG, "initWifiDevicePeerList peers = $peers")
                adapter.setItems(peers)
            }
        }

        manager.requestPeers(channel, peerListListener)
    }

    private fun checkWiFiPermission(): Boolean {
        val isAccessFineLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val isNearbyWifiDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        Log.d(TAG, "checkWiFiPermission isNearbyWifiDevices = $isNearbyWifiDevices, isAccessFineLocation = $isAccessFineLocation")
        return isAccessFineLocation == PackageManager.PERMISSION_GRANTED && isNearbyWifiDevices == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = WiFiDirectBroadcastReceiver(manager, channel, this)
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
}