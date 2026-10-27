package com.raulhrn.vox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val ScreenPadding = 20.dp

@Composable
fun Header(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Vox.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconBtn(VoxIcons.Back, "Voltar", onBack)
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Text(subtitle.uppercase(), style = VoxType.caps, color = c.muted)
            Text(
                title,
                style = if (onBack == null) VoxType.title else VoxType.heading,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

@Composable
fun IconBtn(icon: ImageVector, description: String, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color? = null) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint ?: Vox.colors.text, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun VoxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val c = Vox.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .then(if (primary) Modifier.background(c.accent) else Modifier.border(1.dp, c.line, shape))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (primary) c.onAccent else c.text
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = VoxType.label, color = fg)
    }
}

@Composable
fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit, size: Dp = 64.dp, primary: Boolean = true) {
    val c = Vox.colors
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (primary) Modifier.background(c.accent) else Modifier.background(c.surface2))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = if (primary) c.onAccent else c.text, modifier = Modifier.size(size * 0.42f))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Vox.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .then(if (selected) Modifier.background(c.text) else Modifier.border(1.dp, c.line, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = VoxType.label, color = if (selected) c.bg else c.text, maxLines = 1)
    }
}

@Composable
fun Section(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        style = VoxType.caps,
        color = Vox.colors.muted,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Vox.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun ListRow(title: String, subtitle: String? = null, trailing: String? = null, onClick: () -> Unit) {
    val c = Vox.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = VoxType.body, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = VoxType.small, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Text(trailing, style = VoxType.small, color = c.muted, modifier = Modifier.padding(start = 12.dp))
        }
        Icon(VoxIcons.Chevron, null, tint = c.faint, modifier = Modifier.padding(start = 8.dp).size(20.dp))
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Vox.colors.line))
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, detail: String? = null) {
    val c = Vox.colors
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = VoxType.body, color = c.text)
            if (detail != null) Text(detail, style = VoxType.small, color = c.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent,
                checkedTrackColor = c.accent,
                uncheckedThumbColor = c.muted,
                uncheckedTrackColor = c.surface2,
                uncheckedBorderColor = c.line,
            ),
        )
    }
}

@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, detail: String? = null) {
    val c = Vox.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = VoxType.body, color = c.text)
            if (detail != null) Text(detail, style = VoxType.small, color = c.muted)
        }
        RoundButton(VoxIcons.Remove, "Diminuir", onMinus, size = 36.dp, primary = false)
        Text(
            value,
            style = VoxType.label,
            color = c.text,
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        RoundButton(VoxIcons.Add, "Aumentar", onPlus, size = 36.dp, primary = false)
    }
}

/** Equal-width choice chips laid out in rows of [columns]. */
@Composable
fun <T> ChoiceGrid(
    items: List<T>,
    columns: Int,
    selected: (T) -> Boolean,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val c = Vox.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in items.chunked(columns)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (item in row) {
                    val on = selected(item)
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(shape)
                            .then(if (on) Modifier.background(c.text) else Modifier.border(1.dp, c.line, shape))
                            .clickable { onSelect(item) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label(item), style = VoxType.label, color = if (on) c.bg else c.text, maxLines = 1)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun fieldColors() = Vox.colors.let { c ->
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.text,
        unfocusedBorderColor = c.line,
        focusedLabelColor = c.text,
        unfocusedLabelColor = c.muted,
        cursorColor = c.accent,
        focusedTextColor = c.text,
        unfocusedTextColor = c.text,
    )
}

/** "1:05" style duration. */
fun formatTime(ms: Float): String {
    val total = (ms / 1000f).toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
