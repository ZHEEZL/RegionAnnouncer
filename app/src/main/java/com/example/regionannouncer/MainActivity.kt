
package com.example.regionannouncer

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var textToSpeech: TextToSpeech
    private var previousRegion: String? = null
    private var lastRegion: String? = null
    private var lastLocation: Location? = null
    private var hasAnnouncedBorder = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        textToSpeech = TextToSpeech(this, this)

        startLocationUpdates()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale("ru", "RU")
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                checkRegion(location.latitude, location.longitude)
            }
        }, Looper.getMainLooper())
    }

    private fun checkRegion(lat: Double, lon: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
        val region = address?.subAdminArea ?: address?.adminArea ?: "Неизвестный регион"

        val currentLocation = Location("").apply {
            latitude = lat
            longitude = lon
        }

        if (region != lastRegion) {
            if (lastRegion != null) {
                val message = "Вы въезжаете в $region"
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                previousRegion = lastRegion
            }
            lastRegion = region
            hasAnnouncedBorder = false
        } else {
            if (!hasAnnouncedBorder && previousRegion != null) {
                val lastLoc = lastLocation
                if (lastLoc != null) {
                    val distance = currentLocation.distanceTo(lastLoc)
                    if (distance > 2000) {
                        val message = "Вы приближаетесь к границе $previousRegion и $lastRegion"
                        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                        hasAnnouncedBorder = true
                    }
                }
            }
        }

        lastLocation = currentLocation
    }

    override fun onDestroy() {
        textToSpeech.shutdown()
        super.onDestroy()
    }
}
