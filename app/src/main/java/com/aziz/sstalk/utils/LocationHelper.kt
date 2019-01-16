package com.aziz.sstalk.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import java.util.*

class LocationHelper(val context: Context) : GoogleApiClient.ConnectionCallbacks , GoogleApiClient.OnConnectionFailedListener
{
    private val apiClient: GoogleApiClient = GoogleApiClient.Builder(context)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(LocationServices.API)
        .build()

    init {
        connect()
    }

    fun connect() = apiClient.connect()

    fun disconnect() = apiClient.disconnect()


    fun getLastLocation(){



    }


    fun getAddress(latitude:Double, longitude:Double):Address?{

        if(!apiClient.isConnected) {
            utils.toast(context, "Location unavailable, check your connection")
            return null
        }

        return Geocoder(context, Locale.getDefault())
            .getFromLocation(latitude,longitude,1)[0]
    }


    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d("LocationHelper", "onConnectionFailed: ")
    }

    override fun onConnected(p0: Bundle?) {


        Log.d("LocationHelper", "onConnected:  ")

        LocationServices.getFusedLocationProviderClient(context)
            .lastLocation
            .addOnSuccessListener {
                if(it!=null){
                    Log.d("LocationHelper", "onConnected: on success = "+it.latitude)
                }
            }
            .addOnCompleteListener{task: Task<Location> ->

                if(task.isSuccessful && task.result!=null){
                    val latitude = task.result!!.latitude
                    val longitude = task.result!!.longitude

                    getAddress(latitude, longitude)

                }
                else{
                    Log.d("LocationHelper", "onConnected: "+task.exception.toString())
//                    Log.d("LocationHelper", "onConnected: exception = "+task.exception!!.message)
                }
            }

       // getAddress(getLastLocation().latitude, getLastLocation().longitude))
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d("LocationHelper", "onConnectionSuspended: ")

    }



}