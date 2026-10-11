@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.lcktechnologies.budgetplanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lcktechnologies.budgetplanner.data.Bucket

/** A screen body that scrolls and has the standard side margins. */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
    )
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Big coloured summary card with an optional stacked bar underneath. */
@Composable
fun HeroCard(
    label: String,
    big: String,
    sub: String,
    negative: Boolean,
    segments: List<Pair<Double, Color>>,
    base: Double
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LocalHero.current),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.85f))
            Text(
                big,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (negative) Color(0xFFFFB3AC) else Color.White,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )
            if (sub.isNotBlank()) Text(sub, color = Color.White.copy(alpha = 0.92f))

            val segs = segments.filter { it.first > 0 }
            if (segs.isNotEmpty() && base > 0) {
                Row(
                    Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    segs.forEach { (v, c) ->
                        Box(
                            Modifier
                                .weight(v.toFloat().coerceAtLeast(0.0001f))
                                .fillMaxHeight()
                                .background(c)
                        )
                    }
                    val rest = base - segs.sumOf { it.first }
                    if (rest > 0.001) Spacer(Modifier.weight(rest.toFloat()))
                }
            }
        }
    }
}

/** A list row with a coloured dot, name, amount and a progress bar. */
@Composable
fun ProgressRow(
    dot: Color,
    name: String,
    amount: String,
    progress: Float,
    barColor: Color,
    caption: String? = null,
    captionError: Boolean = false,
    extra: String? = null,
    onClick: (() -> Unit)? = null
) {
    val base = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(base.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(10.dp))
            Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(amount, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = if (captionError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (captionError) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (extra != null) {
            Text(extra, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** Segmented control: options are (id, label) pairs. */
@Composable
fun SegTabs(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (id, label) ->
            val sel = id == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (sel) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(id) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (sel) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
fun SelectChip(label: String, selected: Boolean, dot: Color? = null, onClick: () -> Unit) {
    if (dot != null) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(dot)) }
        )
    } else {
        FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
    }
}

/** "< Title >" navigation bar for months / periods. */
@Composable
fun PeriodNav(title: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.Filled.ChevronLeft, contentDescription = null) }
        Text(
            title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, contentDescription = null) }
    }
}

/**
 * Number input. It keeps its own text so typing "12." works, and reports the parsed value.
 * Pass a different [resetKey] to make the field re-read [value] (e.g. after budgets are bulk-updated).
 */
@Composable
fun NumField(
    value: Double,
    onValue: (Double) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    resetKey: Any? = null
) {
    var text by remember(resetKey) { mutableStateOf(if (value > 0) plain(value) else "") }
    OutlinedTextField(
        value = text,
        onValueChange = { s ->
            val cleaned = s.filter { it.isDigit() || it == '.' }
            text = cleaned
            onValue(cleaned.toDoubleOrNull() ?: 0.0)
        },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

@Composable
fun ConfirmDialog(message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("cancel")) } }
    )
}

data class Slice(val name: String, val value: Double, val color: Color)

@Composable
fun DonutChart(slices: List<Slice>, centerText: String, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }.toFloat()
    Box(modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val strokeW = size.minDimension * 0.26f
            val diameter = size.minDimension - strokeW
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            var start = -90f
            val gap = if (slices.size > 1) 3f else 0f
            for (s in slices) {
                val sweep = (s.value.toFloat() / total) * 360f
                drawArc(
                    color = s.color,
                    startAngle = start + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeW)
                )
                start += sweep
            }
        }
        Text(centerText, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun LegendRows(slices: List<Slice>, money: (Double) -> String) {
    val total = slices.sumOf { it.value }
    slices.forEach { s ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(s.color))
            Spacer(Modifier.width(10.dp))
            Text(s.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(money(s.value) + "  " + Math.round(s.value / total * 100) + "%")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Title + donut + legend, used for both the budget plan and the expenses chart. */
@Composable
fun DonutSection(
    title: String,
    slices: List<Slice>,
    centerText: String,
    emptyText: String,
    footer: String?,
    money: (Double) -> String
) {
    SectionTitle(title)
    if (slices.isEmpty()) {
        MutedText(emptyText, Modifier.padding(top = 4.dp))
        return
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        DonutChart(slices, centerText)
    }
    LegendRows(slices, money)
    if (footer != null) MutedText(footer, Modifier.padding(top = 6.dp))
}

@Composable
fun BarChart(buckets: List<Bucket>, barColor: Color) {
    val max = buckets.maxOfOrNull { it.value } ?: 0.0
    if (max <= 0) return
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().height(120.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            buckets.forEach { b ->
                if (b.value <= 0) {
                    Spacer(Modifier.weight(1f))
                } else {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight((b.value / max).toFloat().coerceIn(0.03f, 1f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(barColor)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            buckets.forEach { b ->
                Text(
                    b.label,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
