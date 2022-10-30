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
import android.widget.CheckBox
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
    private lateinit var servicesSheetView: View
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
            var vehicle = "Bicycle"
            var service = "general service"


            val bike = servicesAlertView.findViewById<Button>(R.id.bike)
            val car = servicesAlertView.findViewById<Button>(R.id.car)
            val motorbike = servicesAlertView.findViewById<Button>(R.id.motorbike)

            bike.setOnClickListener {
                vehicle = "Bicycle"
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

                openServices(vehicle,shopType, progress,alerts, token)
            }
            car.setOnClickListener {
                vehicle = "Car"
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
                openServices(vehicle,shopType, progress,alerts, token)
            }
            motorbike.setOnClickListener {
                vehicle = "Motorcycle"
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
                openServices(vehicle,shopType, progress,alerts, token)
            }
            servicesAlert.setView(servicesAlertView)
            showServiceAlert = servicesAlert.show()
        }
    }

    private fun openServices(vehicle: String, shopType: String, progress: Progress, alerts: Alerts, token: String){
        val servicesSheet = BottomSheetDialog(requireContext())
        val servicesArray = ArrayList<CheckBox>()

        if(vehicle == "Bicycle"){
            servicesSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bike_services, null)

            val generalService = servicesSheetView.findViewById<CheckBox>(R.id.generalService)
            val gearTune = servicesSheetView.findViewById<CheckBox>(R.id.gearTune)
            val bolts = servicesSheetView.findViewById<CheckBox>(R.id.bolts)
            val wheel = servicesSheetView.findViewById<CheckBox>(R.id.wheelAlignment)

            servicesArray.add(generalService)
            servicesArray.add(gearTune)
            servicesArray.add(bolts)
            servicesArray.add(wheel)
        }else if(vehicle == "Motorcycle"){
            servicesSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.motorcycle_services, null)
            val generalService = servicesSheetView.findViewById<CheckBox>(R.id.generalService)
            val changeOil = servicesSheetView.findViewById<CheckBox>(R.id.changeOil)
            val airFilter = servicesSheetView.findViewById<CheckBox>(R.id.airFilter)
            val tirePressure = servicesSheetView.findViewById<CheckBox>(R.id.tirePressure)
            val coolant = servicesSheetView.findViewById<CheckBox>(R.id.coolant)
            val clean = servicesSheetView.findViewById<CheckBox>(R.id.clean)
            servicesArray.add(generalService)
            servicesArray.add(changeOil)
            servicesArray.add(airFilter)
            servicesArray.add(tirePressure)
            servicesArray.add(coolant)
            servicesArray.add(clean)
        }else{
            servicesSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.car_services, null)
            val full = servicesSheetView.findViewById<CheckBox>(R.id.full)
            val airFilter = servicesSheetView.findViewById<CheckBox>(R.id.airFilter)
            val dual = servicesSheetView.findViewById<CheckBox>(R.id.dual)
            val extensive = servicesSheetView.findViewById<CheckBox>(R.id.extensiveBreak)
            val wheelBearings = servicesSheetView.findViewById<CheckBox>(R.id.wheelBearings)
            val electrical = servicesSheetView.findViewById<CheckBox>(R.id.electrical)
            val aircon = servicesSheetView.findViewById<CheckBox>(R.id.aircon)
            val radiator = servicesSheetView.findViewById<CheckBox>(R.id.radiator)
            servicesArray.add(full)
            servicesArray.add(dual)
            servicesArray.add(airFilter)
            servicesArray.add(extensive)
            servicesArray.add(wheelBearings)
            servicesArray.add(electrical)
            servicesArray.add(aircon)
            servicesArray.add(radiator)
        }
        val search = servicesSheetView.findViewById<Button>(R.id.search)
        servicesSheet.setContentView(servicesSheetView)
        servicesSheet.show()

        search.setOnClickListener {
            showServiceAlert.dismiss()
            var serviceString = ""
            val checkedServicesArray = ArrayList<CheckBox>()

            for(i in servicesArray.indices){
                if(servicesArray[i].isChecked){
                    checkedServicesArray.add(servicesArray[i])
                }
            }

            for(i in checkedServicesArray.indices){
                serviceString += if(i + 1 != checkedServicesArray.size){
                    "${checkedServicesArray[i].text}/"
                }else{
                    "${checkedServicesArray[i].text}"
                }
            }

            if (shopType == "mechanic") {
                progress.showProgress("We're Currently Searching Mechanics For You")
                val jsonObject = JSONObject()
                jsonObject.put("lat", latitude)
                jsonObject.put("long", longitude)
                jsonObject.put("vehicle", vehicle)
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
                            serviceString,
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
                jsonObject.put("vehicle", vehicle)
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
                            serviceString,
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