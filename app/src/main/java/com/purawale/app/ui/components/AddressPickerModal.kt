package com.purawale.app.ui.components

import android.Manifest
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.purawale.app.t
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressPickerModal(
    initialAddress: String = "",
    onAddressSelected: (String, Double?, Double?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var searchQuery by remember { mutableStateOf("") }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(18.5204, 73.8567), 15f) // Default Pune
    }
    
    var currentAddress by remember { mutableStateOf(initialAddress) }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var isInitialLocationSet by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var lastResolvedLocation by remember { mutableStateOf<LatLng?>(null) }

    val performReverseGeocode: (LatLng, Boolean) -> Unit = { latLng, updateText ->
        if (updateText) isLoadingAddress = true
        scope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val onAddressResult: (List<android.location.Address>) -> Unit = { addresses ->
                    scope.launch {
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val fullAddress = address.getAddressLine(0) ?: ""
                            if (updateText) {
                                currentAddress = fullAddress
                            }
                            lastResolvedLocation = latLng
                        }
                        if (updateText) isLoadingAddress = false
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1, onAddressResult)
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    onAddressResult(addresses ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e("AddressPicker", "Geocoding failed", e)
                if (updateText) isLoadingAddress = false
            }
        }
    }

    val searchLocation: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            isLoadingAddress = true
            scope.launch {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val onResult: (List<android.location.Address>) -> Unit = { addresses ->
                        scope.launch {
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val latLng = LatLng(address.latitude, address.longitude)
                                val fullAddress = address.getAddressLine(0) ?: query
                                
                                currentAddress = fullAddress
                                lastResolvedLocation = latLng
                                selectedLocation = latLng
                                
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                            }
                            isLoadingAddress = false
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocationName(query, 1, onResult)
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(query, 1)
                        onResult(addresses ?: emptyList())
                    }
                } catch (e: Exception) {
                    Log.e("AddressPicker", "Search failed", e)
                    isLoadingAddress = false
                }
            }
        }
    }

    // Initialize map position based on initialAddress
    LaunchedEffect(initialAddress) {
        if (initialAddress.isNotBlank() && !isInitialLocationSet) {
            isLoadingAddress = true
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val onResult: (List<android.location.Address>) -> Unit = { addresses ->
                    scope.launch {
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val latLng = LatLng(address.latitude, address.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 17f)
                            selectedLocation = latLng
                            lastResolvedLocation = latLng
                            // Keep initialAddress as currentAddress until user moves map
                        }
                        isInitialLocationSet = true
                        isLoadingAddress = false
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(initialAddress, 1, onResult)
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(initialAddress, 1)
                    onResult(addresses ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e("AddressPicker", "Initial geocoding failed", e)
                isInitialLocationSet = true
                isLoadingAddress = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted && initialAddress.isBlank()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 17f)
                        selectedLocation = latLng
                        performReverseGeocode(latLng, true)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("AddressPicker", "Location access denied", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (initialAddress.isBlank()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            // Optional: Show loading while moving
            // isLoadingAddress = true
        } else {
            val center = cameraPositionState.position.target
            // Only reverse geocode if center changed significantly from last resolved location
            val shouldUpdate = lastResolvedLocation == null || 
                               Math.abs(lastResolvedLocation!!.latitude - center.latitude) > 0.0001 ||
                               Math.abs(lastResolvedLocation!!.longitude - center.longitude) > 0.0001
            
            if (shouldUpdate && isInitialLocationSet) {
                selectedLocation = center
                performReverseGeocode(center, true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.85f)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(t("Search location...", "स्थान खोजें...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { searchLocation(searchQuery) }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5D4037),
                    unfocusedBorderColor = Color(0xFFD7CCC8)
                )
            )

            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = hasLocationPermission,
                        zoomControlsEnabled = false
                    ),
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
                )
                
                // Fixed marker in center
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(40.dp).offset(y = (-20).dp),
                    tint = Color.Red
                )
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        t("Selected Location", "चुना गया स्थान"), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color(0xFF5D4037)
                    )
                    Spacer(Modifier.height(4.dp))
                    
                    if (isLoadingAddress) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF5D4037))
                    } else {
                        Text(
                            text = if (currentAddress.isEmpty()) t("Locating...", "खोज रहे हैं...") else currentAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF3E2723),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onAddressSelected(currentAddress, selectedLocation?.latitude, selectedLocation?.longitude) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = currentAddress.isNotEmpty() && !isLoadingAddress
                    ) {
                        Text(t("Confirm Location", "स्थान की पुष्टि करें"), color = Color.White)
                    }
                }
            }
        }
    }
}
