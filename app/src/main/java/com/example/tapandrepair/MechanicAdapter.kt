package com.example.tapandrepair

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.net.SocketTimeoutException

class MechanicAdapter(private val list: MutableList<Profile>, private val vehicleType: String, private val service: String, private val lat: Double, private val long: Double): RecyclerView.Adapter<MechanicAdapter.Holder>() {
    class Holder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.mechanic_item, parent, false))
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {
        var hasAccepted = false
        val curr = list[position]
        holder.itemView.apply {
            val name = findViewById<TextView>(R.id.name)
            val mechanic = findViewById<CardView>(R.id.mechanic)
            val progress = Progress(context)
            val alerts = Alerts(context)
            val db = TokenDB(context)
            val token = db.getToken()
            name.text = "${curr.first_name} ${curr.last_name}"

            mechanic.setOnClickListener{
                AlertDialog.Builder(context)
                    .setTitle("Confirm")
                    .setMessage("Book this mechanic?")
                    .setPositiveButton("YES"){_,_->
                        progress.showProgress("Booking...")
                        val jsonObject = JSONObject()
                        jsonObject.put("shop_mechanic_id", curr.id)
                        jsonObject.put("vehicle_type", vehicleType)
                        jsonObject.put("service", service)
                        jsonObject.put("lat", lat)
                        jsonObject.put("long", long)
                        val request = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
                        CoroutineScope(Dispatchers.IO).launch {
                            val bookResponse = try{ RetrofitInstance.retro.book("Bearer $token", request) }
                            catch(e: SocketTimeoutException){
                                withContext(Dispatchers.Main){
                                    progress.dismiss()
                                    alerts.socketTimeOut()
                                }
                                return@launch
                            }catch(e: HttpException){
                                withContext(Dispatchers.Main){
                                    progress.dismiss()
                                    AlertDialog.Builder(context)
                                        .setTitle("Error")
                                        .setMessage("You are currently booked to a mechanic/repair shop")
                                        .setPositiveButton("OK", null)
                                        .show()
                                }
                                return@launch
                            }catch(e: Exception){
                                withContext(Dispatchers.Main){
                                    progress.dismiss()
                                    alerts.error(e.toString())
                                }
                                return@launch
                            }

                            withContext(Dispatchers.Main){
                                progress.dismiss()
                                val bookingId = bookResponse.booking_id

                                Log.e("Booking Id", bookingId.toString())
                                val bookingSuccessAlert = AlertDialog.Builder(context)
                                    .setTitle("Success")
                                    .setCancelable(false)
                                    .setMessage("Mechanic Successfully Booked! Please Wait for the mechanic to accept your booking.")
                                    .setPositiveButton("OK", null)
                                val showbookingSuccessAlert = bookingSuccessAlert.show()
                                val thisJsonObject = JSONObject()
                                thisJsonObject.put("booking_id", bookingId)
                                val thisRequest = thisJsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

                                CoroutineScope(Dispatchers.IO).launch {
                                    while(!hasAccepted){

                                        val statusResponse = try{ RetrofitInstance.retro.checkBookingStatus("Bearer $token", thisRequest) }
                                        catch(e: Exception){
                                            Log.e("MechanicsAdapter", e.toString())
                                            return@launch
                                        }

                                        withContext(Dispatchers.Main){
                                            Log.e("MechanicsAdapter", statusResponse.status)
                                            if(statusResponse.status == "accepted"){
                                                hasAccepted = true
                                                showbookingSuccessAlert.dismiss()
                                                AlertDialog.Builder(context)
                                                    .setTitle("Success")
                                                    .setMessage("Your booking has been accepted by the mechanic/shop")
                                                    .setPositiveButton("OK", null)
                                                    .show()
                                            }
                                        }
                                        delay(5000)
                                    }
                                }
                            }
                        }
                    }.setNegativeButton("NO", null)
                    .show()
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun add(item: Profile){
        list.add(item)
        notifyItemInserted(list.size - 1)
    }
}