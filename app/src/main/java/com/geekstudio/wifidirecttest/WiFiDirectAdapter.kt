package com.geekstudio.wifidirecttest

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WiFiDirectAdapter : RecyclerView.Adapter<WiFiDirectAdapter.WiFiViewHolder>() {
    private val items = mutableListOf<WifiP2pDevice>()
    var onClickWiFiItemListener: OnClickWiFiItemListener? = null

    /**
     * WiFi P2P Device 클릭 이벤트
     */
    interface OnClickWiFiItemListener {
        fun onClick(wifiP2pDevice: WifiP2pDevice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WiFiViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return WiFiViewHolder(
            view = layoutInflater.inflate(R.layout.item_wifi, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: WiFiViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<WifiP2pDevice>) {
        this@WiFiDirectAdapter.items.clear()
        this@WiFiDirectAdapter.items.addAll(items)
        notifyDataSetChanged()
    }

    fun addItem(item: WifiP2pDevice) {
        this@WiFiDirectAdapter.items.add(0, item)
        notifyItemInserted(0)
    }

    inner class WiFiViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view), OnClickListener {
        private val content = itemView.findViewById<TextView>(R.id.content)

        fun bind(wifiP2pDevice: WifiP2pDevice) {
            val info = "deviceName = ${wifiP2pDevice.deviceName}\ndeviceAddress = ${wifiP2pDevice.deviceAddress}"
            content.text = info
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            kotlin.runCatching {
                items[adapterPosition]
            }.onSuccess { item ->
                onClickWiFiItemListener?.onClick(item)
            }.onFailure { error ->
                Log.d(this@WiFiDirectAdapter.javaClass.simpleName, "onClick onFailure error = $error")
            }
        }
    }
}