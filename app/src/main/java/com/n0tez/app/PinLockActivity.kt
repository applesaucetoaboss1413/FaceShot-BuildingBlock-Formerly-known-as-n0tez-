package com.n0tez.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.n0tez.app.ui.components.AppScreen
import com.n0tez.app.ui.components.HeroPanel
import com.n0tez.app.ui.components.MetricChip
import com.n0tez.app.ui.theme.N0tezTheme

class PinLockActivity : AppCompatActivity() {

    private var isSettingPin = false
    private var firstPinEntry = ""
    private var pinValue by mutableStateOf(TextFieldValue(""))
    private var subtitleText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSettingPin = intent.getBooleanExtra("SET_PIN", false)
        subtitleText = if (isSettingPin) {
            "Enter a 4-digit PIN"
        } else {
            "Enter your 4-digit PIN to unlock"
        }

        setContent {
            N0tezTheme {
                PinLockScreen(
                    title = if (isSettingPin) "Set PIN" else "Enter PIN",
                    subtitle = subtitleText,
                    pinValue = pinValue,
                    onBack = ::finish,
                    onPinChange = { newValue ->
                        val filtered = newValue.text.filter(Char::isDigit).take(4)
                        pinValue = TextFieldValue(filtered)
                        if (filtered.length == 4) {
                            handlePinEntry(filtered)
                        }
                    },
                    onPrimaryAction = {
                        if (pinValue.text.length == 4) {
                            handlePinEntry(pinValue.text)
                        }
                    }
                )
            }
        }
    }

    private fun handlePinEntry(pin: String) {
        if (isSettingPin) {
            if (firstPinEntry.isEmpty()) {
                firstPinEntry = pin
                pinValue = TextFieldValue("")
                subtitleText = "Confirm your PIN"
                return
            }

            if (firstPinEntry == pin) {
                savePin(pin)
                setResult(RESULT_OK)
                Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                pinValue = TextFieldValue("")
                subtitleText = "PINs don't match. Try again."
                firstPinEntry = ""
            }
            return
        }

        if (verifyPin(pin)) {
            setResult(RESULT_OK)
            finish()
        } else {
            pinValue = TextFieldValue("")
            subtitleText = "Wrong PIN. Try again."
        }
    }

    private fun savePin(pin: String) {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "pin_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit().putString("user_pin", pin).apply()
        sharedPreferences.edit().putBoolean("pin_enabled", true).apply()
    }

    private fun verifyPin(pin: String): Boolean {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "pin_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val savedPin = sharedPreferences.getString("user_pin", "")
        return savedPin == pin
    }

    companion object {
        fun isPinEnabled(context: android.content.Context): Boolean {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    "pin_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                sharedPreferences.getBoolean("pin_enabled", false)
            } catch (_: Exception) {
                false
            }
        }
    }
}

@Composable
private fun PinLockScreen(
    title: String,
    subtitle: String,
    pinValue: TextFieldValue,
    onBack: () -> Unit,
    onPinChange: (TextFieldValue) -> Unit,
    onPrimaryAction: () -> Unit
) {
    AppScreen(
        title = title,
        subtitle = "Security gate",
        navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
        onNavigationClick = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroPanel(
                    eyebrow = "Security",
                    title = "A cleaner, more premium unlock flow.",
                    description = subtitle,
                    metrics = {
                        MetricChip("PIN type", "4 digits", Icons.Rounded.Lock)
                        MetricChip("Storage", "Encrypted", Icons.Rounded.Security)
                    },
                    footer = {
                        OutlinedTextField(
                            value = pinValue,
                            onValueChange = onPinChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            placeholder = { Text("••••") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null
                                )
                            }
                        )
                        Button(
                            onClick = onPrimaryAction,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = pinValue.text.length == 4
                        ) {
                            Text("Continue")
                        }
                        Text(
                            text = "PIN data stays in encrypted shared preferences on device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}
