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
import org.prebid.mobile.AdSize
import org.prebid.mobile.InStreamVideoAdUnit
import org.prebid.mobile.ResultCode
import org.prebid.mobile.Util
import org.prebid.mobile.VideoParameters


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
    private var playerView: PlayerView? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // NOTE to request an instream video from prebid
    companion object {
        const val CONFIG_ID = "8000000328"
        const val WIDTH = 300
        const val HEIGHT = 179
    }
    private var adUnit: InStreamVideoAdUnit? = null
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
        binding.playButton.setOnClickListener {
            createAd()
        }

    }

    // code snippets from prebid example https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html
    private fun createAd() {

        Log.d(TAG, "*** createAd")
        // check whether the user allows geo location
        getLocation()

        // https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html#instream-video-api
        // 1. Create VideoAdUnit
        adUnit = InStreamVideoAdUnit(CONFIG_ID, WIDTH, HEIGHT)

        // 2. configure video parameters:
        adUnit?.videoParameters = configureVideoParameters()

        // 3. init player view
        playerView = PlayerView(requireContext())
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600)
        binding.viewContainer.addView(playerView, params)

        // 4. make a bid request to prebid server
        adUnit?.fetchDemand { _: ResultCode?, keysMap: Map<String?, String?>? ->

            // Prepare the creative URI
            val sizes = HashSet<AdSize>()
            sizes.add(AdSize(WIDTH, HEIGHT))
            val prebidURL = Util.generateInstreamUriForGam(
 // todo - finish off from https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html#instream-video-api
            )
        }


        // this seems all entirely wrong - we need to have an exoplayer, and use the google toolkit to handle ads (and call prebid first)


        // make changes for Ozone
        Log.d(TAG, "Setting Ozone vars")



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


        // what to do here - need to show an exoplayer, playing a video, and first call prebid then gpt...

        Log.d(TAG, "need to show an exoplayer, call prebid, call google & do the biz...")
    }

    // https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html#instream-video-api
    private fun configureVideoParameters(): VideoParameters {
        return VideoParameters(listOf("video/x-flv", "video/mp4")).apply {
            placement = Signals.Placement.InStream

            api = listOf(
                Signals.Api.VPAID_1,
                Signals.Api.VPAID_2
            )

            maxBitrate = 1500
            minBitrate = 300
            maxDuration = 30
            minDuration = 5
            playbackMethod = listOf(Signals.PlaybackMethod.AutoPlaySoundOn)
            protocols = listOf(
                Signals.Protocols.VAST_2_0
            )
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