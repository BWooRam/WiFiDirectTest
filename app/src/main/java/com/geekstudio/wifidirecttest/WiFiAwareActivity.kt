package com.geekstudio.wifidirecttest

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.random.Random

class WiFiAwareActivity : AppCompatActivity(R.layout.aware) {
    private val TAG = "WiFiAwareActivity Log"
    private val randomId = Random.nextInt(0, 1000)
    private var channel: WifiP2pManager.Channel? = null
    private var manager: WifiP2pManager? = null
    private var adapter: WiFiDirectAdapter? = null
    private var receiver: WiFiBroadcastReceiver? = null
    private var publishDiscoverySession: PublishDiscoverySession? = null
    private var publishPeerHandle: PeerHandle? = null
    private var subscribeDiscoverySession: SubscribeDiscoverySession? = null
    private var subscribePeerHandle: PeerHandle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            Toast.makeText(this, "FEATURE_WIFI_AWARE Not Feature", Toast.LENGTH_SHORT).show()
        }

        initWiFiAwarePublish()
        findViewById<Button>(R.id.btPublish).setOnClickListener { initWiFiAwarePublish() }
        findViewById<Button>(R.id.btSubscribe).setOnClickListener { initWiFiAwareSubscribe() }
    }

    private fun initWiFiAwarePublish() {
        val wifiAwareManager = getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager?
        wifiAwareManager?.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession?) {
                Log.d(TAG, "initWiFiAwarePublish onAttachFailed Start $randomId")
                if (!checkWiFiPermission()) {
                    return
                }

                val config: PublishConfig = PublishConfig.Builder()
                    .setServiceName("Test")
                    .build()

                session?.publish(config, object : DiscoverySessionCallback() {
                    override fun onPublishStarted(session: PublishDiscoverySession) {
                        Log.d(TAG, "publish onPublishStarted Start $randomId")
                        publishDiscoverySession = session
                    }


                    override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                        Log.d(TAG, "publish onMessageReceived Start $randomId message = ${message.decodeToString()}")
                    }

                    override fun onServiceDiscovered(
                        peerHandle: PeerHandle?,
                        serviceSpecificInfo: ByteArray?,
                        matchFilter: MutableList<ByteArray>?
                    ) {
                        Log.d(TAG, "publish onServiceDiscovered Start $randomId peerHandle = $peerHandle, serviceSpecificInfo = ${serviceSpecificInfo.contentToString()}")
                        publishPeerHandle = peerHandle

                        if(publishPeerHandle != null)
                            publishDiscoverySession?.sendMessage(publishPeerHandle!!, 1, "$randomId publish Hello".toByteArray())
                    }
                }, Handler(mainLooper))
            }

            override fun onAttachFailed() {
                Log.d(TAG, "initWiFiAwarePublish onAttachFailed Start $randomId")
            }

            override fun onAwareSessionTerminated() {
                Log.d(TAG, "initWiFiAwarePublish onAwareSessionTerminated Start $randomId")
            }
        }, Handler(mainLooper))
    }

    private fun initWiFiAwareSubscribe() {
        val wifiAwareManager = getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager?
        wifiAwareManager?.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession?) {
                Log.d(TAG, "initWiFiAwareSubscribe onAttachFailed Start $randomId")
                if (!checkWiFiPermission()) {
                    return
                }

                val config: SubscribeConfig  = SubscribeConfig.Builder()
                    .setServiceName("Test")
                    .build()

                session?.subscribe(config, object : DiscoverySessionCallback() {
                    override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                        Log.d(TAG, "subscribe onSubscribeStarted Start $randomId")
                        subscribeDiscoverySession = session
                    }

                    override fun onServiceDiscovered(
                        peerHandle: PeerHandle?,
                        serviceSpecificInfo: ByteArray?,
                        matchFilter: MutableList<ByteArray>?
                    ) {
                        Log.d(TAG, "subscribe onServiceDiscovered Start $randomId peerHandle = $peerHandle, serviceSpecificInfo = ${serviceSpecificInfo.contentToString()}")
                        subscribePeerHandle = peerHandle

                        if(subscribePeerHandle != null)
                            subscribeDiscoverySession?.sendMessage(subscribePeerHandle!!, 2, "$randomId subscribe Hello".toByteArray())
                    }

                    override fun onMessageReceived(peerHandle: PeerHandle?, message: ByteArray?) {
                        Log.d(TAG, "subscribe onMessageReceived Start $randomId message = ${message?.decodeToString()}")
                    }
                }, Handler(mainLooper))
            }

            override fun onAttachFailed() {
                Log.d(TAG, "initWiFiAwareSubscribe onAttachFailed Start $randomId")
            }

            override fun onAwareSessionTerminated() {
                Log.d(TAG, "initWiFiAwareSubscribe onAwareSessionTerminated Start $randomId")
            }
        }, Handler(mainLooper))
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
            addAction(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED)
        }

        receiver = WiFiBroadcastReceiver(manager, channel, this)
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
}