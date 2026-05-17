package com.purawale.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purawale.app.ui.theme.CircleBirthdaysTheme
import java.time.LocalDate

@Preview(widthDp = 1024, heightDp = 500)
@Composable
fun FeatureGraphic() {
    val darkBrown = Color(0xFF3E2723)
    val midBrown = Color(0xFF5D4037)
    val lightBrown = Color(0xFF8D6E63)
    val accentCream = Color(0xFFFFF3E0)

    Box(
        modifier = Modifier
            .size(width = 1024.dp, height = 500.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(midBrown, darkBrown),
                    radius = 800f
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = lightBrown,
                radius = 350f,
                center = center.copy(x = center.x - 100f),
                alpha = 0.15f,
                style = Stroke(width = 40f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp, vertical = 60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    FeatureIcon(Icons.Default.Cake, accentCream)
                    FeatureIcon(Icons.Default.AccountTree, accentCream)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    FeatureIcon(Icons.Default.CalendarMonth, accentCream)
                    FeatureIcon(Icons.Default.AutoStories, accentCream)
                }
            }

            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Purawale",
                    fontSize = 72.sp,
                    color = accentCream,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 6.sp
                )
                Text(
                    text = "Hum aur Humare",
                    fontSize = 68.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    lineHeight = 64.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .background(accentCream, RoundedCornerShape(50))
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "FAMILY • TRADITIONS • MEMORIES",
                        color = darkBrown,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 512, heightDp = 512)
@Composable
fun StoreIcon() {
    val darkBrown = Color(0xFF3E2723)
    val accentCream = Color(0xFFFFF3E0)

    Box(
        modifier = Modifier
            .size(512.dp)
            .background(darkBrown),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(400.dp)
                .border(12.dp, accentCream.copy(alpha = 0.3f), CircleShape)
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Cake,
                contentDescription = null,
                modifier = Modifier.size(180.dp),
                tint = accentCream
            )
            Text(
                text = "PURAWALE",
                color = Color.White,
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
        }
    }
}

@Composable
fun FeatureIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            tint = color
        )
    }
}

private val mockUser = Member(
    id = "1",
    name = "Arjun Purawale",
    dateOfBirth = LocalDate.now().toString(),
    phoneNumber = "9876543210",
    isAdmin = true,
    location = "Mumbai, India",
    marriageDate = "2012-12-25",
    familyId = "1"
)

private val mockMembers = listOf(
    mockUser,
    Member(id="2", name="Priya Purawale", dateOfBirth="1988-09-20", phoneNumber="9876543211", familyId = "10", relationship = "Wife"),
    Member(id="3", name="Aavya Purawale", dateOfBirth="2020-01-01", phoneNumber="", familyId = "11", relationship = "Daughter"),
    Member(id="4", name="Rahul Purawale", dateOfBirth="1992-03-12", phoneNumber="9876543212", familyId = "2", relationship = "Brother"),
    Member(id="5", name="Dada Ji", dateOfBirth="1950-01-01", phoneNumber="9876543213", familyId = "P", relationship = "Grandfather")
)

@Preview(name = "1. Dashboard - Phone", widthDp = 360, heightDp = 640)
@Composable
fun Phone_Dashboard() {
    CircleBirthdaysTheme {
        DashboardScreen(
            user = mockUser,
            allMembers = mockMembers,
            pendingMembers = emptyList(),
            deletionRequests = emptyList(),
            channels = emptyList(),
            onNavigateToProfiles = {},
            onNavigateToGallery = {},
            onNavigateToDiscussions = {},
            onNavigateToMessages = {},
            onLogout = {},
            onEditProfile = {},
            onPasswordChange = {},
            onNavigateToCookbook = {},
            onNavigateToTraditions = {},
            onNavigateToMemoryLane = {},
            onNavigateToFamilyTree = {},
            onNavigateToCalendar = {}
        )
    }
}

@Preview(name = "2. Family Tree - Phone", widthDp = 360, heightDp = 640)
@Composable
fun Phone_FamilyTree() {
    CircleBirthdaysTheme {
        FamilyTreeScreen(
            currentUser = mockUser,
            members = mockMembers,
            onNavigateBack = {},
            onMemberClick = {}
        )
    }
}

@Preview(name = "3. Directory - Phone", widthDp = 360, heightDp = 640)
@Composable
fun Phone_Directory() {
    CircleBirthdaysTheme {
        ProfileListScreen(
            members = mockMembers,
            pendingMembers = emptyList(),
            currentUser = mockUser,
            onEdit = {},
            onAdd = {},
            onBack = {},
            onHome = {},
            onImportCsv = {},
            onApprove = {},
            onClearAll = {},
            onChat = {},
            overrides = emptyList(),
            onApproveOverride = {}
        )
    }
}

@Preview(name = "4. Traditions - Phone", widthDp = 360, heightDp = 640)
@Composable
fun Phone_Traditions() {
    CircleBirthdaysTheme {
        TraditionsScreen(
            user = mockUser,
            traditions = listOf(
                Tradition(id="1", title="Ganesh Chaturthi", description="Our family's oldest tradition.", authorName = "Arjun"),
                Tradition(id="2", title="Family Brunch", description="Every Sunday.", authorName = "Arjun")
            ),
            onBack = {},
            onAddTradition = { _, _ -> },
            onEditTradition = { _, _ -> },
            onDelete = {},
            onToggleReaction = { _, _ -> },
            onAddComment = { _, _ -> }
        )
    }
}

@Preview(name = "5. Cookbook - Phone", widthDp = 360, heightDp = 640)
@Composable
fun Phone_Cookbook() {
    CircleBirthdaysTheme {
        CookbookScreen(
            user = mockUser,
            recipes = listOf(
                Recipe(id="1", title="Puran Poli", authorName="Arjun", description="Traditional sweet flatbread.", category="Dessert"),
                Recipe(id="2", title="Modak", authorName="Priya", description="Steamed dumplings.", category="Dessert")
            ),
            onBack = {},
            onAddRecipe = { _, _ -> },
            onEditRecipe = { _, _ -> },
            onDelete = {},
            onToggleReaction = { _, _ -> },
            onAddComment = { _, _ -> }
        )
    }
}

@Preview(name = "6. Discussions - Phone", widthDp = 360, heightDp = 640)
@Composable
fun Phone_Discussions() {
    CircleBirthdaysTheme {
        DiscussionsScreen(
            user = mockUser,
            discussions = listOf(
                Discussion(id="1", userName="Arjun", title="Next Family Reunion", content="Should we host it in Mahabaleshwar this year?"),
                Discussion(id="2", userName="Rahul", title="Cricket Match", content="Anyone up for a match this Sunday?")
            ),
            onBack = {},
            onPost = { _, _ -> },
            onDelete = {},
            onApprove = {},
            onAddComment = { _, _ -> },
            onVote = { _, _, _ -> }
        )
    }
}

@Preview(name = "7. Calendar - Phone", widthDp = 360, heightDp = 640)
@Composable
fun Phone_Calendar() {
    CircleBirthdaysTheme {
        CalendarScreen(allMembers = mockMembers, onBack = {})
    }
}

@Preview(name = "8. Gallery - Phone", widthDp = 360, heightDp = 640)
@Composable
fun Phone_Gallery() {
    CircleBirthdaysTheme {
        GalleryScreen(
            user = mockUser,
            memories = listOf(
                Memory(id="1", userName="Arjun", caption="Family trip to Mahabaleshwar", status="APPROVED"),
                Memory(id="2", userName="Priya", caption="Aavya's first birthday!", status="APPROVED")
            ),
            onBack = {},
            onUpload = { _, _ -> },
            onApprove = {},
            onDelete = {},
            onToggleReaction = { _, _ -> },
            onAddComment = { _, _ -> }
        )
    }
}

@Preview(name = "9. Memory Lane - Phone", widthDp = 360, heightDp = 640)
@Composable
fun Phone_MemoryLane() {
    CircleBirthdaysTheme {
        MemoryLaneScreen(
            user = mockUser,
            milestones = listOf(
                Milestone(id="1", title="First Family Home", year="1970", description="Bought the ancestral home.", authorName="Arjun"),
                Milestone(id="2", title="Grandpa's Graduation", year="1945", description="A proud moment for the family.", authorName="Arjun")
            ),
            onBack = {},
            onAddMilestone = { _, _ -> },
            onDeleteMilestone = {},
            onToggleReaction = { _, _ -> },
            onAddComment = { _, _ -> }
        )
    }
}

// Tablet 7-inch
@Preview(name = "1. Dashboard - Tab 7", widthDp = 600, heightDp = 960)
@Composable
fun Tab7_Dashboard() { Phone_Dashboard() }

@Preview(name = "2. Family Tree - Tab 7", widthDp = 600, heightDp = 960)
@Composable
fun Tab7_FamilyTree() { Phone_FamilyTree() }

@Preview(name = "3. Directory - Tab 7", widthDp = 600, heightDp = 960)
@Composable
fun Tab7_Directory() { Phone_Directory() }

@Preview(name = "4. Traditions - Tab 7", widthDp = 600, heightDp = 960)
@Composable
fun Tab7_Traditions() { Phone_Traditions() }

@Preview(name = "5. Cookbook - Tab 7", widthDp = 600, heightDp = 960)
@Composable
fun Tab7_Cookbook() { Phone_Cookbook() }

@Preview(name = "6. Discussions - Tab 7", widthDp = 600, heightDp = 960)
@Composable
fun Tab7_Discussions() { Phone_Discussions() }

@Preview(name = "7. Calendar - Tab 7", widthDp = 600, heightDp = 960)
@Composable
fun Tab7_Calendar() { Phone_Calendar() }

@Preview(name = "8. Gallery - Tab 7", widthDp = 600, heightDp = 960)
@Composable
fun Tab7_Gallery() { Phone_Gallery() }

@Preview(name = "9. Memory Lane - Tab 7", widthDp = 600, heightDp = 960)
@Composable
fun Tab7_MemoryLane() { Phone_MemoryLane() }

// Tablet 10-inch
@Preview(name = "1. Dashboard - Tab 10", widthDp = 800, heightDp = 1280)
@Composable
fun Tab10_Dashboard() { Phone_Dashboard() }

@Preview(name = "2. Family Tree - Tab 10", widthDp = 800, heightDp = 1280)
@Composable
fun Tab10_FamilyTree() { Phone_FamilyTree() }

@Preview(name = "3. Directory - Tab 10", widthDp = 800, heightDp = 1280)
@Composable
fun Tab10_Directory() { Phone_Directory() }

@Preview(name = "4. Traditions - Tab 10", widthDp = 800, heightDp = 1280)
@Composable
fun Tab10_Traditions() { Phone_Traditions() }

@Preview(name = "5. Cookbook - Tab 10", widthDp = 800, heightDp = 1280)
@Composable
fun Tab10_Cookbook() { Phone_Cookbook() }

@Preview(name = "6. Discussions - Tab 10", widthDp = 800, heightDp = 1280)
@Composable
fun Tab10_Discussions() { Phone_Discussions() }

@Preview(name = "7. Calendar - Tab 10", widthDp = 800, heightDp = 1280)
@Composable
fun Tab10_Calendar() { Phone_Calendar() }

@Preview(name = "8. Gallery - Tab 10", widthDp = 800, heightDp = 1280)
@Composable
fun Tab10_Gallery() { Phone_Gallery() }

@Preview(name = "9. Memory Lane - Tab 10", widthDp = 800, heightDp = 1280)
@Composable
fun Tab10_MemoryLane() { Phone_MemoryLane() }
