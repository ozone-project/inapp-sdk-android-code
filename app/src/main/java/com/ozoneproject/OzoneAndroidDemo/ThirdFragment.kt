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
import com.google.ads.interactivemedia.v3.api.AdsLoader
import com.google.ads.interactivemedia.v3.api.AdsManager
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.ozoneproject.OzoneAndroidDemo.databinding.FragmentThirdBinding
import org.prebid.mobile.BannerAdUnit
import org.prebid.mobile.BannerParameters
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.Signals
import org.prebid.mobile.TargetingParams
import org.prebid.mobile.api.data.InitializationStatus


import android.widget.MediaController;
import android.widget.VideoView;


import com.google.android.exoplayer2.ui.PlayerView


// assembled from https://developers.google.com/interactive-media-ads/docs/sdks/android/client-side


private const val TAG = "ThirdFragment"
/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ThirdFragment : Fragment(), LocationListener {

    private var _binding: FragmentThirdBinding? = null
    private var SAMPLE_VIDEO_URL = "https://storage.googleapis.com/gvabox/media/samples/stock.mp4"

    /**
     * IMA sample tag for a single skippable inline video ad. See more IMA sample tags at
     * https://developers.google.com/interactive-media-ads/docs/sdks/html5/client-side/tags
     */
    private val SAMPLE_VAST_TAG_URL =
        ("https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/"
                + "single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast"
                + "&unviewed_position_start=1&env=vp&impl=s&correlator=")

    // Factory class for creating SDK objects.
    private val sdkFactory: ImaSdkFactory? = null

    // The AdsLoader instance exposes the requestAds method.
    private val adsLoader: AdsLoader? = null

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private val adsManager: AdsManager? = null

    // The saved content position, used to resumed content following an ad break.
    private val savedPosition = 0

    // This sample uses a VideoView for content and ad playback. For production
    // apps, Android's Exoplayer offers a more fully featured player compared to
    // the VideoView.
    private val videoPlayer: VideoView? = null
    private val mediaController: MediaController? = null
    private val playButton: View? = null
    private val videoAdPlayerAdapter: VideoAdPlayerAdapter? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // NOTE to request an instream video from prebid
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

        // now add the elements from point 9 in https://developers.google.com/interactive-media-ads/docs/sdks/android/client-side


    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "*** Pausing frag 3")
        adUnit?.stopAutoRefresh()
    }

    /**
     * this is called after onViewCreated
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "*** resuming frag 3")
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

        _binding = FragmentThirdBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "*** onViewCreated")

        binding.buttonThird.setOnClickListener {
            findNavController().navigate(R.id.action_thirdFragment_to_FirstFragment)
        }

        PrebidMobile.initializeSdk(activity?.applicationContext) { status ->
            if (status == InitializationStatus.SUCCEEDED) {
                Log.d(TAG, "SDK initialized successfully!")
            } else if (status == InitializationStatus.SERVER_STATUS_WARNING) {
                Log.d(TAG, "Prebid server status check failed: $status\n${status.description}")
            } else {
                Log.e(TAG, "SDL initialization error : $status\n${status.description}")
            }
        }

    }

    // code snippets from prebid example https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html
    private fun createAd() {

        // check whether the user allows geo location
        getLocation()

        playerView = PlayerVie(requireContext())

        // 1. Create BannerAdUnit
        adUnit = BannerAdUnit(CONFIG_ID, WIDTH, HEIGHT)
        adUnit?.setAutoRefreshInterval(30) // IF you want to auto refresh the ad

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


        // Prebid docs: https://docs.prebid.org/prebid-mobile/prebid-mobile-privacy-regulation.html
        // these 2 should be set automatically in default shared preferences but Usercentrics seems not to set SubjectToGDPR
//        TargetingParams.setSubjectToGDPR(true)  // see First Fragment
//    TargetingParams.setGDPRConsentString("...") // see First Fragment
//        TargetingParams.setPublisherName() // do not set this - it's not useful
        TargetingParams.setOmidPartnerName("Google1")
        TargetingParams.setOmidPartnerVersion("3.16.3")
        PrebidMobile.setShareGeoLocation(getLocationTrackingOK())
        if(getLocationTrackingOK()) {
            TargetingParams.setUserLatLng(
                lastLocation?.latitude?.toFloat(),
                lastLocation?.longitude?.toFloat()
            )
        }

        // 4. Make a bid request to Prebid Server
        val request = AdManagerAdRequest.Builder().build()
        adUnit?.fetchDemand(request) {
            // inside the callback we will call for an ad. Prebid will have set the targeting keys
            Log.d(
                TAG,
                "fetchDemand callback. request targeting is: " + request.customTargeting.toString()
            )
            // both of these have to be set the same way - either in xml or in code
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
            )
        )
        lastLocation = location
    }

}