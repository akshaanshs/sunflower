package com.google.samples.apps.sunflower.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.samples.apps.sunflower.BuildConfig
import com.google.samples.apps.sunflower.data.GardenPlantingRepository
import com.google.samples.apps.sunflower.data.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    plantRepository: PlantRepository,
    private val gardenPlantingRepository: GardenPlantingRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val plantId: String = savedStateHandle.get<String>(PLANT_ID_SAVED_STATE_KEY)!!

    val isPlanted = gardenPlantingRepository.isPlanted(plantId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    val plant = plantRepository.getPlant(plantId).asLiveData()

    private val _showSnackbar = MutableLiveData(false)
    val showSnackbar: LiveData<Boolean>
        get() = _showSnackbar

    @SuppressLint("MissingPermission")
    fun addPlantToGarden() {
        viewModelScope.launch {
            try {
                gardenPlantingRepository.createGardenPlanting(plantId)
                _showSnackbar.value = true

                // Get real location
                var city = "unknown"
                var state = "unknown"
                var district = "unknown"
                var country = "unknown"

                try {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                    if (location != null) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses: List<Address> = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        ) ?: emptyList()

                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            city = address.locality ?: address.subAdminArea ?: "unknown"
                            state = address.adminArea ?: "unknown"
                            district = address.subAdminArea ?: address.locality ?: "unknown"
                            country = address.countryName ?: "unknown"
                        }
                    }
                } catch (locationException: Exception) {
                    // location unavailable, use unknown
                }

                Firebase.analytics.logEvent("form_submit") {
                    param("status", "success")
                    param("form_name", "add_plant_to_garden")
                    param("plant_id", plantId)
                    param("city", city)
                    param("state", state)
                    param("district", district)
                    param("country", country)
                }

            } catch (e: Exception) {
                Firebase.analytics.logEvent("form_submit") {
                    param("status", "failed")
                    param("form_name", "add_plant_to_garden")
                    param("plant_id", plantId)
                    param("error_reason", e.message ?: "unknown_error")
                }
            }
        }
    }

    fun dismissSnackbar() {
        _showSnackbar.value = false
    }

    fun hasValidUnsplashKey() = (BuildConfig.UNSPLASH_ACCESS_KEY != "null")

    companion object {
        private const val PLANT_ID_SAVED_STATE_KEY = "plantId"
    }
}