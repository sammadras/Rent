package com.rent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RentTheme {
                RentApp()
            }
        }
    }
}

private val TAB_NAMES = listOf("Home", "Tenants", "Ledger")

@Composable
fun RentApp(vm: RentViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // New month? Build arrears and re-enable the paid buttons.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshMonth() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            vm.refreshMonth()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .aquaBackground()
            .systemBarsPadding()
    ) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> HomeTab(vm)
                1 -> TenantsTab(vm)
                else -> LedgerTab(vm)
            }
        }

        // Sheet-style tabs along the bottom, like Excel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFFE9E9E9), Color(0xFFBDBDBD))))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TAB_NAMES.forEachIndexed { index, name ->
                AquaButton(
                    text = name,
                    onClick = { tab = index },
                    modifier = Modifier.weight(1f),
                    tone = if (tab == index) AquaTone.Blue else AquaTone.Silver,
                    height = 40.dp
                )
            }
        }
    }
}
