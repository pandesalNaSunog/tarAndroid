package com.example.tapandrepair

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActivityLogAdapter(private val list: MutableList<ActivityLogItem>): RecyclerView.Adapter<ActivityLogAdapter.Holder>() {
    class Holder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.activity_log_item, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val curr = list[position]

        holder.itemView.apply {
            val activity = findViewById<TextView>(R.id.activity)
            val date = findViewById<TextView>(R.id.date)

            activity.text = curr.activity
            date.text = curr.created_at
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun add(item: ActivityLogItem){
        list.add(item)
        notifyItemInserted(list.size - 1)
    }
}