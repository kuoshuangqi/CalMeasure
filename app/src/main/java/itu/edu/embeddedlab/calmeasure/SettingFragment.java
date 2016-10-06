package itu.edu.embeddedlab.calmeasure;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SettingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SettingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingFragment extends Fragment {

    private ParcelUuid mUuid;
    public final static UUID SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    public BluetoothAdapter mBluetoothAdapter;
    public final Handler mHandler = new Handler();
    private boolean mIsScanning = false;
    private final static long SCAN_DURATION = 5000;
    private ListView listview;
    private DeviceListAdapter mAdapter;
    private Button scanButton;
    private TextView deviceStatusText;

    private final static String TAG = SettingFragment.class.getSimpleName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public SettingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingFragment newInstance(String param1, String param2) {
        SettingFragment fragment = new SettingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mUuid = new ParcelUuid(SERVICE_UUID);
        final BluetoothManager manager = (BluetoothManager)this.getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_setting, container, false);
        listview = (ListView)v.findViewById(R.id.device_list_fragment_setting);
        listview.setAdapter(mAdapter = new DeviceListAdapter(this.getActivity()));

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                stopScan();
                ExtendedBluetoothDevice d = (ExtendedBluetoothDevice)mAdapter.getItem(i);
                if(d == null) return;
                Log.e(TAG, d.device.getName());
                Log.e(TAG, d.device.getAddress());
                Intent gattServiceIntent = new Intent(SettingFragment.this.getActivity(), BluetoothLeService.class);
                gattServiceIntent.putExtra(Constant.EXTRAS_DEVICE_ADDRESS, d.device.getAddress());
                SettingFragment.this.getActivity().startService(gattServiceIntent);
                SettingFragment.this.getActivity().bindService(gattServiceIntent, mServiceConnection, 0);
            }
        });

        addBondedDevices();
        scanButton = (Button)v.findViewById(R.id.button_scan_for_device_fragment_setting);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanForDevice(view);
            }
        });
        deviceStatusText = (TextView)v.findViewById(R.id.text_current_device_fragment_setting);
        return v;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getActivity().unbindService(mServiceConnection);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void stopScan() {
        if (mIsScanning) {

            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(scanCallback);

            mIsScanning = false;
        }
    }

    public void scanForDevice(View view){
        System.out.println("start to scan");
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000).setUseHardwareBatchingIfSupported(false).build();
        final List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(mUuid).build());
        scanner.startScan(filters, settings, scanCallback);

        mIsScanning = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsScanning) {
                    stopScan();
                }
            }
        }, SCAN_DURATION);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            // do nothing
            System.out.println("start to onScanResult");
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            System.out.println("start to onBatchScanResults");
            for(final ScanResult result : results){
                System.out.println("device name is "+ result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : "null");
                System.out.println("device rssi is " + result.getRssi());
            }
            mAdapter.update(results);
        }

        @Override
        public void onScanFailed(final int errorCode) {
            // should never be called
            System.out.println("start to onScanFailed");
        }
    };

    private void addBondedDevices() {
        if(mBluetoothAdapter.getBondedDevices() != null) {
            final Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
            mAdapter.addBondedDevices(devices);
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.e(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
//            mBluetoothLeService = null;
            Log.e(TAG, "onServiceDisconnected");
        }
    };

    public void updateDeviceStatus(String status){
        deviceStatusText.setText(status);
    }
}
