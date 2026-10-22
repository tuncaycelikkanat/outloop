package com.nomorereels.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.theme.NmrTheme

@Composable
fun BedtimeTimePickerDialog(
    startH: Int,
    startM: Int,
    endH: Int,
    endM: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var curStartH by remember { mutableIntStateOf(startH) }
    var curStartM by remember { mutableIntStateOf(startM) }
    var curEndH by remember { mutableIntStateOf(endH) }
    var curEndM by remember { mutableIntStateOf(endM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(NmrTheme.dimens.dialogRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = strings.bedtimeDialogTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = strings.bedtimeDialogDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Başlangıç Saati
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.bedtimeStartLabel, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { curStartH = (curStartH + 23) % 24 },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("-") }
                        Text(
                            text = String.format("%02d:%02d", curStartH, curStartM),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Button(
                            onClick = { curStartH = (curStartH + 1) % 24 },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+") }
                    }
                }

                // Bitiş Saati
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.bedtimeEndLabel, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { curEndH = (curEndH + 23) % 24 },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("-") }
                        Text(
                            text = String.format("%02d:%02d", curEndH, curEndM),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Button(
                            onClick = { curEndH = (curEndH + 1) % 24 },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(curStartH, curStartM, curEndH, curEndM) },
                shape = CircleShape
            ) {
                Text(text = strings.save, style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = strings.cancel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    )
}
