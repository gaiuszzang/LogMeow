package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem

import androidx.compose.material.Surface
import androidx.compose.material.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import io.groovin.logmeow.interceptor.MockApiSettingDto
import ui.common.AppTheme
import ui.common.ContextDropdownMenu
import ui.common.DarkMenuItem
import ui.common.Direction
import ui.common.DragHandle
import ui.common.LazyListScrollBar
import ui.common.LogMeowColors
import ui.common.OutlinedSingleTextField
import vm.MockApiDialogRequest
import java.util.UUID

private val HTTP_METHODS = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")

private data class InitialMockState(
    val selectedIndex: Int,
    val isEditMode: Boolean,
    val isAddMode: Boolean,
    val method: String,
    val url: String,
    val statusCode: String,
    val headers: List<Pair<String, String>>,
    val responseBody: String,
    val delayMs: String
)

@Composable
fun MockApiSettingScreen(
    mockApiSettings: List<MockApiSettingDto>,
    onAdd: (MockApiSettingDto) -> Unit,
    onUpdate: (MockApiSettingDto) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
    dialogRequest: MockApiDialogRequest? = null
) {
    // Compute initial state from dialogRequest
    val initialState = remember(dialogRequest) {
        if (dialogRequest == null) return@remember null
        val method = dialogRequest.prefillMethod ?: "GET"
        val url = dialogRequest.prefillUrl ?: ""
        val matchIndex = mockApiSettings.indexOfFirst {
            it.method.equals(method, ignoreCase = true) && it.url == url
        }
        if (matchIndex >= 0) {
            val setting = mockApiSettings[matchIndex]
            InitialMockState(
                selectedIndex = matchIndex,
                isEditMode = false,
                isAddMode = false,
                method = setting.method,
                url = setting.url,
                statusCode = setting.statusCode.toString(),
                headers = setting.responseHeaders.entries.map { it.key to it.value },
                responseBody = setting.responseBody ?: "",
                delayMs = setting.delayMs.toString()
            )
        } else {
            InitialMockState(
                selectedIndex = -1,
                isEditMode = true,
                isAddMode = true,
                method = method,
                url = url,
                statusCode = (dialogRequest.prefillStatusCode ?: 200).toString(),
                headers = dialogRequest.prefillResponseHeaders.entries.map { it.key to it.value },
                responseBody = tryFormatJson(dialogRequest.prefillResponseBody ?: ""),
                delayMs = "0"
            )
        }
    }

    var selectedIndex by remember { mutableStateOf(initialState?.selectedIndex ?: -1) }
    var isEditMode by remember { mutableStateOf(initialState?.isEditMode ?: false) }
    var isAddMode by remember { mutableStateOf(initialState?.isAddMode ?: false) }
    var duplicateError by remember { mutableStateOf(false) }

    // Edit state
    var editMethod by remember { mutableStateOf(initialState?.method ?: "GET") }
    var editUrl by remember { mutableStateOf(initialState?.url ?: "") }
    var editStatusCode by remember { mutableStateOf(initialState?.statusCode ?: "200") }
    var editHeaders by remember { mutableStateOf(initialState?.headers ?: emptyList()) }
    var editResponseBody by remember { mutableStateOf(initialState?.responseBody ?: "") }
    var editDelayMs by remember { mutableStateOf(initialState?.delayMs ?: "0") }

    val selectedSetting = if (selectedIndex in mockApiSettings.indices) mockApiSettings[selectedIndex] else null

    fun loadSetting(setting: MockApiSettingDto) {
        editMethod = setting.method
        editUrl = setting.url
        editStatusCode = setting.statusCode.toString()
        editHeaders = setting.responseHeaders.entries.map { it.key to it.value }
        editResponseBody = setting.responseBody ?: ""
        editDelayMs = setting.delayMs.toString()
    }

    fun selectSetting(index: Int) {
        selectedIndex = index
        val setting = mockApiSettings.getOrNull(index) ?: return
        loadSetting(setting)
        isEditMode = false
        isAddMode = false
        duplicateError = false
    }

    fun enterEditMode() {
        editResponseBody = tryFormatJson(editResponseBody)
        isEditMode = true
        duplicateError = false
    }

    fun enterAddMode(
        method: String = "GET",
        url: String = "",
        statusCode: String = "200",
        headers: List<Pair<String, String>> = emptyList(),
        responseBody: String = "",
        delayMs: String = "0"
    ) {
        editMethod = method
        editUrl = url
        editStatusCode = statusCode
        editHeaders = headers
        editResponseBody = responseBody
        editDelayMs = delayMs
        isAddMode = true
        isEditMode = true
        selectedIndex = -1
        duplicateError = false
    }

    fun cancelEdit() {
        duplicateError = false
        if (isAddMode) {
            isEditMode = false
            isAddMode = false
        } else if (selectedSetting != null) {
            loadSetting(selectedSetting)
            isEditMode = false
        }
    }

    fun hasDuplicate(): Boolean {
        return mockApiSettings.any { existing ->
            existing.method.equals(editMethod, ignoreCase = true) &&
                existing.url == editUrl &&
                existing.id != selectedSetting?.id
        }
    }

    fun saveEdit() {
        if (editUrl.isBlank()) return
        if (hasDuplicate()) {
            duplicateError = true
            return
        }
        duplicateError = false
        val headers = editHeaders.filter { it.first.isNotBlank() }.associate { it.first to it.second }
        val statusCode = editStatusCode.toIntOrNull() ?: 200
        val body = editResponseBody.ifBlank { null }
        val delay = editDelayMs.toLongOrNull() ?: 0

        if (isAddMode) {
            val setting = MockApiSettingDto(
                id = UUID.randomUUID().toString(),
                method = editMethod,
                url = editUrl,
                statusCode = statusCode,
                responseHeaders = headers,
                responseBody = body,
                delayMs = delay
            )
            onAdd(setting)
        } else if (selectedSetting != null) {
            val updated = selectedSetting.copy(
                method = editMethod,
                url = editUrl,
                statusCode = statusCode,
                responseHeaders = headers,
                responseBody = body,
                delayMs = delay
            )
            onUpdate(updated)
        }
        isEditMode = false
        isAddMode = false
    }

    fun deleteSetting(index: Int) {
        val setting = mockApiSettings.getOrNull(index) ?: return
        onDelete(setting.id)
        if (selectedIndex == index) {
            selectedIndex = -1
            isEditMode = false
            isAddMode = false
        } else if (selectedIndex > index) {
            selectedIndex--
        }
    }

    fun copySetting(index: Int) {
        val setting = mockApiSettings.getOrNull(index) ?: return
        enterAddMode(
            method = setting.method,
            url = setting.url,
            statusCode = setting.statusCode.toString(),
            headers = setting.responseHeaders.entries.map { it.key to it.value },
            responseBody = setting.responseBody ?: "",
            delayMs = setting.delayMs.toString()
        )
    }

    Window(
        onCloseRequest = onDismiss,
        title = "Mock API Settings",
        state = rememberWindowState(
            width = 1400.dp,
            height = 700.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )
    ) {
        AppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = LogMeowColors.DarkBackground
            ) {
                var splitFraction by remember { mutableStateOf(0.35f) }
                var totalWidth by remember { mutableStateOf(0f) }
                val density = LocalDensity.current

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .onSizeChanged { totalWidth = it.width.toFloat() }
                ) {
                    // Left: Mock API list
                    MockApiList(
                        settings = mockApiSettings,
                        selectedIndex = selectedIndex,
                        isEditMode = isEditMode,
                        onSelect = { selectSetting(it) },
                        onAdd = { enterAddMode() },
                        onDelete = { deleteSetting(it) },
                        onCopy = { copySetting(it) },
                        modifier = Modifier
                            .weight(splitFraction)
                            .fillMaxHeight()
                    )

                    // Drag handle
                    DragHandle(
                        isVertical = true,
                        onDrag = { delta ->
                            if (totalWidth > 0) {
                                val px = with(density) { delta.dp.toPx() }
                                splitFraction = (splitFraction + px / totalWidth)
                                    .coerceIn(0.2f, 0.6f)
                            }
                        }
                    )

                    // Right: Detail / Editor (unified)
                    MockApiDetail(
                        hasSelection = isAddMode || selectedSetting != null,
                        isEditMode = isEditMode,
                        editMethod = editMethod,
                        editUrl = editUrl,
                        editStatusCode = editStatusCode,
                        editHeaders = editHeaders,
                        editResponseBody = editResponseBody,
                        editDelayMs = editDelayMs,
                        onEditMethodChange = { editMethod = it },
                        onEditUrlChange = { editUrl = it },
                        onEditStatusCodeChange = { editStatusCode = it },
                        onEditHeadersChange = { editHeaders = it },
                        onEditResponseBodyChange = { editResponseBody = it },
                        onEditDelayMsChange = { editDelayMs = it },
                        onEdit = { enterEditMode() },
                        onDelete = {
                            selectedSetting?.let {
                                onDelete(it.id)
                                selectedIndex = -1
                            }
                        },
                        onSave = { saveEdit() },
                        onCancel = { cancelEdit() },
                        isUrlEmpty = editUrl.isBlank(),
                        duplicateError = duplicateError,
                        modifier = Modifier
                            .weight(1f - splitFraction)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun MockApiList(
    settings: List<MockApiSettingDto>,
    selectedIndex: Int,
    isEditMode: Boolean,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
    onCopy: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Text(
            text = "Mock APIs (${settings.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                .background(LogMeowColors.PanelBackground)
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 8.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.hasFocus }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || isEditMode) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> {
                            if (selectedIndex > 0) {
                                val newIndex = selectedIndex - 1
                                onSelect(newIndex)
                                coroutineScope.launch { listState.animateScrollToItem(newIndex) }
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            if (selectedIndex < settings.size - 1) {
                                val newIndex = selectedIndex + 1
                                onSelect(newIndex)
                                coroutineScope.launch { listState.animateScrollToItem(newIndex) }
                            }
                            true
                        }
                        Key.PageUp -> {
                            coroutineScope.launch {
                                val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                                listState.animateScrollBy(-viewportHeight.toFloat())
                                val firstVisible = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                                if (firstVisible != null && firstVisible.index in settings.indices) {
                                    onSelect(firstVisible.index)
                                }
                            }
                            true
                        }
                        Key.PageDown -> {
                            coroutineScope.launch {
                                val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                                listState.animateScrollBy(viewportHeight.toFloat())
                                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                                if (lastVisible != null && lastVisible.index in settings.indices) {
                                    onSelect(lastVisible.index)
                                }
                            }
                            true
                        }
                        else -> false
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusRequester.requestFocus() }
        ) {
            itemsIndexed(settings) { index, setting ->
                val isSelected = index == selectedIndex
                val itemAlpha = if (isEditMode && !isSelected) 0.4f else 1f
                val backgroundColor = when {
                    isSelected && isFocused -> LogMeowColors.SelectedFocused
                    isSelected -> LogMeowColors.SelectedUnfocused
                    else -> Color.Transparent
                }
                ContextDropdownMenu(
                    items = {
                        if (isEditMode) emptyList()
                        else listOf(
                            DarkMenuItem("Copy to New") { onCopy(index) },
                            DarkMenuItem("Delete", color = LogMeowColors.Danger) { onDelete(index) }
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(backgroundColor)
                            .then(
                                if (!isEditMode) Modifier.clickable {
                                    onSelect(index)
                                    focusRequester.requestFocus()
                                }
                                else Modifier
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val mColor = LogMeowColors.methodColor(setting.method)
                        Text(
                            text = setting.method,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = mColor.copy(alpha = itemAlpha),
                            modifier = Modifier.width(60.dp)
                        )
                        Text(
                            text = setting.url,
                            fontSize = 12.sp,
                            color = Color.LightGray.copy(alpha = itemAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            // Add button at the end
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .then(
                            if (!isEditMode) Modifier.clickable { onAdd() }
                            else Modifier
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "+ Add",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isEditMode) Color.Gray else LogMeowColors.Accent
                    )
                }
            }
        }
            LazyListScrollBar(
                state = listState,
                direction = Direction.Vertical,
                thickness = 8.dp,
                color = Color.Gray,
                backgroundColor = Color.DarkGray,
            )
        }
    }
}

@Composable
private fun MockApiDetail(
    hasSelection: Boolean,
    isEditMode: Boolean,
    editMethod: String,
    editUrl: String,
    editStatusCode: String,
    editHeaders: List<Pair<String, String>>,
    editResponseBody: String,
    editDelayMs: String,
    onEditMethodChange: (String) -> Unit,
    onEditUrlChange: (String) -> Unit,
    onEditStatusCodeChange: (String) -> Unit,
    onEditHeadersChange: (List<Pair<String, String>>) -> Unit,
    onEditResponseBodyChange: (String) -> Unit,
    onEditDelayMsChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isUrlEmpty: Boolean,
    duplicateError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Title
        Text(
            text = "Mock Contents",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                .background(LogMeowColors.PanelBackground, RoundedCornerShape(4.dp))
        ) {
            if (!hasSelection) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a mock API or add a new one",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Method selector + URL
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MethodDropdown(
                            selected = editMethod,
                            onSelect = onEditMethodChange,
                            enabled = isEditMode
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedSingleTextField(
                            value = editUrl,
                            onValueChange = onEditUrlChange,
                            label = "URL *",
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = isEditMode && isUrlEmpty,
                            readOnly = !isEditMode
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Response Settings
                    Text("Response Settings", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    OutlinedSingleTextField(
                        value = editStatusCode,
                        onValueChange = { onEditStatusCodeChange(it.filter { c -> c.isDigit() }) },
                        label = "Status Code",
                        modifier = Modifier.width(180.dp),
                        singleLine = true,
                        readOnly = !isEditMode
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedSingleTextField(
                        value = editDelayMs,
                        onValueChange = { onEditDelayMsChange(it.filter { c -> c.isDigit() }) },
                        label = "Additional Delay (ms)",
                        modifier = Modifier.width(180.dp),
                        singleLine = true,
                        readOnly = !isEditMode
                    )

                    Spacer(Modifier.height(12.dp))

                    // Response Headers
                    Text("Response Headers", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    HeaderEditor(
                        headers = editHeaders,
                        onChange = onEditHeadersChange,
                        readOnly = !isEditMode
                    )

                    Spacer(Modifier.height(12.dp))

                    // Response Body
                    var prettyJsonEnabled by remember { mutableStateOf(true) }
                    val isJsonBody = editResponseBody.trim().let {
                        it.startsWith("{") || it.startsWith("[")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Response Body", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        if (!isEditMode && isJsonBody) {
                            Spacer(Modifier.width(8.dp))
                            Checkbox(
                                checked = prettyJsonEnabled,
                                onCheckedChange = { prettyJsonEnabled = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = LogMeowColors.Accent,
                                    uncheckedColor = Color.Gray,
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.size(14.dp).scale(0.75f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Pretty Json",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.clickable { prettyJsonEnabled = !prettyJsonEnabled }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val displayBody = if (!isEditMode && isJsonBody && prettyJsonEnabled) {
                        tryFormatJson(editResponseBody)
                    } else {
                        editResponseBody
                    }
                    OutlinedSingleTextField(
                        value = displayBody,
                        onValueChange = onEditResponseBodyChange,
                        label = "",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        fontFamily = FontFamily.Monospace,
                        readOnly = !isEditMode
                    )
                }
            }
        }

        // Bottom buttons (separated from content area)
        if (hasSelection) {
            if (duplicateError) {
                Text(
                    text = "A mock with the same Method and URL already exists.",
                    fontSize = 12.sp,
                    color = LogMeowColors.Danger,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (duplicateError) 4.dp else 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (isEditMode) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = LogMeowColors.ButtonBackground,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        enabled = !isUrlEmpty,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = LogMeowColors.AccentBackground,
                            contentColor = Color.White,
                            disabledBackgroundColor = LogMeowColors.DisabledBackground,
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Text("Save", fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = LogMeowColors.DeleteButtonBackground,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Delete", fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = LogMeowColors.AccentBackground,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Edit", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}


@Composable
private fun MethodDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val color = LogMeowColors.methodColor(selected)

    Box {
        Box(
            modifier = Modifier
                .width(90.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .then(if (enabled) Modifier.clickable { expanded = true } else Modifier)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = selected,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) color else Color.Gray
            )
        }
        if (enabled) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                HTTP_METHODS.forEach { method ->
                    DropdownMenuItem(onClick = {
                        onSelect(method)
                        expanded = false
                    }) {
                        Text(method, fontSize = 13.sp, color = LogMeowColors.methodColor(method), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderEditor(
    headers: List<Pair<String, String>>,
    onChange: (List<Pair<String, String>>) -> Unit,
    readOnly: Boolean = false
) {
    Column {
        if (headers.isEmpty() && readOnly) {
            Text("(none)", fontSize = 12.sp, color = Color.Gray)
        }
        headers.forEachIndexed { index, (key, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                OutlinedSingleTextField(
                    value = key,
                    onValueChange = { newKey ->
                        onChange(headers.toMutableList().apply { set(index, newKey to value) })
                    },
                    label = "Key",
                    modifier = Modifier.weight(0.3f),
                    singleLine = true,
                    readOnly = readOnly
                )
                Spacer(Modifier.width(4.dp))
                OutlinedSingleTextField(
                    value = value,
                    onValueChange = { newValue ->
                        onChange(headers.toMutableList().apply { set(index, key to newValue) })
                    },
                    label = "Value",
                    modifier = Modifier.weight(0.6f),
                    singleLine = true,
                    readOnly = readOnly
                )
                if (!readOnly) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "X",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LogMeowColors.Danger,
                        modifier = Modifier
                            .clickable {
                                onChange(headers.toMutableList().apply { removeAt(index) })
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
        if (!readOnly) {
            Text(
                text = "+ Add Header",
                fontSize = 12.sp,
                color = LogMeowColors.Accent,
                modifier = Modifier
                    .clickable { onChange(headers + ("" to "")) }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

private val prettyJson = kotlinx.serialization.json.Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

private fun tryFormatJson(raw: String): String {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return raw
    return try {
        val element = prettyJson.parseToJsonElement(trimmed)
        prettyJson.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), element)
    } catch (_: Exception) {
        raw
    }
}
