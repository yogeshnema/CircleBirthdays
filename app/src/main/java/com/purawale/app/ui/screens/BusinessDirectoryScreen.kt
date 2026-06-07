package com.purawale.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.purawale.app.Business
import com.purawale.app.Member
import com.purawale.app.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDirectoryScreen(
    user: Member,
    businesses: List<Business>,
    onBack: () -> Unit,
    onAddBusiness: (Business) -> Unit,
    onDeleteBusiness: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var businessToDelete by remember { mutableStateOf<Business?>(null) }

    if (businessToDelete != null) {
        AlertDialog(
            onDismissRequest = { businessToDelete = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color(0xFF1A1C1E),
            title = { Text(t("Confirm Delete", "हटाने की पुष्टि करें"), color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(t("Are you sure you want to delete this business listing?", "क्या आप वाकई इस व्यवसाय लिस्टिंग को हटाना चाहते हैं?"), color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        businessToDelete?.let { onDeleteBusiness(it.id) }
                        businessToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) { Text(t("Delete", "हटाएं"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { businessToDelete = null }) {
                    Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("Business Directory", "व्यवसाय निर्देशिका")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Business")
                    }
                }
            )
        }
    ) { padding ->
        if (businesses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(t("No businesses found", "कोई व्यवसाय नहीं मिला"))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(businesses) { business ->
                    BusinessCard(business, user.isAdmin || business.addedBy == user.id) {
                        businessToDelete = business
                    }
                }
            }
        }

        if (showAddDialog) {
            AddBusinessDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { business ->
                    onAddBusiness(business.copy(addedBy = user.id))
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun BusinessCard(business: Business, canDelete: Boolean, onDelete: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(business.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(business.type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("${t("Owner", "मालिक")}: ${business.ownerName}", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(business.contactNumber, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, "tel:${business.contactNumber}".toUri())
                        context.startActivity(intent)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(t("Call", "कॉल करें"), fontSize = 12.sp)
                }
            }

            if (business.address.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(business.address, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (business.locationLink.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, business.locationLink.toUri())
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Map, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t("View on Map", "मैप पर देखें"))
                }
            }
        }
    }
}

@Composable
fun AddBusinessDialog(onDismiss: () -> Unit, onConfirm: (Business) -> Unit) {
    var name by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Business") }
    var address by remember { mutableStateOf("") }
    var locationLink by remember { mutableStateOf("") }

    val businessTypes = listOf(
        t("Business", "व्यवसाय"),
        t("Consultancy", "परामर्श"),
        t("Shop", "दुकान"),
        t("Event Hall", "इवेंट हॉल"),
        t("Public Place", "सार्वजनिक स्थान"),
        t("Other", "अन्य")
    )
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFF1A1C1E),
        title = { Text(t("Add Business", "व्यवसाय जोड़ें"), color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFC857),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFFFFC857),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = Color(0xFFFFC857)
                )

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(t("Business Name", "व्यवसाय का नाम")) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text(t("Owner Name", "मालिक का नाम")) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                OutlinedTextField(value = contactNumber, onValueChange = { contactNumber = it }, label = { Text(t("Contact Number", "संपर्क नंबर")) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                
                Box {
                    OutlinedTextField(
                        value = type,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(t("Type", "प्रकार")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFFFFC857))
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expanded, 
                        onDismissRequest = { expanded = false },
                        containerColor = Color(0xFF2A2D31)
                    ) {
                        businessTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t, color = Color.White) }, 
                                onClick = { type = t; expanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(t("Address", "पता")) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                OutlinedTextField(value = locationLink, onValueChange = { locationLink = it }, label = { Text(t("Maps Link", "मैप्स लिंक")) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && ownerName.isNotBlank() && contactNumber.isNotBlank()) {
                        onConfirm(Business(name = name, ownerName = ownerName, contactNumber = contactNumber, type = type, address = address, locationLink = locationLink))
                    }
                },
                enabled = name.isNotBlank() && ownerName.isNotBlank() && contactNumber.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color.Black),
                shape = RoundedCornerShape(24.dp)
            ) { Text(t("Add", "जोड़ें"), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.6f)) }
        }
    )
}
