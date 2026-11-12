/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: SmartPixelsSettingsUI.kt
 * Description: UI component and settings composable for Display feature domain.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmartPixelsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isSmartPixelsEnabled by viewModel.isSmartPixelsEnabled
    var showPermissionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    if (showPermissionSheet) {
        val permissionItems = PermissionUIHelper.getPermissionItems(listOf("ACCESSIBILITY"), context, viewModel)
        PermissionsBottomSheet(
            onDismissRequest = {
                showPermissionSheet = false
                viewModel.check(context)
            },
            featureTitle = R.string.feat_smart_pixels_title,
            permissions = permissionItems,
        )
    }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_grain_24,
                title = stringResource(R.string.smart_pixels_enable_title),
                isChecked = isSmartPixelsEnabled && isAccessibilityEnabled,
                onCheckedChange = { checked ->
                    HapticUtil.performUIHaptic(view)
                    if (isAccessibilityEnabled) {
                        viewModel.setSmartPixelsEnabled(context, checked)
                    } else {
                        showPermissionSheet = true
                    }
                },
                enabled = isAccessibilityEnabled,
                onDisabledClick = { showPermissionSheet = true },
                modifier = Modifier.highlight(highlightSetting == "smart_pixels_enable_toggle"),
            )
        }

        var sliderValue by remember(viewModel.smartPixelsIntensity.floatValue) {
            mutableStateOf(viewModel.smartPixelsIntensity.floatValue)
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.smart_pixels_intensity_title),
                value = sliderValue,
                onValueChange = { value ->
                    sliderValue = value
                },
                onValueChangeFinished = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setSmartPixelsIntensity(context, sliderValue)
                },
                valueRange = 10f..90f,
                increment = 5f,
                iconRes = R.drawable.rounded_blur_linear_24,
                enabled = isSmartPixelsEnabled && isAccessibilityEnabled,
                modifier = Modifier.highlight(highlightSetting == "smart_pixels_intensity_slider"),
            )
        }


        var intervalValue by remember(viewModel.smartPixelsInterval.floatValue) {
            mutableStateOf(viewModel.smartPixelsInterval.floatValue)
        }
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.smart_pixels_interval_title),
                value = intervalValue,
                onValueChange = { intervalValue = it },
                onValueChangeFinished = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setSmartPixelsInterval(intervalValue.toInt())
                },
                valueRange = 5f..30f,
                steps = 4,
                increment = 5f,
                valueFormatter = { "${it.toInt()} min" },
                enabled = isSmartPixelsEnabled && isAccessibilityEnabled,
            )
        }

        var positionsValue by remember(viewModel.smartPixelsPositions.floatValue) {
            mutableStateOf(viewModel.smartPixelsPositions.floatValue)
        }
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.smart_pixels_positions_title),
                value = positionsValue,
                onValueChange = { positionsValue = it },
                onValueChangeFinished = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setSmartPixelsPositions(positionsValue.toInt())
                },
                valueRange = 8f..32f,
                steps = 2,
                increment = 8f,
                valueFormatter = { "${it.toInt()}" },
                enabled = isSmartPixelsEnabled && isAccessibilityEnabled,
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_sync_24,
                title = stringResource(R.string.smart_pixels_random_shift_title),
                description = stringResource(R.string.smart_pixels_random_shift_desc),
                isChecked = viewModel.isSmartPixelsRandomShiftEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performUIHaptic(view)
                    viewModel.setSmartPixelsRandomShiftEnabled(checked)
                },
                enabled = isSmartPixelsEnabled && isAccessibilityEnabled,
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_brightness_auto_24,
                title = stringResource(R.string.smart_pixels_adaptive_intensity_title),
                description = stringResource(R.string.smart_pixels_adaptive_intensity_desc),
                isChecked = viewModel.isSmartPixelsAdaptiveIntensityEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performUIHaptic(view)
                    viewModel.setSmartPixelsAdaptiveIntensityEnabled(checked)
                },
                enabled = isSmartPixelsEnabled && isAccessibilityEnabled,
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_cast_24,
                title = stringResource(R.string.smart_pixels_disable_on_cast_title),
                description = stringResource(R.string.smart_pixels_disable_on_cast_desc),
                isChecked = viewModel.isSmartPixelsDisableOnCastEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performUIHaptic(view)
                    viewModel.setSmartPixelsDisableOnCastEnabled(context, checked)
                },
                enabled = isSmartPixelsEnabled && isAccessibilityEnabled,
                modifier = Modifier.highlight(highlightSetting == "smart_pixels_disable_on_cast_toggle"),
            )
        }
    }
}
