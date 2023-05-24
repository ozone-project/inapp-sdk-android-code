package com.ozoneproject.OzoneAndroidDemo

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.ads.MobileAds
import com.ozoneproject.OzoneAndroidDemo.R
import com.ozoneproject.OzoneAndroidDemo.databinding.ActivityMainBinding
import com.usercentrics.sdk.Usercentrics
import com.usercentrics.sdk.UsercentricsBanner
import com.usercentrics.sdk.UsercentricsOptions


private const val TAG = "MainActivity"


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val options = UsercentricsOptions(settingsId = "x1Y_PNLY58mmMO") // TCF
//        val options = UsercentricsOptions(settingsId = "WCxxFhip98YKKI") // GDPR
//        val options = UsercentricsOptions(settingsId = "SK7PP6qyLnU_P9") // CCPA

        Usercentrics.initialize(this, options)

        MobileAds.initialize(this) {}

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            collectConsent()
        }

        // any compatible cmp can be used.
        // see: https://docs.usercentrics.com/cmp_in_app_sdk/latest/integration/intro-collect/
        // further info: https://docs.usercentrics.com/cmp_in_app_sdk/latest/integration/usercentrics-ui/
        // applying consent: https://docs.usercentrics.com/cmp_in_app_sdk/latest/integration/intro-apply/

        // https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#how-do-third-party-sdks-vendors-access-the-consent-information-in-app
        // "On Android OS, the TC data and TC string shall be stored in the default Shared Preferences for the application context. This can be accessed using the getDefaultSharedPreferences method from the android.preference.PreferenceManager class using the application context."
        Usercentrics.isReady({ status ->
          if (status.shouldCollectConsent) {
            collectConsent()
          } else {
            // Apply consent with status.consents
            Log.d(TAG, "Apply consent with status.consents")
            Log.d(TAG, status.consents.toString())
          }
        }, { error ->
          Log.e(TAG, "Handle non-localized error")
          Log.e(TAG, error.toString())
        })

    }


    private fun collectConsent() {
        val banner = UsercentricsBanner(this)
        banner.showFirstLayer { userResponse ->
            Log.d(TAG, "cmp consent user response:")
            Log.d(TAG, userResponse.toString())
            // this should be automatically in default shared preferences
            // Usercentrics seems not to put gdprApplies in there tho (IABTCF_gdprApplies), just the consent string (IABTCF_TCString)
            Usercentrics.instance.getTCFData { tcfData ->
                val tcString = tcfData.tcString
                Log.d(TAG, "cmp consent string is $tcString")
          }
        }
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

}