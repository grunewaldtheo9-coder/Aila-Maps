package com.example

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.data.AppDatabase
import com.example.data.SavedPlacesRepository
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RouteDetailsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SilkBackground
import com.example.ui.viewmodel.ActiveScreen
import com.example.ui.viewmodel.AilaMapViewModel
import com.example.ui.viewmodel.AilaViewModelFactory
import com.example.ui.viewmodel.TravelMode

import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var locationManager: LocationManager
    private lateinit var viewModel: AilaMapViewModel
    private var textToSpeech: android.speech.tts.TextToSpeech? = null

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null
    private var orientationSensor: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var lastBearingUpdateTime = 0L
    private var lastBearingValue = 0f

    private fun sendBearingUpdate(bearing: Float) {
        val now = System.currentTimeMillis()
        val diffDegrees = Math.abs(bearing - lastBearingValue)
        val shortestDiff = if (diffDegrees > 180f) 360f - diffDegrees else diffDegrees
        
        if (now - lastBearingUpdateTime > 150L && shortestDiff > 2.0f) {
            lastBearingUpdateTime = now
            lastBearingValue = bearing
            if (::viewModel.isInitialized) {
                viewModel.updateUserBearing(bearing)
            }
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                startLocationUpdates()
            }
        }
    }

    private var isGpsListenerActive = false
    private var isNetworkListenerActive = false

    private val isLocationUpdatesActive: Boolean
        get() = isGpsListenerActive || isNetworkListenerActive

    private fun stopLocationUpdates() {
        if (isGpsListenerActive) {
            try {
                locationManager.removeUpdates(gpsLocationListener)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            isGpsListenerActive = false
        }
        if (isNetworkListenerActive) {
            try {
                locationManager.removeUpdates(networkLocationListener)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            isNetworkListenerActive = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVectorSensor == null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        }
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = SavedPlacesRepository(database.savedPlaceDao())

        val sharedPrefs = getSharedPreferences("aila_prefs", Context.MODE_PRIVATE)
        val factory = AilaViewModelFactory(repository, sharedPrefs)
        viewModel = ViewModelProvider(this, factory).get(AilaMapViewModel::class.java)

        // Initialize TextToSpeech engine
        textToSpeech = android.speech.tts.TextToSpeech(applicationContext) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                textToSpeech?.language = java.util.Locale.getDefault()
            }
        }

        // Voice Route Guidance text-to-speech watcher (Enhanced for realistic voice feedback)
        lifecycleScope.launch {
            var lastStepIndex = -1
            var lastIsNavActive = false
            var hasSpokenArrival = false
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isVoiceGuidanceEnabled && state.isNavigationActive) {
                        val steps = state.navigationSteps
                        val currentIdx = state.currentNavigationStepIndex
                        
                        if (!lastIsNavActive) {
                            // Navigation just started! Let's do an initial welcome speech
                            val destinationName = state.activeDestination?.name ?: "your destination"
                            val modePhrase = when (state.travelMode) {
                                TravelMode.DRIVING -> "driving route to"
                                TravelMode.BICYCLING -> "cycling route to"
                                TravelMode.WALKING -> "walking route to"
                                else -> "route to"
                            }
                            val startupText = "Starting GPS navigation. Follow the highlighted $modePhrase $destinationName. Drive safely."
                            textToSpeech?.speak(startupText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "NavStartVoice")
                            lastIsNavActive = true
                            hasSpokenArrival = false
                            lastStepIndex = -1 // Reset to ensure first step runs
                        } else {
                            // Check if they reached the target destination
                            val isArrivalStep = currentIdx >= steps.size - 1 && state.navigationRemainingDistanceMeters < 15.0
                            if (isArrivalStep && !hasSpokenArrival) {
                                hasSpokenArrival = true
                                val arrivalText = "You have arrived at your destination. GPS guidance completed successfully. Thank you for choosing Aila Maps."
                                textToSpeech?.speak(arrivalText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "NavArrivalVoice")
                            } else if (!hasSpokenArrival) {
                                val step = steps.getOrNull(currentIdx)
                                if (step != null && currentIdx != lastStepIndex) {
                                    lastStepIndex = currentIdx
                                    textToSpeech?.speak(step.instruction, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "NavStepVoice")
                                }
                            }
                        }
                    } else {
                        lastIsNavActive = false
                        lastStepIndex = -1
                        hasSpokenArrival = false
                    }
                }
            }
        }

        // Dynamically manage tracking state based on user authentication lifecycle with state flow safety
        var wasAuthenticated: Boolean? = null
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val isAuthenticated = state.isAuthenticated
                    if (wasAuthenticated != isAuthenticated) {
                        wasAuthenticated = isAuthenticated
                        if (!isAuthenticated) {
                            stopLocationUpdates()
                        } else {
                            startLocationUpdates()
                        }
                    }
                }
            }
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SilkBackground
                ) { innerPadding ->
                    AilaAppContainer(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        registerSensors()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        unregisterSensors()
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }

    fun startLocationUpdatesPublic() {
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        // Bypasses hardware requests on virtual/emulator/container hosts to eliminate restricted MONITOR_LOCATION and GPS AppOps errors
        val isRunningInVirtualSandbox = try {
            android.os.Build.FINGERPRINT.contains("generic") ||
            android.os.Build.FINGERPRINT.contains("unknown") ||
            android.os.Build.FINGERPRINT.contains("cuttlefish") ||
            android.os.Build.FINGERPRINT.contains("cutf") ||
            android.os.Build.MODEL.contains("google_sdk") ||
            android.os.Build.MODEL.contains("Emulator") ||
            android.os.Build.MODEL.contains("Android SDK") ||
            android.os.Build.MODEL.contains("Cuttlefish") ||
            android.os.Build.MODEL.contains("cf_") ||
            android.os.Build.HARDWARE.contains("goldfish") ||
            android.os.Build.HARDWARE.contains("ranchu") ||
            android.os.Build.HARDWARE.contains("cutf") ||
            android.os.Build.HARDWARE.contains("gce") ||
            android.os.Build.PRODUCT.contains("sdk") ||
            android.os.Build.PRODUCT.contains("google_sdk") ||
            android.os.Build.PRODUCT.contains("cf_") ||
            android.os.Build.PRODUCT.contains("cuttlefish") ||
            android.os.Build.BOARD.contains("cutf") ||
            android.os.Build.BOARD.contains("goldfish") ||
            android.os.Build.BOARD.contains("ranchu") ||
            android.os.Build.MANUFACTURER.contains("Genymotion")
        } catch (t: Throwable) {
            false
        }
        if (isRunningInVirtualSandbox) {
            return
        }

        if (!::viewModel.isInitialized || !viewModel.uiState.value.isAuthenticated) {
            return
        }

        // Only start updates while the activity is in the foreground/started to prevent background AppOps errors
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }

        // Avoid repetitive re-registrations and duplicate AppOps monitoring sessions
        if (isLocationUpdatesActive) {
            return
        }

        val hasFine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return
        }

        // Avoid AppOps errors on virtualized services/restricted platforms by checking if location is globally enabled
        val isLocEnabled = try {
            androidx.core.location.LocationManagerCompat.isLocationEnabled(locationManager)
        } catch (t: Throwable) {
            false
        }
        if (!isLocEnabled) {
            return
        }

        try {
            // Unregister first to safely prevent duplicate registration listeners
            stopLocationUpdates()

            // Get last known location for immediate map panning safely
            var bestLocation: Location? = null
            val providers = try { locationManager.allProviders } catch (t: Throwable) { emptyList() }
            
            if (hasFine && providers.contains(LocationManager.GPS_PROVIDER)) {
                val lastKnownGps = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch(t: Throwable) { null }
                if (lastKnownGps != null) {
                    bestLocation = lastKnownGps
                }
            }
            
            if ((hasFine || hasCoarse) && providers.contains(LocationManager.NETWORK_PROVIDER)) {
                val lastKnownNet = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch(t: Throwable) { null }
                if (lastKnownNet != null && (bestLocation == null || lastKnownNet.accuracy < bestLocation.accuracy)) {
                    bestLocation = lastKnownNet
                }
            }
            
            bestLocation?.let {
                viewModel.updateUserLocation(it.latitude, it.longitude, shouldCenter = true)
            }

            // Listen for GPS and network location changes safely, matching granted permissions
            val isGpsEnabled = try {
                hasFine && providers.contains(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } catch (t: Throwable) {
                false
            }

            val isNetworkEnabled = try {
                (hasFine || hasCoarse) && providers.contains(LocationManager.NETWORK_PROVIDER) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (t: Throwable) {
                false
            }

            // Prioritize GPS_PROVIDER if enabled. Fall back to NETWORK_PROVIDER if GPS is not enabled/ready.
            // This prevents concurrent overlapping listener allocations which trigger AppOps MONITOR_LOCATION errors.
            if (isGpsEnabled) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000L,
                        5f,
                        gpsLocationListener,
                        Looper.getMainLooper()
                    )
                    isGpsListenerActive = true
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            } else if (isNetworkEnabled) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000L,
                        5f,
                        networkLocationListener,
                        Looper.getMainLooper()
                    )
                    isNetworkListenerActive = true
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private val gpsLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            @Suppress("SENSELESS_COMPARISON")
            if (location != null) {
                try {
                    if (::viewModel.isInitialized) {
                        viewModel.updateUserLocation(location.latitude, location.longitude, shouldCenter = false)
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    private val networkLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            @Suppress("SENSELESS_COMPARISON")
            if (location != null) {
                try {
                    if (::viewModel.isInitialized) {
                        viewModel.updateUserLocation(location.latitude, location.longitude, shouldCenter = false)
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor == null || event.values == null) return
            val values = event.values
            if (values.isEmpty()) return
            try {
                if (!::viewModel.isInitialized) return

                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        if (values.size >= 3) {
                            val rMatrix = FloatArray(9)
                            SensorManager.getRotationMatrixFromVector(rMatrix, values)
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(rMatrix, orientation)
                            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            val normalizedBearing = (azimuth + 360f) % 360f
                            sendBearingUpdate(normalizedBearing)
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val len = minOf(values.size, magnetometerReading.size)
                        System.arraycopy(values, 0, magnetometerReading, 0, len)
                        updateBearingFromMatrix()
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val len = minOf(values.size, accelerometerReading.size)
                        System.arraycopy(values, 0, accelerometerReading, 0, len)
                        updateBearingFromMatrix()
                    }
                    Sensor.TYPE_ORIENTATION -> {
                        if (values.isNotEmpty()) {
                            val azimuth = values[0]
                            sendBearingUpdate(azimuth)
                        }
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun updateBearingFromMatrix() {
        try {
            if (!::viewModel.isInitialized) return
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading
            )
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val normalizedBearing = (azimuth + 360f) % 360f
                sendBearingUpdate(normalizedBearing)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun registerSensors() {
        try {
            rotationVectorSensor?.let {
                sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
            } ?: run {
                accelerometerSensor?.let {
                    sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
                }
                magnetometerSensor?.let {
                    sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
                }
            }
            if (rotationVectorSensor == null && (accelerometerSensor == null || magnetometerSensor == null)) {
                orientationSensor?.let {
                    sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun unregisterSensors() {
        try {
            sensorManager.unregisterListener(sensorEventListener)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}

@Composable
fun AilaAppContainer(
    viewModel: AilaMapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SilkBackground)
    ) {
        when (uiState.currentScreen) {
            ActiveScreen.LOGIN -> {
                LoginScreen(viewModel = viewModel)
            }
            ActiveScreen.EXPLORE -> {
                MapScreen(viewModel = viewModel)
            }
            ActiveScreen.FAVORITES -> {
                FavoritesScreen(viewModel = viewModel)
            }
            ActiveScreen.ROUTE_DETAILS -> {
                RouteDetailsScreen(viewModel = viewModel)
            }
            ActiveScreen.PROFILE -> {
                ProfileScreen(viewModel = viewModel)
            }
        }
    }
}
