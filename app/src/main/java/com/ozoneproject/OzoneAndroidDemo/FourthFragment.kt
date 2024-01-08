package com.ozoneproject.OzoneAndroidDemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.ads.interactivemedia.v3.api.AdsManager
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.ads.AdsMediaSource
import androidx.media3.ui.PlayerView
import com.ozoneproject.OzoneAndroidDemo.databinding.FragmentFourthBinding
import org.json.JSONObject
import org.prebid.mobile.AdSize
import org.prebid.mobile.ExternalUserId
import org.prebid.mobile.InStreamVideoAdUnit
import org.prebid.mobile.ResultCode
import org.prebid.mobile.Signals
import org.prebid.mobile.TargetingParams
import org.prebid.mobile.Util
import org.prebid.mobile.VideoParameters

private const val TAG = "FourthFragment"
/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class FourthFragment : Fragment(), LocationListener {

    private var _binding: FragmentFourthBinding? = null
    private var SAMPLE_VIDEO_URL = "https://storage.googleapis.com/gvabox/media/samples/stock.mp4"


    // The AdsLoader instance exposes the requestAds method.
    private var adsLoader: ImaAdsLoader? = null

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private var adsManager: AdsManager? = null

    // The saved content position, used to resumed content following an ad break.
    private var savedPosition = 0

    // For production apps, Android's Exoplayer offers a more fully featured player compared to VideoView.
    private var playerView: PlayerView? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // NOTE to request an instream video from prebid
    companion object {
        const val CONFIG_ID = "8000000328" // use this for instream 20230802
//        const val CONFIG_ID = "7771070002"
        const val WIDTH = 640
        const val HEIGHT = 480
        const val AD_UNIT_ID = "/22037345/ozone-instream-test"
    }
    private var adUnit: InStreamVideoAdUnit? = null
    var lastLocation: Location? = null
    var locationPermissionGranted: Boolean = false
    var adsUri: Uri? =null
    var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "*** Pausing frag 3")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.e(TAG, " *** LOW MEMORY DETECTED *** ");
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

        _binding = FragmentFourthBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "*** onViewCreated")

        binding.buttonThird.setOnClickListener {
            findNavController().navigate(R.id.action_FourthFragment_to_SecondFragmentB)
        }
        binding.playButtonFrag4.setOnClickListener {
            createAd()
        }

    }

    // code snippets from prebid example https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html
    private fun createAd() {

        Log.d(TAG, "*** createAd")
        // check whether the user allows geo location
        getLocation()

        // 1. Create VideoAdUnit
        adUnit = InStreamVideoAdUnit(CONFIG_ID, WIDTH, HEIGHT)

        // 2. configure video parameters:
        var videoParams: VideoParameters = configureVideoParameters()
        // now add the ozone-specific params
        val jsonExt = JSONObject(
            """{
            "context": "instream",
            "playerSize": [[640,480]],
            "format": [{"w": 640, "h":480}]
            }""".trimMargin()
        );
        videoParams.ozoneSetExt(jsonExt)
        adUnit?.videoParameters = videoParams
        adUnit?.ozoneSetImpAdUnitCode("video-ad") // this may not be needed in app context
        val jsonObj = JSONObject(
            """{
            "section": "sport",
            "pos":"video-ad",
            "keywords": [
                "boxing", "tyson fury", "anthony joshua", "eddie hearn"
            ],
            "oztestmode": "ios_test"
            }
        }""".trimIndent()
        )

        adUnit?.ozoneSetCustomDataTargeting(jsonObj)
//        TargetingParams.setPlacementId("8000000328") // 20230124 - do not do this now; we use the placementId from the adunit configId

        TargetingParams.setAppPageName("https://www.ardm.io/other_page")
        TargetingParams.setSubjectToCOPPA(false) // false by default


        // 3. init player view
        playerView = PlayerView(requireContext())
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600)
        binding.viewContainer.addView(playerView, params)

        // 4. make a bid request to prebid server
        adUnit?.fetchDemand { _: ResultCode?, keysMap: Map<String?, String?>? ->

            Log.d(TAG, "fetchDemand got keys: " + keysMap.toString())
            binding.outputInfo.text = "fetchDemand got keys: " + keysMap.toString()
            // 5. Prepare the creative URI
            val sizes = HashSet<AdSize>()
            sizes.add(AdSize(WIDTH, HEIGHT))
            sizes.add(AdSize(400, 300))
            val prebidURL = Util.generateInstreamUriForGam(
                AD_UNIT_ID, sizes, keysMap
            )
            adsUri = Uri.parse(prebidURL)

            // 6. Init the player
            initializePlayer()
        }

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


    private fun initializePlayer() {

        adsLoader = ImaAdsLoader.Builder(requireContext()).build()

        val playerBuilder = ExoPlayer.Builder(requireContext())
        player = playerBuilder.build()
        playerView!!.player = player
        adsLoader!!.setPlayer(player)

        val uri = Uri.parse(SAMPLE_VIDEO_URL)

        /*
        https://developer.android.com/guide/topics/media/media3/getting-started/migration-guide#exoplayer
        After migrating from ExoPlayer v2 to Media3, you may see a lot of unstable API lint errors. This may make it seem like Media3 is 'less stable' than ExoPlayer v2. This is not the case. The 'unstable' parts of the Media3 API have the same level of stability as the whole of the ExoPlayer v2 API surface, and the guarantees of the stable Media3 API surface are not available in ExoPlayer v2 at all. The difference is simply that a lint error now alerts you to the different levels of stability.
         */

        val mediaItem = MediaItem.fromUri(uri)
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(requireContext(), getString(R.string.app_name))
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
        val mediaSource: MediaSource = mediaSourceFactory.createMediaSource(mediaItem)
        val dataSpec = DataSpec(adsUri!!)
        val adsMediaSource = AdsMediaSource(
            mediaSource, dataSpec, "ad", mediaSourceFactory,
            adsLoader!!, playerView!!
        )
        player?.setMediaSource(adsMediaSource)
        player?.playWhenReady = true
        player?.prepare()
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