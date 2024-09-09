package com.geekstudio.wifidirecttest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket


class WiFiDirectActivity : AppCompatActivity(R.layout.main) {
    private val TAG = "WiFiDirectActivity Log"
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var adapter: WiFiDirectAdapter

    private var receiver: WiFiBroadcastReceiver? = null

    private val port = 9999
    private val buf = ByteArray(1024)
    private var len: Int = 0
    private var hostInetAddress: InetAddress? = null
    private var tryCount = 0
    private val handler by lazy { Handler(mainLooper) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper) {
            Log.d(TAG, "WifiP2pManager onChannelDisconnected")
        }

        findViewById<Button>(R.id.btDiscoverPeers).setOnClickListener { initDiscoverPeers() }
        findViewById<Button>(R.id.btWifiDevicePeerList).setOnClickListener { initWifiDevicePeerList() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = WiFiDirectAdapter().apply {
            onClickWiFiItemListener = object : WiFiDirectAdapter.OnClickWiFiItemListener {
                override fun onClick(wifiP2pDevice: WifiP2pDevice) {
                    Log.d(TAG, "OnClickWiFiItemListener onClick wifiP2pDevice = $wifiP2pDevice")
                    initWiFiConnectDevice(wifiP2pDevice)
                }
            }
        }
        recyclerView.adapter = adapter
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent intent action = ${intent?.action}")

        if (intent?.action == WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) {
            initWifiDevicePeerList()
        } else if (intent?.action == WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) {
            initWiFiRequestConnectionInfo()
        }
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
                for(peer in peers){
                    Log.d(TAG, "initWifiDevicePeerList peer = $peer")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Log.d(TAG, "initWifiDevicePeerList peer wfdInfo = ${peer.wfdInfo}")
                    }
                }
                adapter.setItems(peers)
            }
        }

        manager.requestPeers(channel, peerListListener)
    }

    private fun initWiFiConnectDevice(device: WifiP2pDevice) {
        if (!checkWiFiPermission()) {
            Log.d(TAG, "initWiFiConnectDevice checkWiFiPermission fail")
            return
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                //success logic
                Toast.makeText(
                    this@WiFiDirectActivity,
                    "WifiP2pManager connect onSuccess",
                    Toast.LENGTH_SHORT
                ).show()
                initWiFiRequestConnectionInfo()
            }

            override fun onFailure(reason: Int) {
                //failure logic
                Toast.makeText(
                    this@WiFiDirectActivity,
                    "WifiP2pManager connect onFailure reason = $reason",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun initWiFiRequestConnectionInfo() {
        if (!checkWiFiPermission()) {
            Log.d(TAG, "initWiFiRequestConnectionInfo checkWiFiPermission fail")
            return
        }

        manager.requestConnectionInfo(channel) { info ->
            Toast.makeText(
                this@WiFiDirectActivity,
                "requestConnectionInfo info = ${info.groupOwnerAddress}",
                Toast.LENGTH_SHORT
            ).show()
            hostInetAddress = info.groupOwnerAddress

            Log.d(
                TAG,
                "initWiFiRequestConnectionInfo requestConnectionInfo groupFormed = ${info.groupFormed}, isGroupOwner = ${info.isGroupOwner}"
            )
            if (info.groupFormed && info.isGroupOwner) {
                //Server 역할
                CoroutineScope(Dispatchers.IO).launch {
                    val serverSocket = ServerSocket(port, 50, hostInetAddress)
                    Log.d(TAG, "ServerJob ServerSocket create Success")
                    serverSocket.use {
                        while (tryCount <= 20) {
                            tryCount++
                            val client = serverSocket.accept()
                            Log.d(TAG, "ServerJob accept Success tryCount = $tryCount")
                            val clientHost = client.localAddress
                            val clientPort = client.port
                            Log.d(TAG, "ServerJob clientHost = $clientHost, clientPort = $clientPort")

                            val inputStream = ObjectInputStream(client.getInputStream())
                            val obj = inputStream.readObject()
                            Log.d(TAG, "ServerJob inputStream readObject = $obj")
                            handler.post {
                                Toast.makeText(
                                    this@WiFiDirectActivity,
                                    "ServerJob inputStream readObject = $obj",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            val outputStream = ObjectOutputStream(client.getOutputStream())
                            outputStream.writeObject("serverJob Hello")
                            outputStream.flush()
                            Log.d(TAG, "ServerJob outputStream Success")
                            client.close()
                        }
                        serverSocket.close()
                    }
                }
            } else if (info.groupFormed) {
                //Client 역할
                CoroutineScope(Dispatchers.IO).launch {
                    val socket = Socket()
                    socket.use {
                        Log.d(TAG, "ClientJob Start")
                        socket.bind(null)
                        socket.connect(
                            (InetSocketAddress(hostInetAddress?.hostAddress, port)),
                            15000
                        )
                        Log.d(TAG, "ClientJob Connect Success")

                        val outputStream = ObjectOutputStream(socket.getOutputStream())
                        outputStream.writeObject("clientJob Hello")
                        outputStream.flush()
                        Log.d(TAG, "ClientJob outputStream Success")

                        val inputStream = socket.getInputStream()
                        while (inputStream.read(buf).also { len = it } != -1) {
                            Log.d(TAG, "ClientJob receiverJob buf = ${buf.decodeToString()}")
                            handler.post {
                                Toast.makeText(
                                    this@WiFiDirectActivity,
                                    "ClientJob receiverJob buf = ${buf.decodeToString()}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        inputStream.close()
                        socket.close()
                    }
                }
            }
        }
    }

    private fun initWiFiCreateGroup(device: WifiP2pDevice) {
        if (!checkWiFiPermission()) {
            Log.d(TAG, "initWiFiCreateGroup checkWiFiPermission fail")
            return
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        manager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                //success logic
                Log.d(TAG, "WifiP2pManager createGroup onSuccess")
            }

            override fun onFailure(reason: Int) {
                //failure logic
                Log.d(TAG, "WifiP2pManager createGroup onFailure reason = $reason")
            }
        })
    }

    private fun initWiFiDisConnectDevice() {
        if (!checkWiFiPermission()) {
            Log.d(TAG, "initWiFiDisConnectDevice checkWiFiPermission fail")
            return
        }

        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                //success logic
                Log.d(TAG, "WifiP2pManager removeGroup onSuccess")
            }

            override fun onFailure(reason: Int) {
                //failure logic
                Log.d(TAG, "WifiP2pManager removeGroup onFailure reason = $reason")
            }
        })
    }

    private fun initWiFiCancelDevice() {
        if (!checkWiFiPermission()) {
            Log.d(TAG, "initWiFiDisConnectDevice checkWiFiPermission fail")
            return
        }

        manager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                //success logic
                Log.d(TAG, "WifiP2pManager cancelConnect onSuccess")
            }

            override fun onFailure(reason: Int) {
                //failure logic
                Log.d(TAG, "WifiP2pManager cancelConnect onFailure reason = $reason")
            }
        })
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

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = WiFiBroadcastReceiver(manager, channel, this)
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        initWiFiCancelDevice()
    }
}