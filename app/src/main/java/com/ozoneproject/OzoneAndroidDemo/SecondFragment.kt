package com.ozoneproject.OzoneAndroidDemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.ozoneproject.OzoneAndroidDemo.R
import com.ozoneproject.OzoneAndroidDemo.databinding.FragmentSecondBinding
import org.prebid.mobile.BannerAdUnit
import org.prebid.mobile.BannerParameters
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.Signals
import org.prebid.mobile.TargetingParams
import org.prebid.mobile.api.data.InitializationStatus


/*

this fragment displays an outstream ad (requested at 300x179) in a banner ad slot (300x250)

 */


private const val TAG = "SecondFragment"
/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment(), LocationListener {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val parameters = BannerParameters()


    // NOTE to request a video ad you request 300x179 from prebid, then display in a 300x250 banner ad slot
    companion object {
        const val CONFIG_ID = "8000000328"
        const val WIDTH = 300
        const val HEIGHT = 179
    }
    private var adUnit: BannerAdUnit? = null
    var lastLocation: Location? = null
    var locationPermissionGranted: Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.e(TAG, " *** LOW MEMORY DETECTED *** ");
    }


    override fun onPause() {
        super.onPause()
        Log.d(TAG,"*** Pausing frag 2")
//        adUnit?.stopAutoRefresh()
    }

    /**
     * this is called after onViewCreated
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "*** resuming frag 2")
        if(PrebidMobile.isSdkInitialized()) {
            // when switching back to this screen
            Log.d(TAG, "onResume: Going to load ad")
            createAd()
        } else {
            Log.d(TAG, "onResume: Not going to load ad")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "*** onViewCreated")

        // check whether the user allows geo location
        getLocation()

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_thirdFragment)
        }


    }

    // code snippets from prebid example https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html
    private fun createAd() {

        // 1. Create BannerAdUnit
        adUnit = BannerAdUnit(CONFIG_ID, WIDTH, HEIGHT)
//        adUnit?.setAutoRefreshInterval(60) // IF you want to auto refresh the ad; outstream would probably not refresh tho

        // 2. Configure banner parameters
        parameters.api = listOf(Signals.Api.MRAID_3, Signals.Api.OMID_1)
        adUnit?.bannerParameters = parameters

        // make changes for Ozone
        Log.d(TAG, "Setting Ozone vars")

        TargetingParams.setAppPageName("https://www.ardm.io/sport")

        PrebidMobile.setShareGeoLocation(getLocationTrackingOK())
        if(getLocationTrackingOK()) {
            TargetingParams.setUserLatLng(lastLocation?.latitude?.toFloat(), lastLocation?.longitude?.toFloat())
        }

        // 4. Make a bid request to Prebid Server
        val request = AdManagerAdRequest.Builder().build()
        adUnit?.fetchDemand(request) {
            // inside the callback we will call for an ad. Prebid will have set the targeting keys
            Log.d(TAG, "fetchDemand callback. request targeting is: " + request.customTargeting.toString())
            // both of these have to be set the same way - either in xml or in code
            binding.textOutput.text = "fetchDemand got targeting: " + request.customTargeting.toString()
            binding.adView.loadAd(request)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getLocationTrackingOK(): Boolean {
        Log.d(TAG, "getLocationTrackingOK : $locationPermissionGranted")
        return locationPermissionGranted
    }


    /**
     * See if we can get the users location, and if so store it in lastLocation
     */
    private fun getLocation() {
        if (activity?.applicationContext?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED && activity?.applicationContext?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "user has not granted location permission")
//            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            locationPermissionGranted = false // currently the permission is false
        } else {
            Log.d(TAG, "user has granted location permission")
            val mgr = activity?.applicationContext!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lastLocation = mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            locationPermissionGranted = true
        }
    }

    /**
     * interface function for LocationListener
     */
    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "onLocationChanged with location $location")
        Log.d(
            TAG, String.format(
            "Lat:\t %f\nLong:\t %f\nAlt:\t %f\nBearing:\t %f", location.latitude,
            location.longitude, location.altitude, location.bearing
        ))
        lastLocation = location
    }

}