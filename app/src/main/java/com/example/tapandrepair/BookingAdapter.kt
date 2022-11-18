package com.example.tapandrepair

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView


class BookingAdapter(private val list: MutableList<CustomerTransactionHistoryItem>): RecyclerView.Adapter<BookingAdapter.Holder>() {
    class Holder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.booking_item, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val curr = list[position]
        holder.itemView.apply {
            val mechanic = findViewById<TextView>(R.id.mechanic)
            val service = findViewById<TextView>(R.id.service)
            val amount = findViewById<TextView>(R.id.amountCharged)
            val status = findViewById<TextView>(R.id.status)
            val date = findViewById<TextView>(R.id.date)

            mechanic.text = curr.mechanic
            service.text = curr.service
            amount.text = curr.amount_charged
            status.text = curr.status
            date.text = curr.date
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
    fun add(item: CustomerTransactionHistoryItem){
        list.add(item)
        notifyItemInserted(list.size - 1)
    }
}