package com.lcktechnologies.budgetplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lcktechnologies.budgetplanner.BuildConfig
import com.lcktechnologies.budgetplanner.ui.MutedText
import com.lcktechnologies.budgetplanner.ui.tr
import com.lcktechnologies.budgetplanner.ui.trf

data class Feature(val titleKey: String, val descKey: String)

private val FEATURES = listOf(
    Feature("about_f1_t", "about_f1_d"), Feature("about_f2_t", "about_f2_d"),
    Feature("about_f3_t", "about_f3_d"), Feature("about_f4_t", "about_f4_d"),
    Feature("about_f5_t", "about_f5_d"), Feature("about_f6_t", "about_f6_d"),
    Feature("about_f7_t", "about_f7_d"), Feature("about_f8_t", "about_f8_d"),
    Feature("about_f9_t", "about_f9_d"), Feature("about_f10_t", "about_f10_d")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(lastUpdated: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("about_title")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            MutedText(tr("about_p1"))
            MutedText(tr("about_p2"), Modifier.padding(top = 10.dp))

            SectionH(tr("about_features_h"))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FEATURES.forEach { f ->
                    OutlinedCard(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(tr(f.titleKey), fontWeight = FontWeight.SemiBold)
                            MutedText(tr(f.descKey), Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            SectionH(tr("about_real_life_h"))
            MutedText(tr("about_real_life_p"))
            Text(
                tr("about_tagline"),
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            SectionH(tr("about_mission_h"))
            MutedText(tr("about_mission_p1"))
            MutedText(tr("about_mission_p2"), Modifier.padding(top = 8.dp))
            Text(
                tr("about_mission_tag"), fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)
            )

            HorizontalDivider(Modifier.padding(vertical = 18.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MutedText(trf("about_version", BuildConfig.VERSION_NAME))
            MutedText(tr("about_developer"), Modifier.padding(top = 2.dp))
            MutedText(trf("about_updated", lastUpdated), Modifier.padding(top = 2.dp))

            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp)) {
                Text(tr("done"))
            }
        }
    }
}

@Composable
private fun SectionH(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
    )
}
