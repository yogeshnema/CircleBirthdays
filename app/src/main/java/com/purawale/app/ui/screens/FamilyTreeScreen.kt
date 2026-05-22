package com.purawale.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.purawale.app.*
import com.purawale.app.ui.theme.LightGolden
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyTreeScreen(
    currentUser: Member,
    members: List<Member>,
    onNavigateBack: () -> Unit,
    onViewMember: (Member) -> Unit,
    onEditMember: (Member) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Smooth animations for centering/zooming
    var isUserInteracting by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = if (isUserInteracting) snap() else spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    
    val animOffsetX by animateFloatAsState(
        targetValue = offset.x,
        animationSpec = if (isUserInteracting) snap() else spring(stiffness = Spring.StiffnessLow),
        label = "offsetX"
    )
    val animOffsetY by animateFloatAsState(
        targetValue = offset.y,
        animationSpec = if (isUserInteracting) snap() else spring(stiffness = Spring.StiffnessLow),
        label = "offsetY"
    )
    val animatedOffset = Offset(animOffsetX, animOffsetY)

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var treeCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val memberPositions = remember { mutableStateMapOf<String, Offset>() }
    val expandedNodes = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Track which member is currently "focused" or zoomed in on
    var focusedMemberId by remember { mutableStateOf<String?>(null) }

    val pRoot = members.find { it.familyId == "P" }
    val roots = if (pRoot != null) {
        listOf(pRoot)
    } else {
        members.filter { it.familyId.length == 1 && !it.familyId.endsWith("0") }.sortedBy { it.familyId }
    }

    fun centerOnMember(memberId: String) {
        val member = members.find { it.id == memberId } ?: return
        
        // Auto-expand ancestors
        var fid = member.familyId
        while (fid.isNotEmpty()) {
            val parentFid = if (fid.endsWith("0")) fid.dropLast(1) else fid.dropLast(1)
            val parent = members.find { it.familyId == parentFid || it.familyId == parentFid + "0" }
            if (parent != null) expandedNodes[parent.id] = true
            fid = if (fid.length > 1) fid.dropLast(1) else ""
        }

        scope.launch {
            kotlinx.coroutines.delay(100)
            val pos = memberPositions[memberId] ?: return@launch
            val treeCoords = treeCoordinates ?: return@launch
            
            // The tree is centered in its container. 
            // pos is relative to treeRootCoordinates (the Box containing the Column)
            val treeCenterX = treeCoords.size.width / 2f
            val treeCenterY = treeCoords.size.height / 2f
            
            val targetFromCenter = pos - Offset(treeCenterX, treeCenterY)
            
            scale = 1.0f // Reset scale to 1.0 for readability as requested
            offset = -targetFromCenter
            focusedMemberId = memberId
        }
    }

    fun performZoom(zoomFactor: Float) {
        val oldScale = scale
        scale = (scale * zoomFactor).coerceIn(0.1f, 10f)
        
        val focalPoint = if (focusedMemberId != null) {
            memberPositions[focusedMemberId] ?: Offset(containerSize.width / 2f, containerSize.height / 2f)
        } else {
            Offset(containerSize.width / 2f, containerSize.height / 2f)
        }

        val treeCoords = treeCoordinates ?: return
        val treeContentCenter = Offset(treeCoords.size.width / 2f, treeCoords.size.height / 2f)
        val relativeFocalPoint = focalPoint - treeContentCenter
        
        offset = offset - (relativeFocalPoint * (scale - oldScale))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearchVisible) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(t("Search family...", "परिवार खोजें...")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { isSearchVisible = false; searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        )
                    } else {
                        Text(t("Family Tree", "वंश वृक्ष")) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!isSearchVisible) {
                        IconButton(onClick = { isSearchVisible = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                    }
                    IconButton(onClick = { centerOnMember(currentUser.id) }) { 
                        Icon(Icons.Default.MyLocation, "Find Me", tint = Color(0xFF3E2723)) 
                    }
                    IconButton(onClick = { scale = 1f; offset = Offset.Zero; focusedMemberId = null }) { 
                        Icon(Icons.Default.FilterCenterFocus, "Reset", tint = Color(0xFF3E2723))
                    }
                }
            , colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGolden))
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFEFEBE9))
        ) {
            // Main Canvas for Zoom/Pan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            do {
                                val event = awaitPointerEvent()
                                isUserInteracting = true
                            } while (event.changes.any { it.pressed })
                            isUserInteracting = false
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            scale = (scale * zoom).coerceIn(0.1f, 10f)
                            val effectiveZoom = scale / oldScale
                            
                            val pivot = Offset(containerSize.width / 2f, containerSize.height / 2f)
                            offset = (centroid - pivot) - (centroid - pivot - offset) * effectiveZoom + pan
                            
                            if (zoom != 1f) focusedMemberId = null
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            translationX = animatedOffset.x
                            translationY = animatedOffset.y
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                            clip = false
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .wrapContentSize(unbounded = true) // Allow tree to be larger than screen
                            .onGloballyPositioned { treeCoordinates = it }
                            .align(Alignment.Center)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(200.dp), // Padding to allow space for connectors
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (root in roots) {
                                FamilyNode(
                                    root, 
                                    members, 
                                    currentUser = currentUser,
                                    onView = onViewMember,
                                    onEdit = onEditMember,
                                    onCenter = { centerOnMember(it.id) },
                                    scale = animatedScale, 
                                    focusedMemberId = focusedMemberId,
                                    treeRootCoordinates = treeCoordinates,
                                    onPositionReported = { id, pos -> memberPositions[id] = pos },
                                    expandedNodes = expandedNodes,
                                    viewportSize = containerSize,
                                    viewportOffset = animatedOffset
                                )
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }
                }
            }

            // Search Results Overlay
            if (isSearchVisible && searchQuery.isNotEmpty()) {
                val results = members.filter { it.name.contains(searchQuery, ignoreCase = true) }.take(5)
                if (results.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .width(300.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        LazyColumn {
                            items(results) { member ->
                                ListItem(
                                    headlineContent = { Text(member.name) },
                                    supportingContent = { Text(member.relationship ?: "") },
                                    modifier = Modifier.clickable {
                                        centerOnMember(member.id)
                                        isSearchVisible = false
                                        searchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { performZoom(1.2f) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Add, "Zoom In")
                }
                SmallFloatingActionButton(
                    onClick = { performZoom(0.8f) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Remove, "Zoom Out")
                }
                FloatingActionButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                        focusedMemberId = null
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.CenterFocusStrong, "Reset")
                }
            }
        }
    }
}

@Composable
fun FamilyNode(
    member: Member,
    allMembers: List<Member>,
    currentUser: Member,
    onView: (Member) -> Unit,
    onEdit: (Member) -> Unit,
    onCenter: (Member) -> Unit,
    scale: Float = 1f,
    focusedMemberId: String? = null,
    treeRootCoordinates: LayoutCoordinates? = null,
    onPositionReported: (String, Offset) -> Unit = { _, _ -> },
    expandedNodes: MutableMap<String, Boolean> = mutableStateMapOf(),
    viewportSize: IntSize = IntSize.Zero,
    viewportOffset: Offset = Offset.Zero
) {
    val isExpanded = expandedNodes[member.id] ?: true
    val spouseId = if (member.familyId.endsWith("0")) member.familyId.dropLast(1) else member.familyId + "0"
    val spouse = allMembers.find { it.familyId == spouseId }
    
    val children = allMembers.filter { 
        val baseId = if (member.familyId.endsWith("0")) member.familyId.dropLast(1) else member.familyId
        if (baseId == "P") {
            it.familyId.length == 1 && it.familyId != "P" && !it.familyId.endsWith("0")
        } else {
            it.familyId.length == baseId.length + 1 && it.familyId.startsWith(baseId) && !it.familyId.endsWith("0")
        }
    }.sortedBy { it.familyId }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomCenter) {
                MemberSmallCard(
                    member = member, 
                    currentUser = currentUser,
                    onView = onView,
                    onEdit = onEdit,
                    onCenter = onCenter,
                    currentScale = scale, 
                    isSelf = member.id == currentUser.id,
                    isFocused = member.id == focusedMemberId,
                    treeRootCoordinates = treeRootCoordinates,
                    onPositioned = { pos -> onPositionReported(member.id, pos) },
                    viewportSize = viewportSize,
                    viewportOffset = viewportOffset
                )
                
                if (children.isNotEmpty()) {
                    IconButton(
                        onClick = { expandedNodes[member.id] = !isExpanded },
                        modifier = Modifier
                            .offset(y = 12.dp)
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                            .border(1.5.dp, Color(0xFF8D6E63), CircleShape)
                            .zIndex(2f)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand/Collapse",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF8D6E63)
                        )
                    }
                }
            }
            
            if (spouse != null) {
                ConnectionLine(isVertical = false, length = 32.dp)
                MemberSmallCard(
                    member = spouse, 
                    currentUser = currentUser,
                    onView = onView,
                    onEdit = onEdit,
                    onCenter = onCenter,
                    currentScale = scale, 
                    isSelf = spouse.id == currentUser.id,
                    isFocused = spouse.id == focusedMemberId,
                    treeRootCoordinates = treeRootCoordinates,
                    onPositioned = { pos -> onPositionReported(spouse.id, pos) },
                    viewportSize = viewportSize,
                    viewportOffset = viewportOffset
                )
            }
        }
        
        if (children.isNotEmpty() && isExpanded) {
            ConnectionLine(isVertical = true, length = 32.dp)
            Row {
                for (index in children.indices) {
                    val child = children[index]
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (children.size > 1) {
                            ConnectionLine(
                                isVertical = false, 
                                length = 0.dp,
                                isStart = index > 0, 
                                isEnd = index < children.size - 1
                            )
                        }
                        ConnectionLine(isVertical = true, length = 32.dp)
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            FamilyNode(
                                child, allMembers, currentUser, onView, onEdit, onCenter, scale, 
                                focusedMemberId, treeRootCoordinates, onPositionReported, expandedNodes,
                                viewportSize, viewportOffset
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionLine(
    isVertical: Boolean,
    length: Dp = 24.dp,
    isStart: Boolean = true,
    isEnd: Boolean = true
) {
    val color = Color(0xFF8D6E63) // Solid brown color for better visibility
    val thickness = 2.dp
    
    Canvas(
        modifier = if (isVertical) {
            Modifier.width(thickness).height(length)
        } else {
            if (length > 0.dp) Modifier.width(length).height(thickness)
            else Modifier.fillMaxWidth().height(thickness)
        }
    ) {
        if (isVertical) {
            drawLine(
                color = color,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = thickness.toPx()
            )
        } else {
            val startX = if (isStart) 0f else size.width / 2
            val endX = if (isEnd) size.width else size.width / 2
            drawLine(
                color = color,
                start = Offset(startX, size.height / 2),
                end = Offset(endX, size.height / 2),
                strokeWidth = thickness.toPx()
            )
        }
    }
}

@Composable
fun MemberSmallCard(
    member: Member, 
    currentUser: Member,
    onView: (Member) -> Unit,
    onEdit: (Member) -> Unit,
    onCenter: (Member) -> Unit,
    currentScale: Float = 1f, 
    isSelf: Boolean = false,
    isFocused: Boolean = false,
    treeRootCoordinates: LayoutCoordinates? = null,
    onPositioned: (Offset) -> Unit = {},
    viewportSize: IntSize = IntSize.Zero,
    viewportOffset: Offset = Offset.Zero
) {
    val focusScale = 1f
    val cardWidth = 150.dp 
    val photoSize = 60.dp

    // Improved gender detection
    val isFemale = member.gender.equals("Female", ignoreCase = true) || 
                   member.gender.equals("F", ignoreCase = true) || 
                   member.gender.contains("स्त्री", ignoreCase = true)
    
    val genderColor = if (isFemale) Color(0xFFE91E63) else Color(0xFF2196F3) // Pink for girls, Blue for guys
    val canEdit = currentUser.isAdmin || currentUser.isEditor || member.id == currentUser.id

    Card(
        onClick = { onCenter(member) },
        modifier = Modifier
            .requiredWidth(cardWidth)
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
                if (isFocused) {
                    shadowElevation = 20.dp.toPx()
                }
            }
            .onGloballyPositioned { coords ->
                treeRootCoordinates?.let { root ->
                    val pos = root.localPositionOf(coords, Offset.Zero)
                    val center = pos + Offset(coords.size.width / 2f, coords.size.height / 2f)
                    onPositioned(center)
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            width = if (isFocused) 4.dp else 3.dp, 
            color = genderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 12.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo Section
            Box(
                modifier = Modifier
                    .size(photoSize)
                    .clip(CircleShape)
                    .background(genderColor.copy(alpha = 0.1f))
                    .border(2.dp, genderColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!member.photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = member.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (isFemale) Icons.Default.Woman else Icons.Default.Man,
                        null, 
                        modifier = Modifier.size(photoSize * 0.7f), 
                        tint = genderColor.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Name
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 16.sp
                ),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = Color(0xFF212121)
            )
            
            // Relationship
            val relationshipText = when {
                isSelf -> t("Me", "मैं")
                !member.relationship.isNullOrEmpty() -> member.relationship
                else -> ""
            }
            
            if (!relationshipText.isNullOrEmpty()) {
                Surface(
                    color = genderColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = relationshipText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        ),
                        color = genderColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dual Access Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onView(member) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "View",
                        tint = genderColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (canEdit) {
                    IconButton(
                        onClick = { onEdit(member) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = genderColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
