package com.purawale.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FireTruck
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.purawale.app.Member
import com.purawale.app.t

private data class EmergencyNumber(
    val title: String,
    val subtitle: String,
    val number: String,
    val icon: ImageVector,
    val color: Color
)

private data class NearbyService(
    val title: String,
    val query: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    currentUser: Member,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLocation by remember {
        mutableStateOf(
            currentUser.latitude?.let { lat ->
                currentUser.longitude?.let { lng -> LatLng(lat, lng) }
            }
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun openDial(number: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    fun openMapSearch(query: String) {
        val location = currentLocation
        val encodedQuery = Uri.encode(query)
        val uri = if (location != null) {
            Uri.parse("geo:${location.latitude},${location.longitude}?q=$encodedQuery")
        } else {
            Uri.parse("geo:0,0?q=$encodedQuery")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        runCatching { context.startActivity(intent) }
            .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    @SuppressLint("MissingPermission")
    fun refreshLocation() {
        if (!hasLocationPermission) return
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = LatLng(location.latitude, location.longitude)
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) refreshLocation()
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) refreshLocation()
    }

    val emergencyNumbers = listOf(
        EmergencyNumber(t("National Emergency", "राष्ट्रीय आपातकाल"), t("Police, fire, medical", "पुलिस, आग, चिकित्सा"), "112", Icons.Default.Call, Color(0xFFE53935)),
        EmergencyNumber(t("Ambulance", "एम्बुलेंस"), t("Medical emergency", "चिकित्सा आपातकाल"), "108", Icons.Default.MedicalServices, Color(0xFFD81B60)),
        EmergencyNumber(t("Police", "पुलिस"), t("Immediate police help", "तुरंत पुलिस सहायता"), "100", Icons.Default.LocalPolice, Color(0xFF3949AB)),
        EmergencyNumber(t("Fire", "दमकल"), t("Fire emergency", "आग आपातकाल"), "101", Icons.Default.FireTruck, Color(0xFFF4511E)),
        EmergencyNumber(t("Women Helpline", "महिला हेल्पलाइन"), t("Emergency support", "आपात सहायता"), "1091", Icons.Default.Call, Color(0xFF8E24AA)),
        EmergencyNumber(t("Railway Helpline", "रेलवे हेल्पलाइन"), t("Railway enquiry/help", "रेलवे पूछताछ/सहायता"), "139", Icons.Default.Train, Color(0xFF00897B))
    )

    val nearbyServices = listOf(
        NearbyService(t("Nearby Hospitals", "नजदीकी अस्पताल"), "hospital near me", Icons.Default.LocalHospital, Color(0xFFD81B60)),
        NearbyService(t("Nearby Ambulance", "नजदीकी एम्बुलेंस"), "ambulance service near me", Icons.Default.MedicalServices, Color(0xFFE53935)),
        NearbyService(t("Nearby Fire Station", "नजदीकी दमकल स्टेशन"), "fire station near me", Icons.Default.FireTruck, Color(0xFFF4511E)),
        NearbyService(t("Nearby Police Station", "नजदीकी पुलिस स्टेशन"), "police station near me", Icons.Default.LocalPolice, Color(0xFF3949AB)),
        NearbyService(t("Nearby Railway Station", "नजदीकी रेलवे स्टेशन"), "railway station near me", Icons.Default.Train, Color(0xFF00897B))
    )

    Scaffold(
        containerColor = Color(0xFF080B14),
        topBar = {
            TopAppBar(
                title = { Text(t("Emergency Help", "आपातकालीन सहायता"), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (hasLocationPermission) {
                            refreshLocation()
                        } else {
                            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh location", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF101522))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LocationStatusCard(
                    hasLocationPermission = hasLocationPermission,
                    hasLocation = currentLocation != null,
                    onEnableLocation = {
                        locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    onRefreshLocation = { refreshLocation() }
                )
            }

            item {
                SectionTitle(t("Call Now", "अभी कॉल करें"))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    emergencyNumbers.forEach { item ->
                        EmergencyNumberCard(item = item, onCall = { openDial(item.number) })
                    }
                }
            }

            item {
                SectionTitle(t("Find Nearby", "नजदीक खोजें"))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    nearbyServices.forEach { service ->
                        NearbyServiceCard(service = service, onOpenMap = { openMapSearch(service.query) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationStatusCard(
    hasLocationPermission: Boolean,
    hasLocation: Boolean,
    onEnableLocation: () -> Unit,
    onRefreshLocation: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (hasLocation) Icons.Default.LocationOn else Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = if (hasLocation) Color(0xFFFFC857) else Color.White.copy(alpha = 0.65f)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (hasLocation) t("Location ready", "लोकेशन तैयार") else t("Location needed for nearby search", "नजदीक खोजने के लिए लोकेशन चाहिए"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (hasLocationPermission) {
                        t("Nearby searches will open in Maps.", "नजदीकी खोज मैप्स में खुलेगी।")
                    } else {
                        t("Enable location to search around you.", "अपने आसपास खोजने के लिए लोकेशन चालू करें।")
                    },
                    color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(
                onClick = if (hasLocationPermission) onRefreshLocation else onEnableLocation,
                border = BorderStroke(1.dp, Color(0xFFFFC857))
            ) {
                Text(if (hasLocationPermission) t("Refresh", "रीफ्रेश") else t("Enable", "चालू करें"), color = Color(0xFFFFC857))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun EmergencyNumberCard(item: EmergencyNumber, onCall: () -> Unit) {
    EmergencyActionCard(
        icon = item.icon,
        iconColor = item.color,
        title = item.title,
        subtitle = "${item.subtitle} • ${item.number}",
        actionLabel = t("Call", "कॉल"),
        actionIcon = Icons.Default.Call,
        onAction = onCall
    )
}

@Composable
private fun NearbyServiceCard(service: NearbyService, onOpenMap: () -> Unit) {
    EmergencyActionCard(
        icon = service.icon,
        iconColor = service.color,
        title = service.title,
        subtitle = t("Open nearby results in Maps", "मैप्स में नजदीकी परिणाम खोलें"),
        actionLabel = t("Map", "मैप"),
        actionIcon = Icons.Default.Map,
        onAction = onOpenMap
    )
}

@Composable
private fun EmergencyActionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    actionLabel: String,
    actionIcon: ImageVector,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.09f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color(0xFF101522)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(actionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
