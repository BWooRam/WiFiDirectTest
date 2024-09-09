package com.geekstudio.wifidirecttest

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class WiFiBroadcastReceiver(
    private val clazz: Class<out AppCompatActivity>
) : BroadcastReceiver() {
    private val TAG = "WiFiDirectBroadcastReceiver"


    override fun onReceive(context: Context, intent: Intent) {
        val action: String = intent.action.toString()
        when (action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // 기기의 Wi-Fi 연결 상태가 변경될 때 브로드캐스트합니다.
                Log.d(TAG, "onReceive WIFI_P2P_STATE_CHANGED_ACTION")
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // discoverPeers()를 호출할 때 브로드캐스트합니다.
                // 일반적으로 다음과 같은 경우 requestPeers()를 호출하여 업데이트된 동종 앱 목록을 가져옵니다.
                // 애플리케이션에서 이 인텐트를 처리할 수 있습니다.
                Log.d(TAG, "onReceive WIFI_P2P_STATE_CHANGED_ACTION")
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, clazz).apply {
                        setAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                    },
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                ).send()
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // 기기에서 Wi-Fi P2P가 활성화되었거나 비활성화되었는지 브로드캐스트합니다.
                Log.d(TAG, "onReceive WIFI_P2P_CONNECTION_CHANGED_ACTION")
                val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?

                if (networkInfo?.isConnected == true) {
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, clazz).apply {
                            setAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                        },
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    ).send()
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // 기기 이름과 같은 기기의 세부정보가 변경되면 브로드캐스트합니다.
                val device =
                    intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                Log.d(
                    TAG,
                    "onReceive WIFI_P2P_THIS_DEVICE_CHANGED_ACTION device = $device, deviceName = ${device?.deviceName}, deviceAddress = ${device?.deviceAddress}"
                )
            }

            WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED -> {
                Log.d(TAG, "onReceive ACTION_WIFI_AWARE_STATE_CHANGED")
                val wifiAwareManager =
                    context.getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager?

                if (wifiAwareManager?.isAvailable == true) {
                    Log.d(TAG, "onReceive ACTION_WIFI_AWARE_STATE_CHANGED isAvailable = true")
                } else {
                    Log.d(TAG, "onReceive ACTION_WIFI_AWARE_STATE_CHANGED isAvailable = false")
                }
            }
        }
    }
}