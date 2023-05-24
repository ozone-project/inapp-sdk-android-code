
# Developer notes for integrating Ozone's prebid server auction endpoint with an existing Android application which currently shows ads using DFP adserver.

This code will build & run, and demonstrates how to use Ozone prebid header bidding with DFP adserver. It contains libraries you can use in your own project.

## Overview : Recapping your current setup:

An Android app will generally do something like this to request an ad (Kotlin):

```
(binding is set to something like: FragmentFirstBinding.inflate(inflater, container, false) which contains an com.google.android.gms.ads.AdView element with id=adView)
// ... 
val request = AdManagerAdRequest.Builder().build()
binding.adView.loadAd(request)
```

which calls the adserver, and ends with an ad being rendered in the adView element.


## Header bidding works like this: 

```
var adUnit = BannerAdUnit(CONFIG_ID, WIDTH, HEIGHT)
// ... set targeting, for bidders to examine ... (omitted)
val request = AdManagerAdRequest.Builder().build()
adUnit?.fetchDemand(request) {
	// inside the callback we will call for an ad. Prebid will have set targeting keys on the request object, ready to send to the adserver.
        Log.d(TAG, "fetchDemand callback. request targeting is: " + request.customTargeting.toString())
        binding.adView.loadAd(request)
}
```

See this example Android app for exactly how to set the targeting required.

This time we first make a call to the Ozone prebid auction, wait for the response, and then finally make the call to the adserver which results in an ad being rendered in the adView element. Note that additional 
parameters will be sent to the adserver which allow prebid ads to compete with other adserver content (if any).


## Dev Requirements for adding Ozone prebid capability

You will first need to add the Ozone prebid libraries to your Android project. 

1. Download this demo app from https://github.com/ozone-project/inapp-sdk-android-code. This contains the Ozone prebid library files and the code demonstrates how to use them.
2. Copy all the jar files from the demo app /libs directory into a directory in your app called /libs or whatever you want to call it
3. Hilight all the files you just imported, right-click them and select 'Add as Library'

All the Ozone prebid code will now be available to your app, and the code in FirstFragment.kt and SecondFragment.kt will all work in your application.

Take a look at the code in FirstFragment.kt and SecondFragment.kt to see the app initialising, configuring, requesting bids, then finally making the call to the adserver.


## CMP - Customer Management Platform for requesting GDPR/USP consent 

The app shows how to use the UserCentrics CMP to request and manage user consent for data capture & use for header bidding. It is essential that you comply with data regulations for each user as advised by your 
legal advisors. The CMP can be configured to request consent for users subject to GDPR or USP. You can use any compatible CMP, note that at time of writing you should be using a TCFAPI v2 compliant CMP.

For more information about CMPs and TCF API see the IAB docs at https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md

The way we import UserCentrics into this app is by adding a line into the app/build.gradle file: 

```
    implementation "com.usercentrics.sdk:usercentrics-ui:2.8.0"
```

And you can see the code used to decide whether to pop up the CMP, and to collect the users response in MainActivity.kt

For more information see the Kotlin examples at : https://docs.usercentrics.com/cmp_in_app_sdk/latest/integration/intro-collect/


## Location tracking

If you already use location tracking in your app then please review whether the consent you get from the user covers the use of location for targeted ads. We have built a location consent popup into the app which 
will gather the user's response when the app first starts and the OS will persist that in the location for the device/OS version something like: settings->apps->Ozone Prebid Integration 
Example->Permissions->Location. 

Note that in order to use location information you will need to add user-permission elements into AndroidManifest.xml like this

```
    <!-- Always include this permission -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <!-- Include only if your app benefits from precise location access. -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

In the app (FirstFragment.kt) we use an ActivityResult to get the user's permission: 

```
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
```

and then
```
   requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
```
to show the dialog.

This code sets user.geo.lat & user.geo.lon in the request to the Ozone auction:

```
        PrebidMobile.setShareGeoLocation(getLocationTrackingOK())
        if(getLocationTrackingOK()) {
            Log.d(TAG, "setting location info in the auction call")
            TargetingParams.setUserLatLng(lastLocation?.latitude?.toFloat(), lastLocation?.longitude?.toFloat())
        } else {
            Log.d(TAG, "NOT going to set location info in the auction call")
        }
```




