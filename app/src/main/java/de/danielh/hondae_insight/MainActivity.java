package de.danielh.hondae_insight;

import android.Manifest; // Import needed for permissions
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager; // Import needed for check
import android.os.Build; // Import needed for version check
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Helper for permissions
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class MainActivity extends AppCompatActivity {

    private MainActivityViewModel _viewModel;
    private SharedPreferences _preferences;
    private final static String DEVICE_NAME = "device_name";
    private final static String DEVICE_MAC = "device_mac";
    
    // Permission Launcher for Android 12+
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize the Permission Launcher
        // We register this BEFORE we use it. It handles the user's Yes/No response.
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission granted, proceed with startup
                runBluetoothStartup(savedInstanceState);
            } else {
                // Permission denied. Show an error and close.
                Toast.makeText(this, "Bluetooth permission is required to use this app.", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        _viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        
        // Setup Observers
        _viewModel.getToastMessage().observe(this, messageResId -> {
            if (messageResId != null) Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show();
        });
        _viewModel.getCloseActivity().observe(this, shouldClose -> {
            if (shouldClose != null && shouldClose) finish();
        });

        if (!_viewModel.setupViewModel()) {
            return;
        }

        _preferences = getPreferences(MODE_PRIVATE);

        // Setup UI (RecyclerView, etc.)
        RecyclerView deviceList = findViewById(R.id.main_devices);
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.main_swiperefresh);

        deviceList.setLayoutManager(new LinearLayoutManager(this));
        DeviceAdapter adapter = new DeviceAdapter();
        deviceList.setAdapter(adapter);

        _viewModel.getPairedDeviceList().observe(this, adapter::updateList);

        // Define refresh behavior, but check permission first!
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (hasBluetoothPermission()) {
                _viewModel.refreshPairedDevices();
            } else {
                // Re-request permission if they try to refresh without it
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
            }
            swipeRefreshLayout.setRefreshing(false);
        });

        // 2. Start the Logic Flow
        if (hasBluetoothPermission()) {
            // We have permission (or we are on Android < 12), go ahead.
            runBluetoothStartup(savedInstanceState);
        } else {
            // We are on Android 12+ and don't have permission yet. Ask for it.
            // This will trigger the popup, and the result is handled in the Launcher above.
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }
    }

    /**
     * Safe method to run operations that require Bluetooth permissions.
     */
    private void runBluetoothStartup(Bundle savedInstanceState) {
        // Refresh the list
        _viewModel.refreshPairedDevices();

        // Check for auto-connect
        if (savedInstanceState == null) {
            checkLastConnectedDevice();
        }
    }

    /**
     * Helper to check if we have the necessary permissions.
     * Returns true if on Android 11 or lower, or if Android 12+ permissions are granted.
     */
    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Permissions not required for API < 31
    }

    private void checkLastConnectedDevice() {
        String deviceName = _preferences.getString(DEVICE_NAME, null);
        String deviceMac = _preferences.getString(DEVICE_MAC, null);

        if (deviceName != null && deviceMac != null) {
            new Handler(Looper.getMainLooper()).post(() -> 
                openCommunicationsActivity(deviceName, deviceMac)
            );
        }
    }

    public void openCommunicationsActivity(String deviceName, String macAddress) {
        // Double check permission before launching (just to be safe)
        if (!hasBluetoothPermission()) return;

        final SharedPreferences.Editor editor = _preferences.edit();
        editor.putString(DEVICE_NAME, deviceName);
        editor.putString(DEVICE_MAC, macAddress);
        editor.apply();
        
        Intent intent = new Intent(this, CommunicateActivity.class);
        intent.putExtra("device_name", deviceName);
        intent.putExtra("device_mac", macAddress);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    // --- ViewHolder and Adapter ---
    private class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final RelativeLayout _layout;
        private final TextView _text1;
        private final TextView _text2;

        DeviceViewHolder(View view) {
            super(view);
            _layout = view.findViewById(R.id.list_item);
            _text1 = view.findViewById(R.id.list_item_text1);
            _text2 = view.findViewById(R.id.list_item_text2);
        }

        void setupView(BluetoothDevice device) {
            // NOTE: device.getName() requires permission on Android 12+
            // But since we checked permission before populating the list, this is usually safe.
            // SecurityException could still theoretically happen if permission is revoked while app is running.
            try {
                if (!hasBluetoothPermission()) {
                     _text1.setText("Permission Missing");
                     return;
                }
                String name = device.getName() != null ? device.getName() : "Unknown Device";
                _text1.setText(name);
                _text2.setText(device.getAddress());
                _layout.setOnClickListener(view -> openCommunicationsActivity(name, device.getAddress()));
            } catch (SecurityException e) {
                _text1.setText("Error");
            }
        }
    }

    class DeviceAdapter extends RecyclerView.Adapter<DeviceViewHolder> {
        private BluetoothDevice[] _deviceList = new BluetoothDevice[0];

        @NotNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            return new DeviceViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NotNull DeviceViewHolder holder, int position) {
            holder.setupView(_deviceList[position]);
        }

        @Override
        public int getItemCount() {
            return _deviceList.length;
        }

        void updateList(Collection<BluetoothDevice> deviceList) {
            this._deviceList = deviceList.toArray(new BluetoothDevice[0]);
            notifyDataSetChanged();
        }
    }
}
