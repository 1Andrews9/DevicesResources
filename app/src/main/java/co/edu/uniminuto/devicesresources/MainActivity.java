package co.edu.uniminuto.devicesresources;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private Activity activity;

    private TextView versionAndroid;
    private int versionSDK;
    private ProgressBar pbLevelBattery;
    private TextView tvLevelBattery;
    private IntentFilter batteryFilter;

    private TextView tvConexion;
    private ConnectivityManager conexion;
    private CameraManager cameraManager;
    private String cameraID;
    private Button onFlash;
    private Button offFlash;

    private Button btnSaveFile;
    private EditText nameFile;

    private BluetoothAdapter bluetoothAdapter;
    private TextView tvBluetoothStatus;
    private Button btnEnableBluetooth;
    private Button btnDisableBluetooth;
    private Button btnListDevices;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_BT = 2;
    private Button btnTakePhoto;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initObjects();

        btnTakePhoto.setOnClickListener(v -> {
            // Abre la actividad de la cámara
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        // Linterna
        onFlash.setOnClickListener(this::onLight);
        offFlash.setOnClickListener(this::offLight);

        // Batería
        batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(broReceiver, batteryFilter);

        // Bluetooth
        btnEnableBluetooth.setOnClickListener(v -> enableBluetooth());
        btnDisableBluetooth.setOnClickListener(v -> disableBluetooth());
        btnListDevices.setOnClickListener(v -> listPairedDevices());

        // Guardar archivo
        btnSaveFile.setOnClickListener(this::saveFile);
    }


    BroadcastReceiver broReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int levelBattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            pbLevelBattery.setProgress(levelBattery);
            tvLevelBattery.setText("levelBattery " + levelBattery + "%");
        }
    };

    private void onLight(View view) {
        try {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            cameraID = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraID, true);
        } catch (Exception e) {
            Log.i("Linterna", e.getMessage());
        }
    }

    private void offLight(View view) {
        try {
            cameraManager.setTorchMode(cameraID, false);
        } catch (Exception e) {
            Log.i("Linterna", e.getMessage());
        }
    }

    private void checkConnection() {
        try {
            conexion = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (conexion != null) {
                NetworkInfo networkInfo = conexion.getActiveNetworkInfo();
                boolean stateNet = networkInfo != null && networkInfo.isConnectedOrConnecting();
                if (stateNet) {
                    tvConexion.setText("State On");
                } else {
                    tvConexion.setText("State OFF or NO Info");
                }
            }
        } catch (Exception e) {
            Log.i("CONEXION", e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetooth();
            } else {
                Toast.makeText(this, "Permiso de Bluetooth denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableBluetooth() {
        if (bluetoothAdapter == null) {
            Log.i("Bluetooth", "El dispositivo no soporta Bluetooth");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSION_BT);
                    return;
                }
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_PERMISSION_BT);
                    return;
                }
            }

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            tvBluetoothStatus.setText("Bluetooth Activado");
        } else {
            tvBluetoothStatus.setText("Bluetooth ya está activado");
        }
    }

    private void disableBluetooth() {
        if (bluetoothAdapter == null) {
            Log.i("Bluetooth", "El dispositivo no soporta Bluetooth");
            return;
        }

        if (bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            tvBluetoothStatus.setText("Abriendo configuración para desactivar Bluetooth");
        } else {
            tvBluetoothStatus.setText("Bluetooth ya está desactivado");
        }
    }


    private void listPairedDevices() {
        if (bluetoothAdapter == null) {
            Log.i("Bluetooth", "El dispositivo no soporta Bluetooth");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSION_BT);
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<String> deviceList = new ArrayList<>();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceInfo = device.getName() + " - " + device.getAddress();
                deviceList.add(deviceInfo);
            }
        } else {
            deviceList.add("No hay dispositivos emparejados");
        }

        ListView lvPairedDevices = findViewById(R.id.lvPairedDevices);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        lvPairedDevices.setAdapter(adapter);
    }


    private void saveFile(View view) {
        String fileName = nameFile.getText().toString().trim();

        if (fileName.isEmpty()) {
            Log.i("FILE", "El nombre del archivo está vacío");
            return;
        }

        String studentName = "Nombre del estudiante: Andres Rativa";
        int batteryLevel = pbLevelBattery.getProgress();
        String androidVersion = Build.VERSION.RELEASE;

        String content = "Nombre del estudiante: " + studentName + "\n" +
                "Nivel de batería: " + batteryLevel + "%\n" +
                "Versión de Android: " + androidVersion;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName + ".txt");

        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK) {
            Uri uri = data.getData();

            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    String content = "Nombre del estudiante: Andres Rativa\n" +
                            "Nivel de batería: " + pbLevelBattery.getProgress() + "%\n" +
                            "Versión de Android: " + Build.VERSION.RELEASE;

                    outputStream.write(content.getBytes());
                    outputStream.close();
                    Log.i("FILE", "Archivo guardado correctamente");
                }
            } catch (IOException e) {
                Log.e("FILE", "Error al guardar el archivo", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Version de android
        String versionSO= Build.VERSION.RELEASE;
        versionSDK=Build.VERSION.SDK_INT;
        versionAndroid.setText("Versión SO:"+versionSO+" / SDK:"+versionSDK);
        checkConnection();
    }
    private void initObjects() {
        this.context = this;
        this.activity = this;
        this.versionAndroid = findViewById(R.id.tvVersionAndroid);
        this.pbLevelBattery = findViewById(R.id.pbLevelBattery);
        this.tvLevelBattery = findViewById(R.id.tvLevelBattery);
        this.tvConexion = findViewById(R.id.tvState);
        this.nameFile = findViewById(R.id.etNameFile);
        this.onFlash = findViewById(R.id.btnOn);
        this.offFlash = findViewById(R.id.btnOff);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        this.btnEnableBluetooth = findViewById(R.id.btnEnableBluetooth);
        this.btnDisableBluetooth = findViewById(R.id.btnDisableBluetooth);
        this.btnListDevices = findViewById(R.id.btnListDevices);
        this.btnSaveFile = findViewById(R.id.btnSaveFile);
        this.btnTakePhoto = findViewById(R.id.btnTakePhoto);
    }
}
