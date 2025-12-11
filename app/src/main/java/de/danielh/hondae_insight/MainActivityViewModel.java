package de.danielh.hondae_insight;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.harrysoft.androidbluetoothserial.BluetoothManager;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MainActivityViewModel extends AndroidViewModel {

    // The shared Bluetooth Manager
    private BluetoothManager _bluetoothManager;

    // The paired devices list that the activity observes
    private final MutableLiveData<Collection<BluetoothDevice>> _pairedDeviceList = new MutableLiveData<>();

    // A LiveData for error messages (Strings) so the Activity can show the Toast
    private final MutableLiveData<Integer> _toastMessage = new MutableLiveData<>();
    // A navigation trigger (Boolean) to tell the Activity to close
    private final MutableLiveData<Boolean> _closeActivity = new MutableLiveData<>();

    private boolean _viewModelSetup = false;

    public MainActivityViewModel(@NotNull Application application) {
        super(application);
    }

    public boolean setupViewModel() {
        if (!_viewModelSetup) {
            _viewModelSetup = true;

            // Setup our BluetoothManager
            _bluetoothManager = BluetoothManager.getInstance();
            
            if (_bluetoothManager == null) {
                // Instead of showing Toast here, we post the resource ID to the LiveData
                _toastMessage.postValue(R.string.no_bluetooth);
                // Signal the activity to close
                _closeActivity.postValue(true);
                return false;
            }
        }
        return true;
    }

    public void refreshPairedDevices() {
        if (_bluetoothManager != null) {
            // Safety check: getPairedDevicesList() might return null or crash if BT is off
            Collection<BluetoothDevice> pairedDevices = _bluetoothManager.getPairedDevicesList();
            if (pairedDevices != null) {
                _pairedDeviceList.postValue(pairedDevices);
            } else {
                _pairedDeviceList.postValue(Collections.emptyList());
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // CRITICAL FIX: Removed _bluetoothManager.close().
        // Since BluetoothManager is a Singleton used by other activities,
        // closing it here would break the app when navigating away from Main.
    }

    // Getters for the Activity to observe
    public LiveData<Collection<BluetoothDevice>> getPairedDeviceList() { 
        return _pairedDeviceList; 
    }
    
    public LiveData<Integer> getToastMessage() { 
        return _toastMessage; 
    }
    
    public LiveData<Boolean> getCloseActivity() {
        return _closeActivity;
    }
}
