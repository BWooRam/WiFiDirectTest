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
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
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


class WiFiDirectServiceSearchActivity : AppCompatActivity(R.layout.main2) {
    private val TAG = "WiFiDirectServiceSearchActivity Log"
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

        findViewById<Button>(R.id.btAddLocalService).setOnClickListener { initAddLocalService() }
        findViewById<Button>(R.id.btDiscoverService).setOnClickListener { initDiscoverService() }

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
//            initWifiDevicePeerList()
        } else if (intent?.action == WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) {
//            initWiFiRequestConnectionInfo()
        }
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

        val servListener =
            DnsSdServiceResponseListener { instanceName, registrationType, resourceType ->
                Log.d(TAG, "DnsSdServiceResponseListener instanceName = $instanceName")
                Log.d(TAG, "DnsSdServiceResponseListener registrationType = $registrationType")
                Log.d(TAG, "DnsSdServiceResponseListener resourceType = $resourceType")

                if (resourceType != null)
                    adapter.addItem(resourceType)
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
                    Log.d(TAG, "discoverServices onSuccess")
                }

                override fun onFailure(code: Int) {
                    // Command failed. Check for P2P_UNSUPPORTED, ERROR, or BUSY
                    when (code) {
                        WifiP2pManager.P2P_UNSUPPORTED -> {
                            Log.d(
                                TAG,
                                "discoverServices onFailure Wi-Fi Direct isn't supported on this device."
                            )
                        }
                    }
                }
            }
        )
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
                    this@WiFiDirectServiceSearchActivity,
                    "WifiP2pManager connect onSuccess",
                    Toast.LENGTH_SHORT
                ).show()
                initWiFiRequestConnectionInfo()
            }

            override fun onFailure(reason: Int) {
                //failure logic
                Toast.makeText(
                    this@WiFiDirectServiceSearchActivity,
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
                this@WiFiDirectServiceSearchActivity,
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
                            Log.d(
                                TAG,
                                "ServerJob clientHost = $clientHost, clientPort = $clientPort"
                            )

                            val inputStream = ObjectInputStream(client.getInputStream())
                            val obj = inputStream.readObject()
                            Log.d(TAG, "ServerJob inputStream readObject = $obj")
                            handler.post {
                                Toast.makeText(
                                    this@WiFiDirectServiceSearchActivity,
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
                                    this@WiFiDirectServiceSearchActivity,
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

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = WiFiBroadcastReceiver(WiFiDirectServiceSearchActivity::class.java)
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