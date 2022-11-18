package com.example.tapandrepair

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.net.SocketTimeoutException

class MechanicAdapter(private val list: MutableList<MechanicDetailsItem>, private val vehicleType: String, private val service: String, private val lat: Double, private val long: Double): RecyclerView.Adapter<MechanicAdapter.Holder>() {
    class Holder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.mechanic_item, parent, false))
    }
    lateinit var showDeniedAlert: AlertDialog
    override fun onBindViewHolder(holder: Holder, position: Int) {
        var hasAccepted = false
        val curr = list[position]
        holder.itemView.apply {

            val name = findViewById<TextView>(R.id.name)
            val mechanicCard = findViewById<CardView>(R.id.mechanic)
            val rating = findViewById<TextView>(R.id.rating)
            val progress = Progress(context)
            val alerts = Alerts(context)
            val db = TokenDB(context)
            val token = db.getToken()
            val distance = findViewById<TextView>(R.id.distance)
            val shopType = findViewById<TextView>(R.id.shopType)
            name.text = "${curr.mechanic.first_name} ${curr.mechanic.last_name}"
            rating.text = curr.average_rating.toString()
            distance.text = "${curr.distance} km."
            shopType.text = curr.mechanic.shop_type
            mechanicCard.setOnClickListener{
                Log.e("hjaf", "hjahfaf")
                val bookAlert = AlertDialog.Builder(context)
                val bookAlertView = LayoutInflater.from(context).inflate(R.layout.view_mechanic_profile, null)
                bookAlert.setView(bookAlertView)
                val book = bookAlertView.findViewById<Button>(R.id.book)
                val cancel = bookAlertView.findViewById<Button>(R.id.cancel)
                val thisName = bookAlertView.findViewById<TextView>(R.id.name)
                val averageRating = bookAlertView.findViewById<TextView>(R.id.averageRating)

                thisName.text = name.text.toString()
                averageRating.text = rating.text.toString()

                val showBookAlert = bookAlert.show()

                cancel.setOnClickListener {
                    showBookAlert.dismiss()
                }
                book.setOnClickListener {
                    progress.showProgress("Booking...")
                    val jsonObject = JSONObject()
                    jsonObject.put("shop_mechanic_id", curr.mechanic.id)
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
                                Log.e("Error", e.toString())
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
                            val bookingSuccessAlertView = LayoutInflater.from(context).inflate(R.layout.waiting_to_accept_booking, null)
                            bookingSuccessAlert.setCancelable(false)
                            bookingSuccessAlert.setView(bookingSuccessAlertView)
                            val showBookingSuccessAlert = bookingSuccessAlert.show()

                            val cancelBooking = bookingSuccessAlertView.findViewById<Button>(R.id.cancel)

                            cancelBooking.setOnClickListener {
                                AlertDialog.Builder(context)
                                    .setTitle("Cancel")
                                    .setMessage("Cancel Booking?")
                                    .setPositiveButton("YES"){_,_->
                                        progress.showProgress("Please Wait...")
                                        val cancelBookingJson = JSONObject()

                                        cancelBookingJson.put("booking_id", bookingId)

                                        val cancelBookingRequest = cancelBookingJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val cancelBookingResponse = try{ RetrofitInstance.retro.cancelBooking("Bearer $token", cancelBookingRequest) }
                                            catch(e: SocketTimeoutException){
                                                withContext(Dispatchers.Main){
                                                    progress.dismiss()
                                                    alerts.socketTimeOut()
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
                                                if(cancelBookingResponse.status == "cancelled by the customer"){
                                                    showBookingSuccessAlert.dismiss()
                                                }
                                            }
                                        }
                                    }.setNegativeButton("NO", null)
                                    .show()
                            }

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
                                            showBookingSuccessAlert.dismiss()
                                            AlertDialog.Builder(context)
                                                .setTitle("Success")
                                                .setMessage("Your booking has been accepted by the mechanic/shop")
                                                .setCancelable(false)
                                                .setPositiveButton("OK"){_,_->
                                                    val intent = Intent(context, MechanicArrival::class.java)
                                                    intent.putExtra("shop_mechanic_id", statusResponse.shop_mechanic_id)
                                                    intent.putExtra("name", statusResponse.shop_mechanic_name)
                                                    intent.putExtra("booking_id", bookingId)
                                                    startActivity(context, intent, null)
                                                }
                                                .show()
                                        }else if(statusResponse.status == "denied"){
                                            hasAccepted = true
                                            val deniedAlert = AlertDialog.Builder(context)
                                                .setTitle("Denied")
                                                .setMessage("Your booking has been denied by the mechanic/shop")
                                                .setCancelable(false)
                                                .setPositiveButton("OK"){_,_->
                                                    showBookingSuccessAlert.dismiss()
                                                    showBookAlert.dismiss()
                                                    showDeniedAlert.dismiss()
                                                }
                                            showDeniedAlert = deniedAlert.show()
                                        }else{
                                            Log.e("jkljadf", "jklajf")
                                        }
                                    }
                                    delay(5000)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun add(item: MechanicDetailsItem){
        list.add(item)
        notifyItemInserted(list.size - 1)
    }
}