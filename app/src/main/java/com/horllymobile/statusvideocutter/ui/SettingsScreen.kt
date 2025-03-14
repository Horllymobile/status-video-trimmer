package com.horllymobile.statusvideocutter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.horllymobile.statusvideocutter.R
import com.horllymobile.statusvideocutter.appLanguages
import com.horllymobile.statusvideocutter.ui.viewmodel.SettingsViewModel

data class Setting(
    val name: String? = "",
    val value: Any? = null
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val settingsUiState by settingsViewModel.settingsUiState.collectAsState()
    var chunkDropDownExpand by rememberSaveable {
        mutableStateOf(false)
    }

    var langDropDownExpand by rememberSaveable {
        mutableStateOf(false)
    }

    var themDropDownExpand by rememberSaveable {
        mutableStateOf(false)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings))
                    }
                },
                title = {
                    Text(stringResource(R.string.settings))
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) {padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.chunk_duration))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.duration, settingsUiState.chunkDuration?.value.toString().toInt()))
                        IconButton(
                            onClick = {
                                chunkDropDownExpand = true
                            }
                        ) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.arrowDropDown))
                        }
                        DropdownMenu(
                            expanded = chunkDropDownExpand,
                            onDismissRequest = { chunkDropDownExpand = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.duration, 60)) },
                                onClick = {
                                    settingsViewModel.updateChunkDuration(60)
                                    chunkDropDownExpand = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.duration, 30)) },
                                onClick = {
                                    settingsViewModel.updateChunkDuration(30)
                                    chunkDropDownExpand = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
//                Spacer(modifier = Modifier.height(10.dp))
            }
            item {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.language))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        settingsUiState.language?.let { Text(stringResource(it.displayLanguage)) }
                        IconButton(
                            onClick = {
                                langDropDownExpand = true
                            }
                        ) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.arrowDropDown))
                        }
                        DropdownMenu(
                            expanded = langDropDownExpand,
                            onDismissRequest = { langDropDownExpand = false }
                        ) {
                            appLanguages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(lang.displayLanguage)) },
                                    onClick = {
                                        settingsViewModel.updateLanguage(lang)
                                        langDropDownExpand = false
                                    }
                                )
                            }
                        }
                    }


                }
                HorizontalDivider()
            }

            item {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.theme))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${settingsUiState.theme?.value!!}")
                        IconButton(
                            onClick = {
                                themDropDownExpand = true
                            }
                        ) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.arrowDropDown))
                        }
                        DropdownMenu(
                            expanded = themDropDownExpand,
                            onDismissRequest = { themDropDownExpand = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.light)) },
                                onClick = {
                                    settingsViewModel.updateTheme("Light")
                                    themDropDownExpand = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dark)) },
                                onClick = {
                                    settingsViewModel.updateTheme("Dark")
                                    themDropDownExpand = false
                                }
                            )
                        }
                    }


                }
            }

        }
    }
}

@Composable
fun SettingsDropDownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onDismissed: () -> Unit,
    items: List<String> = emptyList(),
    onSelect: (Any) -> Unit
) {
    Box(
        modifier = modifier
            .padding(16.dp)
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissed
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}