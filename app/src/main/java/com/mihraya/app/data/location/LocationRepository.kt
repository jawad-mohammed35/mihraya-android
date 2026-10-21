package com.mihraya.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.mihraya.app.data.settings.SavedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.DateTimeException
import java.time.ZoneId
import java.util.Locale
import kotlin.coroutines.resume

class LocationException(message: String) : Exception(message)

data class PlaceResult(
    val name: String,
    val region: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
) {
    fun toSaved(): SavedLocation = SavedLocation(
        latitude = latitude,
        longitude = longitude,
        name = name,
        region = listOf(region, country).filter { it.isNotBlank() && it != name }.distinct().joinToString(", "),
        countryCode = countryCode,
        timeZone = timeZone,
        automatic = false,
        updatedAt = System.currentTimeMillis(),
    )
}

class LocationRepository(
    private val context: Context,
    private val http: OkHttpClient,
    private val json: Json,
) {
    private val manager = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean = manager != null && LocationManagerCompat.isLocationEnabled(manager)

    @SuppressLint("MissingPermission")
    suspend fun detect(): SavedLocation {
        if (!hasPermission()) throw LocationException("Location permission is needed")
        val lm = manager ?: throw LocationException("Location is not available on this device")
        if (!LocationManagerCompat.isLocationEnabled(lm)) throw LocationException("Turn on location services and try again")

        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }.filter { provider -> runCatching { lm.isProviderEnabled(provider) }.getOrDefault(false) }

        val now = System.currentTimeMillis()
        val recent = providers
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { now - it.time < RECENT_FIX_MS }
            .maxByOrNull { it.time }

        val location = recent ?: providers
            .filter { it != LocationManager.PASSIVE_PROVIDER }
            .firstNotNullOfOrNull { provider -> currentLocation(lm, provider) }
            ?: providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
            ?: throw LocationException("Couldn't determine your location. Try again or search for your city.")

        val address = reverseGeocode(location.latitude, location.longitude)
        val name = address?.locality ?: address?.subAdminArea ?: address?.adminArea
            ?: String.format(Locale.US, "%.3f, %.3f", location.latitude, location.longitude)
        return SavedLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            name = name,
            region = listOfNotNull(address?.adminArea, address?.countryName)
                .filter { it.isNotBlank() && it != name }
                .distinct()
                .joinToString(", "),
            countryCode = address?.countryCode.orEmpty(),
            timeZone = ZoneId.systemDefault().id,
            automatic = true,
            updatedAt = now,
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(lm: LocationManager, provider: String): Location? =
        withTimeoutOrNull(FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                try {
                    LocationManagerCompat.getCurrentLocation(
                        lm,
                        provider,
                        signal,
                        ContextCompat.getMainExecutor(context),
                    ) { location -> if (continuation.isActive) continuation.resume(location) }
                } catch (e: RuntimeException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(GEOCODE_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        },
                    )
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()
            }
        }
    }

    suspend fun search(query: String): List<PlaceResult> = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query.trim())
            .addQueryParameter("count", "15")
            .addQueryParameter("language", "en")
            .addQueryParameter("format", "json")
            .build()
        val body = http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Search failed (${response.code})")
            response.body?.string().orEmpty()
        }
        json.decodeFromString(GeoResponse.serializer(), body).results.map { r ->
            PlaceResult(
                name = r.name,
                region = r.admin1.orEmpty(),
                country = r.country.orEmpty(),
                countryCode = r.countryCode.orEmpty(),
                latitude = r.latitude,
                longitude = r.longitude,
                timeZone = r.timezone?.takeIf(::isValidZone) ?: ZoneId.systemDefault().id,
            )
        }
    }

    private fun isValidZone(id: String): Boolean = try {
        ZoneId.of(id)
        true
    } catch (e: DateTimeException) {
        false
    }

    @Serializable
    private data class GeoResponse(val results: List<GeoResult> = emptyList())

    @Serializable
    private data class GeoResult(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val country: String? = null,
        @SerialName("country_code") val countryCode: String? = null,
        val admin1: String? = null,
        val timezone: String? = null,
    )

    private companion object {
        const val RECENT_FIX_MS = 30 * 60 * 1000L
        const val FIX_TIMEOUT_MS = 15_000L
        const val GEOCODE_TIMEOUT_MS = 8_000L
    }
}
