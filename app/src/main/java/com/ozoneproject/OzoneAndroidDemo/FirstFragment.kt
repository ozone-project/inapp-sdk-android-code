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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.ozoneproject.OzoneAndroidDemo.R
import com.ozoneproject.OzoneAndroidDemo.databinding.FragmentFirstBinding
import org.prebid.mobile.BannerAdUnit
import org.prebid.mobile.BannerParameters
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.Signals
import org.prebid.mobile.TargetingParams
import org.prebid.mobile.api.data.InitializationStatus


private const val TAG = "FirstFragment"


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), LocationListener {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // from prebid code
    companion object {
        const val CONFIG_ID = "8000000328"
        const val WIDTH = 300
        const val HEIGHT = 250
    }
    private var adUnit: BannerAdUnit? = null
    var lastLocation: Location? = null
    var locationPermissionGranted: Boolean = false



    // https://developer.android.com/training/permissions/requesting#kotlin
    // https://developer.android.com/training/basics/intents/result#kotlin
    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your app.
                Log.d(TAG, "User granted permission for location")
            } else {
                Log.d(TAG, "user denied permission for location")
            }
            locationPermissionGranted = isGranted
            Log.d(TAG, "Going to restart the ad, with current geo settings")
            adUnit?.stopAutoRefresh()
            createAd()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG,"*** Pausing frag 1")
        adUnit?.stopAutoRefresh()
    }

    /**
     * this is called after onViewCreated
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "*** resuming frag 1")
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
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "*** onViewCreated")

        getLocation()
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        // set test devices like this
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(listOf("ABCDEF012345")).build()
        )

        // from prebid code
        // get the application context form the main activity https://stackoverflow.com/questions/12659747/call-an-activity-method-from-a-fragment
        PrebidMobile.initializeSdk(activity?.applicationContext) { status ->
            if (status == InitializationStatus.SUCCEEDED) {
                Log.d(TAG, "SDK initialized successfully!")
            } else if (status == InitializationStatus.SERVER_STATUS_WARNING) {
                Log.d(TAG, "Prebid server status check failed: $status\n${status.description}")
            } else {
                Log.e(TAG, "SDL initialization error : $status\n${status.description}")
            }
        }
        // do not call the adserver, without prebid, eg:
        // val request = AdManagerAdRequest.Builder().build()
        //  binding.adView.loadAd(request)
        // you will do this in the prebid fetchDemand call back function

    }


    // code snippets from prebid example https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html
    private fun createAd() {

        // 1. Create BannerAdUnit
        adUnit = BannerAdUnit(CONFIG_ID, WIDTH, HEIGHT)
        adUnit?.setAutoRefreshInterval(30) // IF you want to auto refresh the ad. This makes a call to the Ozone prebid adserver each time.

        // 2. Configure banner parameters
        val parameters = BannerParameters()
        parameters.api = listOf(Signals.Api.MRAID_3, Signals.Api.OMID_1)
        adUnit?.bannerParameters = parameters

        // make changes for Ozone
        Log.d(TAG, "Setting Ozone vars")

        PrebidMobile.setPrebidServerHost(Host.createCustomHost("https://elb.the-ozone-project.com/openrtb2/app"))
        PrebidMobile.setCustomStatusEndpoint("https://elb.the-ozone-project.com/status")

        PrebidMobile.setPrebidServerAccountId("OZONEGMG0001")
        TargetingParams.setDomain("ardm.io")
        TargetingParams.setStoreUrl("google play store url here")
        TargetingParams.setBundleName("this is the bundleName")
        TargetingParams.setAppPageName("https://www.ardm.io/news")
        TargetingParams.setSubjectToCOPPA(false) // false by default

        // OMSDK settings, optional - see https://docs.prebid.org/prebid-mobile/pbm-api/android/pbm-targeting-params-android.html
        TargetingParams.setUserAge(99)
        TargetingParams.setGender(TargetingParams.GENDER.FEMALE)


        // Prebid docs: https://docs.prebid.org/prebid-mobile/prebid-mobile-privacy-regulation.html
        // these 2 should be set automatically in default shared preferences but Usercentrics seems not to set SubjectToGDPR
        TargetingParams.setSubjectToGDPR(true)
//    TargetingParams.setGDPRConsentString("...") // see https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#how-do-third-party-sdks-vendors-access-the-consent-information-in-app
//        TargetingParams.setPublisherName() // do not set this - it's not useful
        TargetingParams.setOmidPartnerName("Google1")
        TargetingParams.setOmidPartnerVersion("3.16.3")
        PrebidMobile.setShareGeoLocation(getLocationTrackingOK())
        if(getLocationTrackingOK()) {
            Log.d(TAG, "setting location info in the auction call")
            TargetingParams.setUserLatLng(lastLocation?.latitude?.toFloat(), lastLocation?.longitude?.toFloat())
        } else {
            Log.d(TAG, "NOT going to set location info in the auction call")
        }

        // 4. Make a bid request to Prebid Server
        val request = AdManagerAdRequest.Builder().build()
        adUnit?.fetchDemand(request) {
            // inside the callback we will call for an ad. Prebid will have set targeting keys on the request object, ready to send to the adserver.
            Log.d(TAG, "fetchDemand callback. request targeting is: " + request.customTargeting.toString())
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
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
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