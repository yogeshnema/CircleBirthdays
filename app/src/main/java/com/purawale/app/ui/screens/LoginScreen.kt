package com.purawale.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.purawale.app.AppConfig
import com.purawale.app.Member
import com.purawale.app.hash
import com.purawale.app.t

@Composable
fun LoginScreen(
    members: List<Member>,
    error: String?,
    isHindi: Boolean,
    onLanguageToggle: (Boolean) -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val noEmailError = t("No email app found.", "कोई ईमेल ऐप नहीं मिला।")
    val enterPhoneError = t("Enter phone number", "फ़ोन नंबर दर्ज करें")
    val incorrectPasswordError = t("Incorrect password", "ग़लत पासवर्ड")

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Language Toggle at the top
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .background(Color(0xFF5D4037).copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "English",
                    color = if (!isHindi) Color(0xFF3E2723) else Color(0xFF3E2723).copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (!isHindi) FontWeight.ExtraBold else FontWeight.Medium
                )
                Switch(
                    checked = isHindi,
                    onCheckedChange = onLanguageToggle,
                    modifier = Modifier.scale(0.8f).padding(horizontal = 8.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF5D4037),
                        checkedTrackColor = Color(0xFFD7CCC8),
                        uncheckedThumbColor = Color(0xFF8D6E63),
                        uncheckedTrackColor = Color(0xFFEFEBE9)
                    )
                )
                Text(
                    "हिंदी",
                    color = if (isHindi) Color(0xFF3E2723) else Color(0xFF3E2723).copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isHindi) FontWeight.ExtraBold else FontWeight.Medium
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9).copy(alpha = 0.98f)),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(2.dp, Color(0xFF5D4037))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        t("Purawale\nHum aur Humare", "पुरावाले\nहम और हमारे"),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color(0xFF3E2723),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.all { char -> char.isDigit() }) phone = it; localError = null },
                        label = { Text(t("Phone Number", "फ़ोन नंबर"), fontWeight = FontWeight.Bold) },
                        placeholder = { Text(t("Enter your phone", "फ़ोन नंबर दर्ज करें")) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF3E2723),
                            unfocusedTextColor = Color(0xFF3E2723),
                            focusedBorderColor = Color(0xFF3E2723),
                            unfocusedBorderColor = Color(0xFF5D4037).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFF3E2723),
                            unfocusedLabelColor = Color(0xFF5D4037),
                            cursorColor = Color(0xFF3E2723),
                            focusedContainerColor = Color.White.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; localError = null },
                        label = { Text(t("Password", "पासवर्ड"), fontWeight = FontWeight.Bold) },
                        placeholder = { Text(t("Enter your password", "पासवर्ड दर्ज करें")) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF3E2723),
                            unfocusedTextColor = Color(0xFF3E2723),
                            focusedBorderColor = Color(0xFF3E2723),
                            unfocusedBorderColor = Color(0xFF5D4037).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFF3E2723),
                            unfocusedLabelColor = Color(0xFF5D4037),
                            cursorColor = Color(0xFF3E2723),
                            focusedContainerColor = Color.White.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    val context = LocalContext.current
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:".toUri()
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConfig.SUPPORT_EMAIL))
                                putExtra(Intent.EXTRA_SUBJECT, "Forgot Password - Nema Purawale App")
                                putExtra(Intent.EXTRA_TEXT, "Hello Admin,\n\nI forgot my password for the phone number: $phone.\n\nPlease help me reset it.\n\nThank you.")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                localError = noEmailError
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            t("Forgot Password?", "पासवर्ड भूल गए?"),
                            color = Color(0xFF5D4037),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = TextDecoration.Underline
                        )
                    }

                    val displayError = error ?: localError
                    if (displayError != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                        ) {
                            Text(
                                displayError,
                                color = Color(0xFFB71C1C),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (phone.isBlank()) {
                                localError = enterPhoneError
                            } else {
                                val user = members.find { it.phoneNumber == phone }
                                val storedHash = user?.password
                                val inputHash = password.hash()

                                val isValid = if (storedHash == null) {
                                    password == "1234"
                                } else {
                                    inputHash == storedHash
                                }

                                if (isValid) {
                                    onLoginSuccess(phone)
                                } else {
                                    localError = incorrectPasswordError
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3E2723),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            t("Login", "लॉगिन करें"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val uriHandler = LocalUriHandler.current
                    TextButton(
                        onClick = { uriHandler.openUri(AppConfig.PRIVACY_POLICY_URL) }
                    ) {
                        Text(
                            t("Privacy Policy", "गोपनीयता नीति"),
                            color = Color(0xFF5D4037),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}
