package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogLicensePlateV2

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesameware.data.DataModule
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.AccessAddressViewModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.LicensePlateValue
import com.sesameware.smartyard_oem.ui.main.settings.faceSettings.compose.FloodedBrandButton
import com.sesameware.smartyard_oem.ui.main.settings.faceSettings.compose.TransparentButton
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

/**
 * Content of the dialog for adding a license plate number using API version 2.
 * The state is stored in [rememberSaveable]/[remember], so it survives recomposition.
 *
 * When [licensePlate] is passed, the dialog edits it: the form is initialized with its values,
 * and saving removes the original number before adding the updated one.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialogLicensePlateV2Content(
    viewModel: AccessAddressViewModel,
    flatId: Int,
    licensePlate: LicensePlateValue? = null,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
    }

    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
    }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val validityHours = integerResource(R.integer.guest_vehicle_validity_hours)

    val formats by viewModel.supportedPlateFormats.observeAsState()
    val inFlight by viewModel.globalData.progressVisibility.observeAsState(false)

    val countries = remember(formats) {
        formats?.map { PlateCountry(it.countryCode, it.regex) }
    }

    val isEditMode = licensePlate != null

    // The original validity period in the device's time zone (edit mode only).
    val originalStart = remember {
        licensePlate?.validFrom?.let { PlateValidityCalculator.parseIso(it) }
    }
    val originalEnd = remember {
        licensePlate?.validTo?.let { PlateValidityCalculator.parseIso(it) }
    }

    var selectedCode by rememberSaveable { mutableStateOf(licensePlate?.countryCode) }
    val hasEventsTracking = DataModule.providerConfig.hasEventsTracking
    var checked by remember { mutableStateOf(licensePlate?.watcherId != null) }

    // Select the first available country by default when the formats are loaded.
    LaunchedEffect(countries) {
        if (selectedCode == null) {
            selectedCode = countries?.firstOrNull()?.code
        }
    }

    val selectedCountry = remember(countries, selectedCode) {
        countries?.firstOrNull { it.code == selectedCode }
    }

    val validator = remember(selectedCountry?.regex) {
        selectedCountry?.let { PlateNumberValidator(it.regex) }
    }

    var plate by rememberSaveable { mutableStateOf(licensePlate?.number.orEmpty()) }

    // Revalidate the license plate against the selected country's format.
    val plateComplete = validator?.isComplete(plate) == true
    val plateError = plate.isNotEmpty() && !plateComplete

    // Unlimited validity is enabled by default.
    var isUnlimited by rememberSaveable {
        mutableStateOf(originalStart == null && originalEnd == null)
    }
    // When editing, automatic expiration is restored if the original period matches it.
    var autoEnd by rememberSaveable {
        mutableStateOf(
            originalStart != null &&
                    PlateValidityCalculator.autoEnd(originalStart, validityHours) == originalEnd
        )
    }

    var startDate by remember { mutableStateOf<LocalDate?>(originalStart?.toLocalDate()) }
    var startTime by remember { mutableStateOf<LocalTime?>(originalStart?.toLocalTime()) }
    var endDate by remember { mutableStateOf<LocalDate?>(originalEnd?.toLocalDate()) }
    var endTime by remember { mutableStateOf<LocalTime?>(originalEnd?.toLocalTime()) }

    val startDateTime: LocalDateTime? = remember(startDate, startTime) {
        startDate?.let {
            PlateValidityCalculator.combine(
                it,
                startTime ?: LocalTime.MIDNIGHT
            )
        }
    }

    val manualEndDateTime: LocalDateTime? = remember(endDate, endTime) {
        endDate?.let {
            PlateValidityCalculator.combine(
                it,
                endTime ?: LocalTime.MIDNIGHT
            )
        }
    }

    val autoEndDateTime: LocalDateTime? =
        remember(startDateTime, autoEnd, validityHours) {
            if (autoEnd && startDateTime != null) {
                PlateValidityCalculator.autoEnd(startDateTime, validityHours)
            } else {
                null
            }
        }

    // Unlimited validity ignores both start and end date/time.
    val effectiveEndDateTime: LocalDateTime? = when {
        isUnlimited -> null
        autoEnd -> autoEndDateTime
        else -> manualEndDateTime
    }

    // For limited validity, at least one date/time must be specified.
    // When automatic expiration is enabled, the start date/time is mandatory.
    val periodValid = when {
        isUnlimited -> true
        autoEnd -> startDateTime != null &&
                PlateValidityCalculator.isPeriodValid(
                    startDateTime,
                    effectiveEndDateTime
                )

        else -> (startDateTime != null || manualEndDateTime != null) &&
                PlateValidityCalculator.isPeriodValid(
                    startDateTime,
                    effectiveEndDateTime
                )
    }

    // Prevents repeated submissions while the requests of the current submission are running.
    var submitting by remember { mutableStateOf(false) }

    // In edit mode, set once the original number has been removed, so that a retry after
    // a failed addition does not try to remove it again.
    var originalRemoved by rememberSaveable { mutableStateOf(false) }

    // Unlock the form when the requests have finished (on success the dialog is closed instead).
    LaunchedEffect(inFlight) {
        if (!inFlight) {
            submitting = false
        }
    }

    val canConfirm =
        selectedCountry != null &&
                plateComplete &&
                periodValid &&
                !inFlight &&
                !submitting

    // Close the dialog when a successful addition event is received.
    val success by viewModel.addLicensePlateV2Success.observeAsState()

    LaunchedEffect(success) {
        success?.getContentIfNotHandled()?.let {
            onDismiss()
        }
    }

    MaterialTheme {
        Surface(
            shape = RoundedCornerShape(
                dimensionResource(R.dimen.dialog_large_corner_radius)
            ),
            color = colorResource(id = R.color.dialog_background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isEditMode) {
                            R.string.dialog_license_plate_v2_title_edit
                        } else {
                            R.string.dialog_license_plate_v2_title
                        }
                    ),
                    style = MaterialTheme.typography.h6
                )

                Spacer(Modifier.height(16.dp))

                // Country selection.
                Text(
                    text = stringResource(
                        R.string.dialog_license_plate_v2_country
                    )
                )

                Spacer(Modifier.height(4.dp))

                CountrySelector(
                    countries = countries,
                    selectedCountry = selectedCountry,
                    onSelected = { selectedCode = it.code }
                )

                Spacer(Modifier.height(12.dp))

                // License plate number input.
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = colorResource(R.color.brand),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = colorResource(R.color.brand),
                        textColor = colorResource(R.color.accent)
                    ),
                    value = plate,
                    onValueChange = { candidate ->
                        val normalized = normalizePlateNumber(candidate)
                        if (
                            validator == null ||
                            validator.isValidPrefix(normalized)
                        ) {
                            plate = normalized
                        }
                    },
                    singleLine = true,
                    isError = plateError,
                    label = {
                        Text(
                            text = stringResource(
                                R.string.dialog_license_plate_v2_number_hint
                            ),
                            color = colorResource(R.color.brand)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (plateError) {
                    Text(
                        text = stringResource(
                            R.string.dialog_license_plate_v2_number_incomplete
                        ),
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (hasEventsTracking) {
                    Row(
                        modifier = Modifier
                            .clickable {checked = !checked},
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            null,
                            modifier = Modifier.padding(end = 8.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = colorResource(R.color.brand),
                                checkmarkColor = colorResource(R.color.button_text_flooded)
                            )
                        )
                        Text(stringResource(R.string.event_log_track_lp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Unlimited validity toggle.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            R.string.dialog_license_plate_v2_validity_unlimited
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = isUnlimited,
                        onCheckedChange = { isUnlimited = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorResource(R.color.brand),
                            uncheckedThumbColor = Color.Gray,
                            checkedTrackColor = colorResource(R.color.brand),
                            uncheckedTrackColor = Color.Gray
                        )
                    )
                }

                // Period settings are available only for limited validity.
                if (!isUnlimited) {
                    Spacer(Modifier.height(12.dp))

                    // Validity start.
                    DateTimeRow(
                        dateLabel = stringResource(
                            R.string.dialog_license_plate_v2_valid_from_date
                        ),
                        timeLabel = stringResource(
                            R.string.dialog_license_plate_v2_valid_from_time
                        ),
                        date = startDate,
                        time = startTime,
                        dateFormatter = dateFormatter,
                        timeFormatter = timeFormatter,
                        onDateClick = {
                            showDatePicker(context, startDate) {
                                startDate = it
                                if (startTime == null) {
                                    startTime = LocalTime.MIDNIGHT
                                }
                            }
                        },
                        onTimeClick = {
                            showTimePicker(context, startTime) {
                                startTime = it
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Automatic expiration toggle.
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.dialog_license_plate_v2_auto_end,
                                validityHours
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = autoEnd,
                            onCheckedChange = { autoEnd = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorResource(R.color.brand),
                                uncheckedThumbColor = Color.Gray,
                                checkedTrackColor = colorResource(R.color.brand),
                                uncheckedTrackColor = Color.Gray
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    if (autoEnd) {
                        val preview = autoEndDateTime?.let {
                            it.format(dateFormatter) +
                                    " " +
                                    it.format(timeFormatter)
                        } ?: "—"

                        Text(
                            text = stringResource(
                                R.string.dialog_license_plate_v2_auto_end_preview,
                                preview
                            )
                        )
                    } else {
                        // Validity end (manual input).
                        DateTimeRow(
                            dateLabel = stringResource(
                                R.string.dialog_license_plate_v2_valid_to_date
                            ),
                            timeLabel = stringResource(
                                R.string.dialog_license_plate_v2_valid_to_time
                            ),
                            date = endDate,
                            time = endTime,
                            onDateClick = {
                                showDatePicker(context, endDate) {
                                    endDate = it
                                    if (endTime == null) {
                                        endTime = LocalTime.MIDNIGHT
                                    }
                                }
                            },
                            dateFormatter = dateFormatter,
                            timeFormatter = timeFormatter,
                            onTimeClick = {
                                showTimePicker(context, endTime) {
                                    endTime = it
                                }
                            }
                        )
                    }

                    if (!periodValid) {
                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = stringResource(
                                R.string.dialog_license_plate_v2_period_invalid
                            ),
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                FloodedBrandButton(
                    title = stringResource(
                        if (isEditMode) R.string.save else R.string.dialog_license_plate_v2_confirm
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canConfirm,
                    onClick = {
                        val country = selectedCountry
                            ?: return@FloodedBrandButton

                        val validFrom = if (isUnlimited) {
                            null
                        } else {
                            startDateTime?.let {
                                PlateValidityCalculator.toIso(it)
                            }
                        }

                        val validTo = if (isUnlimited) {
                            null
                        } else {
                            effectiveEndDateTime?.let {
                                PlateValidityCalculator.toIso(it)
                            }
                        }

                        val number = plate
                        val trackEvent = checked
                        val addLicensePlate = {
                            viewModel.addLicensePlateV2(
                                flatId = flatId,
                                number = number,
                                countryCode = country.code,
                                validFrom = validFrom,
                                validTo = validTo,
                                trackEvent = trackEvent
                            )
                        }

                        submitting = true
                        if (licensePlate != null && !originalRemoved) {
                            // Edit mode: add the updated number only after the original one has been removed.
                            viewModel.removeLicensePlate(flatId, licensePlate.number) {
                                originalRemoved = true
                                addLicensePlate()
                            }
                        } else {
                            addLicensePlate()
                        }
                    }
                )

                Spacer(Modifier.height(20.dp))

                TransparentButton(
                    title = stringResource(
                        id = R.string.dialog_license_plate_v2_cancel
                    ),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !inFlight && !submitting
                )
            }
        }
    }
}

@Composable
private fun CountrySelector(
    countries: List<PlateCountry>?,
    selectedCountry: PlateCountry?,
    onSelected: (PlateCountry) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = !countries.isNullOrEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedCountry?.let { countryLabel(it) } ?: "—",
                modifier = Modifier.weight(1f),
                color = colorResource(R.color.accent)
            )

            Text(
                text = "▼",
                color = colorResource(R.color.accent)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            countries?.forEach { country ->
                DropdownMenuItem(
                    onClick = {
                        onSelected(country)
                        expanded = false
                    }
                ) {
                    Text(
                        text = countryLabel(country),
                        color = colorResource(R.color.accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun countryLabel(country: PlateCountry): String {
    val name = country.displayNameRes?.let {
        stringResource(it)
    } ?: country.code.uppercase()

    return "${country.flagEmoji}  $name"
}

@Composable
private fun DateTimeRow(
    dateLabel: String,
    timeLabel: String,
    date: LocalDate?,
    time: LocalTime?,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        PickerField(
            label = dateLabel,
            value = date?.format(dateFormatter) ?: "",
            onClick = onDateClick,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(8.dp))

        PickerField(
            label = timeLabel,
            value = time?.format(timeFormatter) ?: "",
            onClick = onTimeClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            singleLine = true,
            label = {
                Text(
                    text = label,
                    style = TextStyle(fontSize = 14.sp)
                )
            },
            textStyle = TextStyle(
                fontSize = 14.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Transparent clickable overlay because the field is read-only.
        Box(
            modifier = Modifier
                .matchClickOverlay()
                .clickable(onClick = onClick)
        )
    }
}

private fun Modifier.matchClickOverlay(): Modifier = this
    .fillMaxWidth()
    .height(56.dp)

private fun showDatePicker(
    context: android.content.Context,
    current: LocalDate?,
    onPicked: (LocalDate) -> Unit
) {
    val base = current ?: LocalDate.now()

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onPicked(LocalDate.of(year, month + 1, dayOfMonth))
        },
        base.year,
        base.monthValue - 1,
        base.dayOfMonth
    ).show()
}

private fun showTimePicker(
    context: android.content.Context,
    current: LocalTime?,
    onPicked: (LocalTime) -> Unit
) {
    val base = current ?: LocalTime.now()

    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onPicked(LocalTime.of(hourOfDay, minute))
        },
        base.hour,
        base.minute,
        true
    ).show()
}

private val cyrillicToLatin = mapOf(
    'А' to 'A',
    'В' to 'B',
    'Е' to 'E',
    'К' to 'K',
    'М' to 'M',
    'Н' to 'H',
    'О' to 'O',
    'Р' to 'P',
    'С' to 'C',
    'Т' to 'T',
    'У' to 'Y',
    'Х' to 'X'
)

private fun normalizePlateNumber(value: String): String =
    value.uppercase().map { cyrillicToLatin[it] ?: it }.joinToString("")
