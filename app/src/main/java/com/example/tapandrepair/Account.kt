package com.example.tapandrepair

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Account.newInstance] factory method to
 * create an instance of this fragment.
 */
class Account : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progress = Progress(requireContext())
        val alerts = Alerts(requireContext())
        val db = TokenDB(requireContext())
        val name = view.findViewById<TextView>(R.id.name)
        val contact = view.findViewById<TextView>(R.id.contact)
        val bookingRecycler = view.findViewById<RecyclerView>(R.id.bookingRecycler)
        val bookingAdapter = BookingAdapter(mutableListOf())
        bookingRecycler.adapter = bookingAdapter
        bookingRecycler.layoutManager = LinearLayoutManager(requireContext())

        val token = db.getToken()
        progress.showProgress("Loading...")

        CoroutineScope(Dispatchers.IO).launch {
            val profile = try{ RetrofitInstance.retro.profile("Bearer $token") }
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

                name.text = "${profile.user.last_name}, ${profile.user.first_name}"
                contact.text = profile.user.contact_number

                for(i in profile.bookings.indices){
                    bookingAdapter.add(profile.bookings[i])
                }
            }
        }

        val logout = view.findViewById<Button>(R.id.logout)
        logout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are your sure you want to log out?")
                .setPositiveButton("YES"){_,_->
                    db.delete()
                    val intent = Intent(requireContext(), Home::class.java)
                    startActivity(intent)
                    activity?.finishAffinity()
                }
                .setNegativeButton("NO", null)
                .show()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Account.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Account().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}