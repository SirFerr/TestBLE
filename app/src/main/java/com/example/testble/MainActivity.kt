package com.example.testble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.testble.ui.theme.TestBLETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestBLETheme() {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DeviceListScreen()
                }
            }
        }
    }
}

@Composable
fun DeviceListScreen() {
    val context = LocalContext.current
    var devices by remember { mutableStateOf(listOf<BluetoothDevice>()) }

    RequestPermissions()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(Modifier.weight(10f)) {
            items(devices.distinctBy { it.address }) { device ->
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@items
                }
                Text(text = "${device.name} - ${device.address}")
            }
        }

        Row {
            Button(onClick = {
                devices = listOf()
                startBleScan(context = context) { device ->
                    devices = devices + device
                }
            }) {
                Text(text = "Scan BLE")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(onClick = {
                devices = listOf()
                startBtScan(context = context) { device ->
                    devices = devices + device
                }
            }) {
                Text(text = "Scan BT")
            }
        }
    }
}

@Composable
fun RequestPermissions() {
    val permissionList =
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ){}

    LaunchedEffect(key1 = true) {
        multiplePermissionsLauncher.launch(permissionList.toTypedArray())
    }
}

fun startBleScan(context: Context, onDeviceFound: (BluetoothDevice) -> Unit) {
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            onDeviceFound(device)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE", "$errorCode")
        }
    }

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    bluetoothLeScanner?.startScan(scanCallback)

    Handler(Looper.getMainLooper()).postDelayed({
        bluetoothLeScanner?.stopScan(scanCallback)
    }, 10000)
}

fun startBtScan(context: Context, onDeviceFound: (BluetoothDevice) -> Unit) {
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
        return
    }

    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
    pairedDevices?.forEach { device ->
        onDeviceFound(device)
    }

    val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let { onDeviceFound(it) }
                }
            }
        }
    }

    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
    context.registerReceiver(discoveryReceiver, filter)

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    bluetoothAdapter.startDiscovery()

    Handler(Looper.getMainLooper()).postDelayed({
        bluetoothAdapter.cancelDiscovery()
        context.unregisterReceiver(discoveryReceiver)
    }, 10000)
}


