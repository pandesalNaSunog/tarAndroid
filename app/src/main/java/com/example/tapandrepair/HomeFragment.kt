package com.example.tapandrepair

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.net.SocketTimeoutException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private lateinit var showServiceAlert: AlertDialog
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
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @SuppressLint("UseCompatLoadingForDrawables", "ResourceAsColor")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val findMechanic = view.findViewById<Button>(R.id.findMechanic)
        val findShop = view.findViewById<Button>(R.id.findShop)
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val client = LocationServices.getFusedLocationProviderClient(requireContext())
        val gpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)


        val progress = Progress(requireContext())
        val alerts = Alerts(requireContext())
        val db = TokenDB(requireContext())
        val token = db.getToken()

        var shopType = ""

        findMechanic.setOnClickListener {
            shopType = "mechanic"
            findMechanicFunction(shopType,client, progress, alerts, token)
        }

        findShop.setOnClickListener {
            shopType = "shop"
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.find_shop_choose_option, null)

            bottomSheetDialog.setContentView(sheetView)
            bottomSheetDialog.show()

            val shopLocations = sheetView.findViewById<Button>(R.id.shopLocations)
            val selectShop = sheetView.findViewById<Button>(R.id.selectShop)

            shopLocations.setOnClickListener {
                val intent = Intent(requireContext(), ShopMap::class.java)
                startActivity(intent)
            }

            selectShop.setOnClickListener {
                findMechanicFunction(shopType,client, progress, alerts, token)
            }


        }
    }

    @SuppressLint("MissingPermission")
    private fun findMechanicFunction(shopType: String, client: FusedLocationProviderClient, progress: Progress, alerts: Alerts, token: String) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        } else {
            val task = client.lastLocation
            task.addOnSuccessListener {
                latitude = it.latitude
                longitude = it.longitude
            }

            val servicesAlert = AlertDialog.Builder(requireContext())
            val servicesAlertView =
                LayoutInflater.from(requireContext()).inflate(R.layout.choose_service, null)
            var vehicle = "bike"
            var service = "general service"


            val bike = servicesAlertView.findViewById<Button>(R.id.bike)
            val car = servicesAlertView.findViewById<Button>(R.id.car)
            val motorbike = servicesAlertView.findViewById<Button>(R.id.motorbike)

            val generalService = servicesAlertView.findViewById<Button>(R.id.generalService)
            val puncture = servicesAlertView.findViewById<Button>(R.id.puncture)
            val battery = servicesAlertView.findViewById<Button>(R.id.battery)
            val search = servicesAlertView.findViewById<Button>(R.id.search)

            search.setOnClickListener {
                showServiceAlert.dismiss()


                if (shopType == "mechanic") {
                    progress.showProgress("We're Currently Searching Mechanics For You")
                    val jsonObject = JSONObject()
                    jsonObject.put("lat", latitude)
                    jsonObject.put("long", longitude)
                    val request = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
                    CoroutineScope(Dispatchers.IO).launch {
                        val mechanics = try {
                            RetrofitInstance.retro.getMechanics("Bearer $token", request)
                        } catch (e: SocketTimeoutException) {
                            withContext(Dispatchers.Main) {
                                progress.dismiss()
                                alerts.socketTimeOut()
                            }
                            return@launch
                        } catch (e: HttpException) {
                            withContext(Dispatchers.Main) {
                                progress.dismiss()
                                AlertDialog.Builder(context)
                                    .setTitle("Error")
                                    .setMessage("You're Currently Booked to a Mechanic/Shop")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                            return@launch
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                progress.dismiss()
                                alerts.error(e.toString())
                            }
                            return@launch
                        }

                        withContext(Dispatchers.Main) {
                            progress.dismiss()
                            val mechanicsAlert = AlertDialog.Builder(requireContext())
                            val mechanicsAlertView = LayoutInflater.from(requireContext())
                                .inflate(R.layout.suggested_mechanics, null)
                            mechanicsAlert.setView(mechanicsAlertView)

                            val mechanicAdapter = MechanicAdapter(
                                mutableListOf(),
                                vehicle,
                                service,
                                latitude,
                                longitude
                            )
                            val mechanicRecycler =
                                mechanicsAlertView.findViewById<RecyclerView>(R.id.mechanicsRecycler)
                            mechanicRecycler.adapter = mechanicAdapter
                            mechanicRecycler.layoutManager = LinearLayoutManager(requireContext())

                            mechanicsAlert.show()
                            for (i in mechanics.indices) {
                                mechanicAdapter.add(mechanics[i])
                            }
                        }
                    }
                } else {
                    progress.showProgress("We're Currently Searching Repair Shops For You")
                    val jsonObject = JSONObject()
                    jsonObject.put("lat", latitude)
                    jsonObject.put("long", longitude)
                    val request = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
                    CoroutineScope(Dispatchers.IO).launch {
                        val mechanics = try {
                            RetrofitInstance.retro.getShops("Bearer $token", request)
                        } catch (e: SocketTimeoutException) {
                            withContext(Dispatchers.Main) {
                                progress.dismiss()
                                alerts.socketTimeOut()
                            }
                            return@launch
                        } catch (e: HttpException) {
                            withContext(Dispatchers.Main) {
                                progress.dismiss()
                                AlertDialog.Builder(context)
                                    .setTitle("Error")
                                    .setMessage("You're Currently Booked to a Mechanic/Shop")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                            return@launch
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                progress.dismiss()
                                alerts.error(e.toString())
                            }
                            return@launch
                        }

                        withContext(Dispatchers.Main) {
                            progress.dismiss()
                            val mechanicsAlert = AlertDialog.Builder(requireContext())
                            val mechanicsAlertView = LayoutInflater.from(requireContext())
                                .inflate(R.layout.suggested_mechanics, null)
                            mechanicsAlert.setView(mechanicsAlertView)

                            val mechanicAdapter = MechanicAdapter(
                                mutableListOf(),
                                vehicle,
                                service,
                                latitude,
                                longitude
                            )
                            val mechanicRecycler =
                                mechanicsAlertView.findViewById<RecyclerView>(R.id.mechanicsRecycler)
                            mechanicRecycler.adapter = mechanicAdapter
                            mechanicRecycler.layoutManager = LinearLayoutManager(requireContext())

                            mechanicsAlert.show()
                            for (i in mechanics.indices) {
                                mechanicAdapter.add(mechanics[i])
                            }
                        }
                    }
                }
            }


            generalService.setOnClickListener {
                service = "general service"
                generalService.background =
                    getDrawable(requireContext(), R.drawable.solid_blue_button)
                generalService.setTextColor(Color.parseColor("#FFFFFF"))

                puncture.background =
                    getDrawable(requireContext(), R.drawable.stroke_blue_button)
                puncture.setTextColor(Color.parseColor("#00007D"))

                battery.background =
                    getDrawable(requireContext(), R.drawable.stroke_blue_button)
                battery.setTextColor(Color.parseColor("#00007D"))
            }
            puncture.setOnClickListener {
                service = "puncture and flat tyre"
                puncture.background =
                    getDrawable(requireContext(), R.drawable.solid_blue_button)
                puncture.setTextColor(Color.parseColor("#FFFFFF"))

                generalService.background =
                    getDrawable(requireContext(), R.drawable.stroke_blue_button)
                generalService.setTextColor(Color.parseColor("#00007D"))

                battery.background =
                    getDrawable(requireContext(), R.drawable.stroke_blue_button)
                battery.setTextColor(Color.parseColor("#00007D"))
            }
            battery.setOnClickListener {
                service = "battery shops"
                battery.background = getDrawable(requireContext(), R.drawable.solid_blue_button)
                battery.setTextColor(Color.parseColor("#FFFFFF"))

                puncture.background =
                    getDrawable(requireContext(), R.drawable.stroke_blue_button)
                puncture.setTextColor(Color.parseColor("#00007D"))

                generalService.background =
                    getDrawable(requireContext(), R.drawable.stroke_blue_button)
                generalService.setTextColor(Color.parseColor("#00007D"))
            }


            bike.setOnClickListener {
                vehicle = "bike"
                bike.background =
                    getDrawable(requireContext(),R.drawable.solid_blue_button)
                val img = getDrawable(requireContext(), R.drawable.bike_white)
                bike.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null)

                car.background =
                    getDrawable(requireContext(),R.drawable.stroke_blue_button)
                val imgblack = getDrawable(requireContext(), R.drawable.car_black)
                car.setCompoundDrawablesWithIntrinsicBounds(imgblack, null, null, null)

                motorbike.background =
                    getDrawable(requireContext(),R.drawable.stroke_blue_button)
                val imgblack2 = getDrawable(requireContext(), R.drawable.motorbike_black)
                motorbike.setCompoundDrawablesWithIntrinsicBounds(imgblack2, null, null, null)
            }
            car.setOnClickListener {
                vehicle = "car"
                car.background =
                    getDrawable(requireContext(),R.drawable.solid_blue_button)
                val img = getDrawable(requireContext(), R.drawable.car_white)
                car.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null)

                bike.background =
                    getDrawable(requireContext(),R.drawable.stroke_blue_button)
                val imgblack = getDrawable(requireContext(), R.drawable.bike_black)
                bike.setCompoundDrawablesWithIntrinsicBounds(imgblack, null, null, null)

                motorbike.background =
                    getDrawable(requireContext(),R.drawable.stroke_blue_button)
                val imgblack2 = getDrawable(requireContext(), R.drawable.motorbike_black)
                motorbike.setCompoundDrawablesWithIntrinsicBounds(imgblack2, null, null, null)
            }
            motorbike.setOnClickListener {
                vehicle = "motorbike"
                motorbike.background =
                    getDrawable(requireContext(),R.drawable.solid_blue_button)
                val img = getDrawable(requireContext(), R.drawable.motorbike_white)
                motorbike.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null)

                car.background =
                    getDrawable(requireContext(),R.drawable.stroke_blue_button)
                val imgblack = getDrawable(requireContext(), R.drawable.car_black)
                car.setCompoundDrawablesWithIntrinsicBounds(imgblack, null, null, null)

                bike.background =
                    getDrawable(requireContext(),R.drawable.stroke_blue_button)
                val imgblack2 = getDrawable(requireContext(), R.drawable.bike_black)
                bike.setCompoundDrawablesWithIntrinsicBounds(imgblack2, null, null, null)
            }
            servicesAlert.setView(servicesAlertView)
            showServiceAlert = servicesAlert.show()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}