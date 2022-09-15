package com.example.tapandrepair

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView

class BookingAdapter(private val list: MutableList<Booking>): RecyclerView.Adapter<BookingAdapter.Holder>() {
    class Holder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.booking_item, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val curr = list[position]
        holder.itemView.apply {
            val name = findViewById<TextView>(R.id.name)
            val status = findViewById<TextView>(R.id.status)
            val bookingCard = findViewById<CardView>(R.id.bookingCard)

            bookingCard.setOnClickListener{
                val intent = Intent(context, MechanicArrival::class.java)
                startActivity(context, intent, null)
            }
            name.text = "Name: ${curr.name}"
            status.text = "Status: ${curr.status}"
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
    fun add(item: Booking){
        list.add(item)
        notifyItemInserted(list.size - 1)
    }
}