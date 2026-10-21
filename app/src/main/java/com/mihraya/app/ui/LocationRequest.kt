package com.mihraya.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.mihraya.app.data.settings.SavedLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Stable
class LocationRequestState {
    var loading by mutableStateOf(false)
        internal set
    var error by mutableStateOf<String?>(null)
        internal set
    internal var trigger: () -> Unit = {}

    fun request() = trigger()

    fun clearError() {
        error = null
    }
}

/** Requests coarse location if needed, detects the position and saves it as the automatic location. */
@Composable
fun rememberLocationRequest(onSuccess: (SavedLocation) -> Unit = {}): LocationRequestState {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val state = remember { LocationRequestState() }
    val latestOnSuccess by rememberUpdatedState(onSuccess)

    fun detect() {
        state.loading = true
        state.error = null
        scope.launch {
            try {
                val location = container.location.detect()
                container.applyLocation(location, keepAutomatic = true).join()
                latestOnSuccess(location)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.error = e.message ?: "Couldn't determine your location"
            } finally {
                state.loading = false
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) {
            detect()
        } else {
            state.error = "Location permission was declined. You can search for your city instead."
        }
    }

    state.trigger = {
        if (container.location.hasPermission()) detect()
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    return state
}
