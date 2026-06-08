package com.purawale.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purawale.app.t
import com.purawale.app.ui.components.AppTopBar
import com.purawale.app.ui.components.ScreenContainer

private data class HelpFaq(
    val question: String,
    val answer: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    val faqs = listOf(
        HelpFaq(
            question = t("What is the dashboard?", "डैशबोर्ड क्या है?"),
            answer = t(
                "The dashboard is your starting point. It shows your profile, notifications, today's family events, upcoming dates, and tiles for every main feature.",
                "डैशबोर्ड आपका शुरुआती पेज है। इसमें आपकी प्रोफ़ाइल, नोटिफ़िकेशन, आज के पारिवारिक कार्यक्रम, आगामी तारीखें और सभी मुख्य सुविधाओं की टाइलें दिखती हैं।"
            ),
            icon = Icons.Default.TipsAndUpdates
        ),
        HelpFaq(
            question = t("How do Profiles and Family Tree work?", "प्रोफ़ाइल और वंश वृक्ष कैसे काम करते हैं?"),
            answer = t(
                "Profiles show family member details. Family Tree gives a visual relationship view. Regular users can view family information and edit only what the app allows for their own profile.",
                "प्रोफ़ाइल में परिवार के सदस्यों की जानकारी दिखती है। वंश वृक्ष रिश्तों को दृश्य रूप में दिखाता है। सामान्य उपयोगकर्ता परिवार की जानकारी देख सकते हैं और अपनी प्रोफ़ाइल में वही बदल सकते हैं जिसकी अनुमति है।"
            ),
            icon = Icons.Default.People
        ),
        HelpFaq(
            question = t("What are Gallery, Memory Lane, Discussions, and Messages?", "गैलरी, यादें, चर्चा और संदेश क्या हैं?"),
            answer = t(
                "Gallery is for photos, Memory Lane is for memories, Discussions are for family conversations, and Messages are private chats with family members.",
                "गैलरी फोटो के लिए है, यादें पुरानी यादों के लिए हैं, चर्चा पारिवारिक बातचीत के लिए है, और संदेश परिवार के सदस्यों के साथ निजी चैट के लिए हैं।"
            ),
            icon = Icons.Default.Collections
        ),
        HelpFaq(
            question = t("How do events and reminders help me?", "कार्यक्रम और रिमाइंडर मेरी कैसे मदद करते हैं?"),
            answer = t(
                "Today's Events and Coming Up highlight birthdays, anniversaries, remembrances, and calendar items so you can call, message, or send wishes on time.",
                "आज के कार्यक्रम और आगामी भाग जन्मदिन, वर्षगांठ, पुण्यतिथि और कैलेंडर आइटम दिखाते हैं, ताकि आप समय पर कॉल, संदेश या शुभकामना भेज सकें।"
            ),
            icon = Icons.Default.CalendarMonth
        ),
        HelpFaq(
            question = t("What are Cookbook, Traditions, Business, Achievements, and Games?", "नुस्खे, परंपरा, व्यवसाय, उपलब्धियां और खेल क्या हैं?"),
            answer = t(
                "Cookbook stores recipes, Traditions preserves family practices, Business lists family businesses, Achievements celebrates milestones, and Family Games gives shared activities.",
                "नुस्खे रेसिपी के लिए हैं, परंपरा पारिवारिक रीति-रिवाजों को संजोती है, व्यवसाय परिवार के व्यवसाय दिखाता है, उपलब्धियां सफलताओं को मनाती हैं, और खेल साथ में गतिविधियां देते हैं।"
            ),
            icon = Icons.Default.Forum
        ),
        HelpFaq(
            question = t("What should I use in an emergency?", "आपातकाल में क्या उपयोग करें?"),
            answer = t(
                "Use the Emergency tile for quick access to important numbers and nearby services. Use it when you need help fast.",
                "महत्वपूर्ण नंबरों और आसपास की सेवाओं तक जल्दी पहुंचने के लिए आपातकाल टाइल का उपयोग करें। जब तुरंत मदद चाहिए तब इसका उपयोग करें।"
            ),
            icon = Icons.Default.Emergency
        ),
        HelpFaq(
            question = t("Why do I see notifications?", "मुझे नोटिफ़िकेशन क्यों दिखते हैं?"),
            answer = t(
                "Notifications tell you about new messages, family updates, approvals, comments, and other activity that may need your attention.",
                "नोटिफ़िकेशन नए संदेश, पारिवारिक अपडेट, अनुमोदन, टिप्पणियां और अन्य गतिविधियां बताते हैं जिन पर आपका ध्यान चाहिए।"
            ),
            icon = Icons.Default.Notifications
        ),
        HelpFaq(
            question = t("Who can approve or change family data?", "परिवार का डेटा कौन अनुमोदित या बदल सकता है?"),
            answer = t(
                "Some changes may need admin review. Regular users get a simple view focused on browsing, connecting, and submitting allowed information.",
                "कुछ बदलावों के लिए एडमिन समीक्षा की जरूरत हो सकती है। सामान्य उपयोगकर्ताओं को देखने, जुड़ने और अनुमत जानकारी भेजने पर केंद्रित सरल दृश्य मिलता है।"
            ),
            icon = Icons.Default.Security
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = t("Help", "मदद"),
                subtitle = t("FAQs and basic features", "FAQ और बुनियादी सुविधाएं"),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                }
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Help, null, tint = Color(0xFFFFC857), modifier = Modifier.size(36.dp))
                            Column(modifier = Modifier.padding(start = 14.dp)) {
                                Text(
                                    t("Quick Guide", "त्वरित गाइड"),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    t(
                                        "Use this page whenever you need a refresher on the app's main buttons and tiles.",
                                        "ऐप के मुख्य बटन और टाइलों की जानकारी दोबारा देखने के लिए इस पेज का उपयोग करें।"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }

                items(faqs) { faq ->
                    HelpFaqCard(faq)
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun HelpFaqCard(faq: HelpFaq) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(faq.icon, null, tint = Color(0xFFFFC857), modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    faq.question,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.76f)
                )
            }
        }
    }
}
