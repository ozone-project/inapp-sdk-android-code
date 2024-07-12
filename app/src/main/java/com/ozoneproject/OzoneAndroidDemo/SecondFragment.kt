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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.ozoneproject.OzoneAndroidDemo.databinding.FragmentSecondBinding
import org.prebid.mobile.BannerAdUnit
import org.prebid.mobile.BannerParameters
import org.prebid.mobile.InterstitialAdUnit
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.Signals
import org.prebid.mobile.TargetingParams
import org.prebid.mobile.VideoParameters
import java.util.Arrays

// https://developers.google.com/admob/android/interstitial
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;


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

    private var parameters = BannerParameters()
    private var interstitialBannerParams = BannerParameters()
    private var interstitialVideoParams = VideoParameters(listOf("video/mp4"))
    private var interstitialShown: Boolean = false

    // NOTE to request a video ad you request 300x179 from prebid, then display in a 300x250 banner ad slot
    companion object {
        const val CONFIG_ID = "8000000328"
        const val WIDTH = 300
        const val HEIGHT = 179
    }
    private var adUnit: BannerAdUnit? = null
    private var interstitialAdUnit: InterstitialAdUnit? = null;
    var lastLocation: Location? = null
    var locationPermissionGranted: Boolean = false
    private var mInterstitialAd: InterstitialAd? = null // https://developers.google.com/admob/android/interstitial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.interstitialShown = false

        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                mInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                mInterstitialAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }

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

    private var context: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    /**
     * this is called after onViewCreated
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "*** resuming frag 2")

        Log.d(TAG, "Setting test device ID")
        RequestConfiguration.Builder().setTestDeviceIds(listOf("EE4361CC35F01AE142E261039CD1A893")).build()
        if(PrebidMobile.isSdkInitialized()) {
            // when switching back to this screen
            if(this.interstitialShown) {
                Log.d(TAG, "onResume: Going to load ad")
                createAd()
            } else {
                Log.d(TAG, "onResume: Going to load interstitial ad")
                showInterstitial()
                this.interstitialShown = true
            }
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
        adUnit?.ozoneSetImpAdUnitCode("mpu")

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

    private fun showInterstitial() {

        // https://docs.prebid.org/prebid-mobile/modules/rendering/android-sdk-integration-gam.html#interstitial-api <-- note that this is for a newer version

        // test using a different placementId for interstitial
        interstitialAdUnit = InterstitialAdUnit("8000000328", 80, 80)

        // 2. Configure banner parameters
        interstitialBannerParams.api = listOf(Signals.Api.MRAID_1, Signals.Api.MRAID_2, Signals.Api.MRAID_3, Signals.Api.OMID_1 )
        interstitialAdUnit?.bannerParameters = interstitialBannerParams

//        interstitialVideoParams.protocols = listOf(Signals.Protocols.VAST_2_0)
//        interstitialVideoParams.playbackMethod = listOf(Signals.PlaybackMethod.AutoPlaySoundOff)
//        interstitialVideoParams.api = listOf(Signals.Api.MRAID_1, Signals.Api.MRAID_2, Signals.Api.MRAID_3, Signals.Api.OMID_1 )
//        interstitialAdUnit?.videoParameters = interstitialVideoParams
        interstitialAdUnit?.ozoneSetImpAdUnitCode("mpu")

        // make changes for Ozone
        Log.d(TAG, "Setting Ozone vars for interstitial")

        TargetingParams.setAppPageName("https://www.ardm.io/sport")

        PrebidMobile.setShareGeoLocation(getLocationTrackingOK())
        if(getLocationTrackingOK()) {
            TargetingParams.setUserLatLng(lastLocation?.latitude?.toFloat(), lastLocation?.longitude?.toFloat())
        }

        // 4. Make a bid request to Prebid Server
        val request = AdManagerAdRequest.Builder().build()
        interstitialAdUnit?.fetchDemand(request) {
            // inside the callback we will call for an ad. Prebid will have set the targeting keys
            Log.d(TAG, "fetchDemand callback for instl. request targeting is: " + request.customTargeting.toString())
//            binding.interstitialAdView.setAdSize( AdSize(400, 800))
//            binding.interstitialAdView.adUnitId = "/22037345/inapp-test-adunit"
//            binding.interstitialAdView.loadAd(request)

            // https://developers.google.com/admob/android/interstitial
            // how to send a nullable var to a function that expects a non-nullable parameter
            activity?.applicationContext?.let {
                Log.d(TAG, "Going to load interstitial ad...")
                InterstitialAd.load(it, "/22037345/inapp-test-adunit", request, object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, "Failed to load ad: " + adError.toString())
                        mInterstitialAd = null
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d(TAG, "Ad was loaded.")
                        mInterstitialAd = interstitialAd

                        // If you are prefetching then you would do this at an appropriate time later...
                        displayInterstitial()
                    }
                })
            }
        }
    }

    /**
     * After the interstitial has been fetched, now display it:
     */
    private fun displayInterstitial() {
        activity?.let {
            if (mInterstitialAd != null) {
                Log.d("TAG", "Showing the interstitial")
                mInterstitialAd?.show(it)
            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet.")
            }
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