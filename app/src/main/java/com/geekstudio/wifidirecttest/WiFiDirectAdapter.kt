package com.geekstudio.wifidirecttest

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WiFiDirectAdapter : RecyclerView.Adapter<WiFiDirectAdapter.WiFiViewHolder>() {
    private val items = mutableListOf<WifiP2pDevice>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WiFiViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return WiFiViewHolder(layoutInflater.inflate(R.layout.item_wifi, parent, false))
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

    class WiFiViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        private val content = itemView.findViewById<TextView>(R.id.content)
        fun bind(wifiP2pDevice: WifiP2pDevice) {
            val info = "deviceName = ${wifiP2pDevice.deviceName}\ndeviceAddress = ${wifiP2pDevice.deviceAddress}"
            content.text = info
        }
    }
}