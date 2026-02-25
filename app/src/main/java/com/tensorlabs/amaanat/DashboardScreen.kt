package com.tensorlabs.amaanat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tensorlabs.amaanat.data.AccountEntity
import java.util.Calendar

// Helper Data Class
data class SmsPayload(
    val bMobile: String,
    val bMsg: String,
    val gMobiles: List<String>,
    val gMsg: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current

    // --- MEMORY: SHARED PREFERENCES SETUP ---
    // Check if user has already accepted the disclosure
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val isAlreadyAccepted = remember { prefs.getBoolean("disclosure_accepted", false) }

    // Show dialog only if NOT already accepted
    var showDisclosure by remember { mutableStateOf(!isAlreadyAccepted) }
    var isDisclosureChecked by remember { mutableStateOf(false) }

    val accounts by viewModel.filteredAccounts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // --- UI STATES ---
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }

    // Bottom Sheet (Edit/Delete)
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Info/About Bottom Sheet
    var showInfoSheet by remember { mutableStateOf(false) }
    val infoSheetState = rememberModalBottomSheetState()

    // SMS Dialog State
    var showSmsDialog by remember { mutableStateOf(false) }
    var accountForSms by remember { mutableStateOf<AccountEntity?>(null) }

    // Pending SMS Data
    var pendingSmsData by remember { mutableStateOf<SmsPayload?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // --- PERMISSION LAUNCHER (UPDATED: Removed READ_SMS) ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        val phoneGranted = permissions[Manifest.permission.READ_PHONE_STATE] ?: false

        if (smsGranted && phoneGranted && pendingSmsData != null) {
            startSmsService(context, pendingSmsData!!)
            pendingSmsData = null
        } else {
            Toast.makeText(context, "Permission Denied! Cannot send SMS.", Toast.LENGTH_LONG).show()
        }
    }

    // --- BLOCK BACK BUTTON (When Disclosure is Open) ---
    BackHandler(enabled = showDisclosure) {
        // Do nothing, forces user to interact with dialog
    }

    // --- MAIN CONTENT ---
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        selectedAccount = null // Clear for new entry
                        showAddEditDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Add, null)
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        })
                    }
            ) {

                // --- 1. SEARCH BAR ---
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search A/c, Name or Bank...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                )

                Spacer(modifier = Modifier.height(12.dp))

                // --- 2. INFO CARD (Triggers Bottom Sheet) ---
                InfoCard(
                    onClick = { showInfoSheet = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. LIST ---
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.nestedScroll(remember {
                        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                return super.onPreScroll(available, source)
                            }
                        }
                    })
                ) {
                    items(accounts) { account ->
                        AccountItem(
                            account = account,
                            onSendClick = {
                                accountForSms = account
                                showSmsDialog = true
                            },
                            onLongClick = {
                                selectedAccount = account
                                showBottomSheet = true
                            }
                        )
                    }
                }
            }

            // --- 4. BOTTOM SHEET (Manage Entry) ---
            if (showBottomSheet && selectedAccount != null) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Manage: ${selectedAccount?.accountNumber}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                showBottomSheet = false
                                showAddEditDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Rounded.Edit, null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Edit Details")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.deleteAccount(selectedAccount!!)
                                showBottomSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Icon(Icons.Rounded.Delete, null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Delete Entry")
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }

            // --- 5. INFO & SUPPORT BOTTOM SHEET ---
            if (showInfoSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showInfoSheet = false },
                    sheetState = infoSheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    AppInfoContent(context)
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            // --- 6. SMS CONFIRMATION DIALOG ---
            if (showSmsDialog && accountForSms != null) {
                SmsConfirmationDialog(
                    account = accountForSms!!,
                    onDismiss = { showSmsDialog = false },
                    onSendConfirmed = { bMobile, bMsg, gMobiles, gMsg ->

                        val payload = SmsPayload(bMobile, bMsg, gMobiles, gMsg)

                        // CHECK PERMISSION FIRST (UPDATED: Removed READ_SMS)
                        val hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                        val hasPhonePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

                        if (hasSmsPermission && hasPhonePermission) {
                            startSmsService(context, payload)
                        } else {
                            pendingSmsData = payload // Save data pending permission
                            permissionLauncher.launch(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE))
                        }

                        showSmsDialog = false
                    }
                )
            }

            // --- 7. FULL ENTRY DIALOG ---
            if (showAddEditDialog) {
                FullEntryDialog(
                    account = selectedAccount,
                    onDismiss = { showAddEditDialog = false },
                    onSave = { updatedAccount ->
                        viewModel.saveAccount(updatedAccount)
                        showAddEditDialog = false
                    }
                )
            }
        }

        // --- 8. GOOGLE PLAY COMPLIANT DISCLOSURE DIALOG ---
        if (showDisclosure) {
            Dialog(
                onDismissRequest = { /* Prevent Dismiss */ },
                properties = DialogProperties(
                    dismissOnBackPress = false, // Back button block
                    dismissOnClickOutside = false, // Outside click block
                    usePlatformDefaultWidth = false
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "SMS Permission Disclosure",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Recovery Intimation utilizes the SEND_SMS permission to perform its core functionality. This permission is required to send professional repayment reminders to borrowers and guarantors directly from the app. Without this, the app cannot facilitate debt recovery. We strictly store your data locally and do not read your private messages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- CHECKBOX SECTION ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isDisclosureChecked = !isDisclosureChecked }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isDisclosureChecked,
                                onCheckedChange = { isDisclosureChecked = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "I understand and agree.",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- BUTTONS ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // DENY BUTTON -> Closes App
                            OutlinedButton(
                                onClick = {
                                    (context as? Activity)?.finish()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Deny")
                            }

                            // ACCEPT BUTTON -> Saves to Prefs & Closes Dialog
                            Button(
                                onClick = {
                                    // Memory mein Save kar lo
                                    prefs.edit().putBoolean("disclosure_accepted", true).apply()
                                    showDisclosure = false
                                },
                                enabled = isDisclosureChecked,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Accept")
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- NEW COMPONENT: INFO CARD ---
@Composable
fun InfoCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "App Info & Support",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

// --- NEW COMPONENT: APP INFO SHEET CONTENT ---
@Composable
fun AppInfoContent(context: Context) {
    // Get App Version (Safe method for all Android versions)
    val versionName = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }
    } catch (e: Exception) {
        "Unknown"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon Placeholder
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Recovery Intimation",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Version $versionName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Actions List
        InfoActionItem(
            icon = Icons.Rounded.BugReport,
            title = "Submit Bug Report",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("business.amaan0@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Bug Report - Recovery Intimation App")
                }
                try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show() }
            }
        )

        InfoActionItem(
            icon = Icons.Rounded.Star,
            title = "Rate on Play Store",
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
                } catch (e: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                }
            }
        )

        InfoActionItem(
            icon = Icons.Rounded.PrivacyTip,
            title = "Privacy Policy",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://recoveryintimation.blogspot.com/2026/02/privacy-policy-for-recovery-intimation.html"))
                try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show() }
            }
        )
    }
}

@Composable
fun InfoActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// Helper Function to Start Service
fun startSmsService(context: Context, payload: SmsPayload) {
    try {
        val serviceIntent = Intent(context, SmsService::class.java).apply {
            putExtra("b_mobile", payload.bMobile)
            putExtra("b_msg", payload.bMsg)
            putStringArrayListExtra("g_mobiles", ArrayList(payload.gMobiles))
            putExtra("g_msg", payload.gMsg)
        }
        context.startService(serviceIntent)
        Toast.makeText(context, "Sending SMS in background...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to start service: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// --- COMPONENT: BANK SELECTION FIELD ---
@Composable
fun BankSelectionField(
    bankName: String,
    onBankSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Top Banks List
    val banks = listOf(
        "State Bank of India", "J&K Bank", "HDFC Bank", "ICICI Bank",
        "Punjab National Bank", "Axis Bank", "Canara Bank",
        "Bank of Baroda", "Union Bank", "Kotak Mahindra Bank", "IndusInd Bank"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = bankName,
            onValueChange = { onBankSelected(it) },
            label = { Text("Bank Name") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Rounded.ArrowDropDown, null)
                }
            },
            singleLine = true
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 250.dp)
        ) {
            banks.forEach { bank ->
                DropdownMenuItem(
                    text = { Text(bank) },
                    onClick = {
                        onBankSelected(bank)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- FULL ENTRY DIALOG ---
@Composable
fun FullEntryDialog(
    account: AccountEntity? = null,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    var accNo by remember { mutableStateOf(account?.accountNumber ?: "") }
    var name by remember { mutableStateOf(account?.name ?: "") }
    var mobile by remember { mutableStateOf(account?.mobileNumber ?: "") }
    var amount by remember { mutableStateOf(account?.amount ?: "") }

    var bankName by remember { mutableStateOf(account?.bankName ?: "J&K Bank") }

    var payByDate by remember { mutableStateOf(account?.payByDate ?: "") }

    var g1Name by remember { mutableStateOf(account?.g1Name ?: "") }
    var g1Mobile by remember { mutableStateOf(account?.g1Mobile ?: "") }
    var g2Name by remember { mutableStateOf(account?.g2Name ?: "") }
    var g2Mobile by remember { mutableStateOf(account?.g2Mobile ?: "") }

    var accError by remember { mutableStateOf(false) }
    var mobileError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // --- FIX: Using android.app.DatePickerDialog to avoid M3 Crash ---
    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, day -> onDateSelected("$day/${month + 1}/$year") },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp).wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp).heightIn(max = 650.dp)) {
                Text(if (account == null) "New Entry" else "Edit Details", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionTitle("Bank & Borrower")

                    BankSelectionField(bankName = bankName, onBankSelected = { bankName = it })

                    OutlinedTextField(
                        value = accNo,
                        onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() }) { accNo = it; accError = false } },
                        label = { Text("Account No (16 Digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = accError,
                        supportingText = { if (accError) Text("Must be exactly 16 digits", color = MaterialTheme.colorScheme.error) },
                        trailingIcon = { if (accNo.length == 16) Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50)) }
                    )

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) { mobile = it; mobileError = false } },
                        label = { Text("Mobile (10 Digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = mobileError,
                        supportingText = { if (mobileError) Text("Must be exactly 10 digits", color = MaterialTheme.colorScheme.error) },
                        trailingIcon = { if (mobile.length == 10) Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50)) }
                    )

                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                    SectionTitle("Due Date")
                    ReadOnlyDateField("Pay By Date (Due)", payByDate) { showDatePicker { payByDate = it } }

                    SectionTitle("Guarantors (Optional)")
                    OutlinedTextField(value = g1Name, onValueChange = { g1Name = it }, label = { Text("Guarantor 1 Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = g1Mobile, onValueChange = { if (it.length <= 10) g1Mobile = it }, label = { Text("G1 Mobile") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))

                    OutlinedTextField(value = g2Name, onValueChange = { g2Name = it }, label = { Text("Guarantor 2 Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = g2Mobile, onValueChange = { if (it.length <= 10) g2Mobile = it }, label = { Text("G2 Mobile") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        var isValid = true
                        if (accNo.length != 16) { accError = true; isValid = false }
                        if (mobile.length != 10) { mobileError = true; isValid = false }

                        if (isValid) {
                            val newEntry = AccountEntity(
                                id = account?.id ?: 0,
                                accountNumber = accNo, name = name, mobileNumber = mobile, amount = amount,
                                bankName = bankName,
                                payByDate = payByDate,
                                g1Name = g1Name, g1Mobile = g1Mobile, g2Name = g2Name, g2Mobile = g2Mobile
                            )
                            onSave(newEntry)
                        }
                    }) { Text("Save Record") }
                }
            }
        }
    }
}

// --- ACCOUNT ITEM ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountItem(
    account: AccountEntity,
    onSendClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.accountNumber,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${account.name} • ${account.bankName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Event, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Due: ${account.payByDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${account.amount}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                IconButton(
                    onClick = onSendClick,
                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.Send,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --- SMS CONFIRMATION DIALOG ---
@Composable
fun SmsConfirmationDialog(
    account: AccountEntity,
    onDismiss: () -> Unit,
    onSendConfirmed: (String, String, List<String>, String) -> Unit
) {
    val borrowerMobile = if (account.mobileNumber.length == 10) account.mobileNumber else ""
    val initialBorrowerMsg = remember { SmsUtils.generateBorrowerMessage(account) }
    var borrowerMsg by remember { mutableStateOf(initialBorrowerMsg) }

    val guarantorMobiles = remember {
        val list = mutableListOf<String>()
        if (account.g1Mobile.length == 10) list.add(account.g1Mobile)
        if (account.g2Mobile.length == 10) list.add(account.g2Mobile)
        list
    }
    val hasGuarantors = guarantorMobiles.isNotEmpty()
    val initialGuarantorMsg = remember { SmsUtils.generateGuarantorMessage(account) }
    var guarantorMsg by remember { mutableStateOf(initialGuarantorMsg) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Message, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Confirm SMS", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    if (borrowerMobile.isNotEmpty()) {
                        Text("To Borrower (${account.name})", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = borrowerMsg,
                            onValueChange = { borrowerMsg = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Text("No valid mobile number for Borrower.", color = MaterialTheme.colorScheme.error)
                    }

                    if (hasGuarantors) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(20.dp))

                        Text("To Guarantors (${guarantorMobiles.size})", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.error))
                        Text("Numbers: ${guarantorMobiles.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = guarantorMsg,
                            onValueChange = { guarantorMsg = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.error,
                                unfocusedBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSendConfirmed(borrowerMobile, borrowerMsg, guarantorMobiles, guarantorMsg)
                        },
                        enabled = borrowerMobile.isNotEmpty() || hasGuarantors
                    ) {
                        Icon(Icons.Rounded.Send, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send All")
                    }
                }
            }
        }
    }
}

@Composable
fun ReadOnlyDateField(label: String, value: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value, onValueChange = { }, label = { Text(label) },
            modifier = Modifier.fillMaxWidth(), enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
            ),
            trailingIcon = { Icon(Icons.Rounded.CalendarMonth, null) }
        )
        Box(modifier = Modifier.matchParentSize().clickable { onClick() })
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}