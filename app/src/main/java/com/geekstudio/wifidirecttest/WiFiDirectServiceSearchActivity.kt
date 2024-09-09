package com.geekstudio.wifidirecttest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import java.net.InetAddress


class WiFiDirectServiceSearchActivity : AppCompatActivity(R.layout.main2) {
    private val TAG = "WiFiDirectServiceSearchActivity Log"
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var adapter: WiFiDirectAdapter

    private val port = 9999

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper) {
            Log.d(TAG, "WifiP2pManager onChannelDisconnected")
        }

        findViewById<Button>(R.id.btAddLocalService).setOnClickListener { initAddLocalService() }
        findViewById<Button>(R.id.btDiscoverService).setOnClickListener { initDiscoverService() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = WiFiDirectAdapter().apply {
            onClickWiFiItemListener = object : WiFiDirectAdapter.OnClickWiFiItemListener {
                override fun onClick(wifiP2pDevice: WifiP2pDevice) {
                    Log.d(TAG, "OnClickWiFiItemListener onClick wifiP2pDevice = $wifiP2pDevice")
                }
            }
        }
        recyclerView.adapter = adapter
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent intent action = ${intent?.action}")

/*        if (intent?.action == WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) {
            initWifiDevicePeerList()
        } else if (intent?.action == WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) {
            initWiFiRequestConnectionInfo()
        }*/
    }

    private fun initAddLocalService() {
        if (!checkWiFiPermission()) {
            Log.d(TAG, "initDiscoverPeers checkWiFiPermission fail")
            return
        }

        val record: Map<String, String> = mapOf(
            "listenport" to port.toString(),
            "buddyname" to "BuddyName Test",
            "available" to "visible"
        )

        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record)

        manager.addLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "addLocalService onSuccess")
            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "addLocalService onFailure reason = $reason")
            }
        })
    }

    private fun initDiscoverService() {
        if (!checkWiFiPermission()) {
            Log.d(TAG, "initDiscoverPeers checkWiFiPermission fail")
            return
        }

        val txtListener = DnsSdTxtRecordListener { fullDomain, record, device ->
            Log.d(TAG, "DnsSdTxtRecordListener fullDomain = $fullDomain")
            Log.d(TAG, "DnsSdTxtRecordListener record = $record")
            Log.d(TAG, "DnsSdTxtRecordListener device = $device")
        }

        val servListener = DnsSdServiceResponseListener { instanceName, registrationType, resourceType ->
            Log.d(TAG, "DnsSdServiceResponseListener instanceName = $instanceName")
            Log.d(TAG, "DnsSdServiceResponseListener registrationType = $registrationType")
            Log.d(TAG, "DnsSdServiceResponseListener resourceType = $resourceType")
        }

        manager.setDnsSdResponseListeners(channel, servListener, txtListener)

        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
        manager.addServiceRequest(
            channel,
            serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "addServiceRequest onSuccess")
                }

                override fun onFailure(reason: Int) {
                    // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                    Log.d(TAG, "addServiceRequest onFailure reason = $reason")
                }
            }
        )

        manager.discoverServices(
            channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Success!
                }

                override fun onFailure(code: Int) {
                    // Command failed. Check for P2P_UNSUPPORTED, ERROR, or BUSY
                    when (code) {
                        WifiP2pManager.P2P_UNSUPPORTED -> {
                            Log.d(TAG, "Wi-Fi Direct isn't supported on this device.")
                        }
                    }
                }
            }
        )
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

        Log.d(
            TAG,
            "checkWiFiPermission isNearbyWifiDevices = $isNearbyWifiDevices, isAccessFineLocation = $isAccessFineLocation"
        )
        return isAccessFineLocation == PackageManager.PERMISSION_GRANTED && isNearbyWifiDevices == PackageManager.PERMISSION_GRANTED
    }
}