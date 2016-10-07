/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package itu.edu.embeddedlab.calmeasure;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import itu.edu.embeddedlab.swiftforestjava.Classifier;
import itu.edu.embeddedlab.swiftforestjava.DataSeriseProcesser;
import itu.edu.embeddedlab.swiftforestjava.Instance;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private final static UUID GENERIC_ATTRIBUTE_SERVICE = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    private final static UUID SERVICE_CHANGED_CHARACTERISTIC = UUID.fromString("00002A05-0000-1000-8000-00805f9b34fb");

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter adapter;
    private BluetoothDevice device;
    private String mDeviceName;
    private BluetoothGatt mBluetoothGatt;
    private static final UUID MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    public final static UUID SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");

    private int pushCount = 1;
    private List<List<Long>> acceleratorData;

//    private BluetoothGattCharacteristic mCharacteristic;
    private List<BluetoothGattCharacteristic> mCharacteristic = new ArrayList<BluetoothGattCharacteristic>();


    //we have finish the bluetooth part, now send the mqtt message
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
//    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a33blzdhx25gtp.iot.us-west-2.amazonaws.com";
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "abskiqkcu15ye.iot.us-east-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
//    private static final String COGNITO_POOL_ID = "us-west-2:b43e375d-1385-42e6-b3ad-1a38abe58bb2";
    private static final String COGNITO_POOL_ID = "us-east-1:03400a4c-91b6-47d1-b7f7-74b87a473df7";
    // Name of the AWS IoT policy to attach to a newly created certificate
//    private static final String AWS_IOT_POLICY_NAME = "GaryPolicy1";
    private static final String AWS_IOT_POLICY_NAME = "androidiotpolicy";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_1;
//    private static final Regions MY_REGION = Regions.US_EAST_1;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;

    KeyStore clientKeyStore = null;
    String certificateId;

    CognitoCachingCredentialsProvider credentialsProvider;

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    //broadcast the sample information
    public void onSampleValueReceived(final int value) {
        final Intent broadcast = new Intent(Constant.BROADCAST_EXAMPLE_MESSAGE);
        broadcast.putExtra(Constant.BROADCAST_EXAMPLE_MESSAGE_EXTRA_DATA, value);
        sendBroadcast(broadcast);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String mDeviceAddress = intent.getStringExtra(Constant.EXTRAS_DEVICE_ADDRESS);
        Log.e(TAG, "we get the message in the service that the device address is " + mDeviceAddress);

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();
        device = adapter.getRemoteDevice(mDeviceAddress);
        mDeviceName = device.getName();
        Log.e(TAG, "we get the device name in the service the name is " + mDeviceName);

        connect(device);

        return 3;
    }

    //the example nordic give use the templateManager to initial, exact it here
    //the templateManager just implement the BleManagerGattCallBack, which seems unnessary for now
    public void connect(BluetoothDevice device){
        if(mBluetoothGatt != null){
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        Log.e(TAG, "try to connect");
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG, "onConnectionStateChange " + status + " " + newState);

            if(newState == 2){
                //means it's connected;
                mBluetoothGatt.discoverServices();
                Bundle bundle = new Bundle();
                bundle.putInt(Constant.BROADCAST_BLE_CONNECT, 1);
                updateUIStatus(bundle);
            }else if(newState == 0){
                //means it's disconnected;
                Bundle bundle = new Bundle();
                bundle.putInt(Constant.BROADCAST_BLE_DISCONNECT, 1);
                updateUIStatus(bundle);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e(TAG, "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                BluetoothGattService service = gatt.getService(SERVICE_UUID);
//                mCharacteristic = service.getCharacteristic(MEASUREMENT_CHARACTERISTIC_UUID);
                BluetoothGattService temperatureService = gatt.getService(UUID.fromString("739298B6-87B6-4984-A5DC-BDC18B068985"));
                mCharacteristic.add(temperatureService.getCharacteristic(UUID.fromString("33EF9113-3B55-413E-B553-FEA1EAADA459")));

                if (ensureServiceChangedEnabled(gatt))
                    return;
                for(int i = 0 ; i < mCharacteristic.size(); i ++){
                    enableNotifications(mCharacteristic.get(i));
                }

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.e(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//            Log.e(TAG, "onCharacteristicChanged");
            onCharacteristicNotified(gatt, characteristic);
        }
    };

    protected void onCharacteristicNotified(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        // TODO this method is called when a notification has been received
        // This method may be removed from this class if not required
        byte[] value = characteristic.getValue();
//        Log.e(TAG, "the value is length" + value.length + "timestamp " + System.currentTimeMillis() );
        pushCount++;
        if(pushCount > 20){
            pushCount = 0;
        }
        int maskField = value[0];
        if(value.length == 7 && pushCount %2 ==0 ){
            //change the humidity
            int offset = 1;
            if(SensorDataParser.humidityHasChanged(maskField) ){
                float data = SensorDataParser.getHumidityPercent(value, offset);
                offset += SensorDataParser.SENSOR_HUMD_DATA_SIZE;
                publish("humidity", data);
//                Log.e(TAG, "humidity is " + data);
                // the value is data;
            }
            if ( SensorDataParser.pressureHasChanged(maskField)) {
                float data = SensorDataParser.getPressureMBar(value, offset);
                offset += SensorDataParser.SENSOR_PRES_DATA_SIZE;
            }

            if (SensorDataParser.temperatureHasChanged(maskField)) {
                float data = SensorDataParser.getTemperatureC(value, offset);
                offset += SensorDataParser.SENSOR_TEMP_DATA_SIZE;
                publish("temperature", data);
//                Log.e(TAG, "temperature is " + data);
            }
        }
        if(value.length == 19){
//            Log.e(TAG, "we get the acc infor");
            int offset = 1;
            int[] accData = new int[3];
            String[] attrs = new String[]{"accx", "accy", "accz"};
            if (SensorDataParser.accelerometerHasChanged(maskField) ) {
                SensorDataParser.getAccelorometerData(value, offset, accData);
                List<Long> data = new ArrayList<Long>();
                data.add((long)accData[0]);
                data.add((long)accData[1]);
                data.add((long)accData[2]);
                data.add(System.currentTimeMillis());
                acceleratorData.add(data);
                if(acceleratorData.size() == 80){
                    //send the accelerator data to analysis the status
                    Log.e(TAG, "start to analysis data");
                    InputStream inputStream = null;
                    try {
                        Instance features= DataSeriseProcesser.convertXYZtoInstance(acceleratorData);
                        inputStream = getResources().getAssets().open("savedForest.out");
                        ObjectInputStream in = new ObjectInputStream(inputStream);
                        Classifier forest=(Classifier)in.readObject();
                        Object resulttype = forest.classify(features);
                        Log.e(TAG, "the gesture type is " + resulttype.toString());
                        Bundle bundle = new Bundle();
                        bundle.putString(Constant.BROADCAST_AWS_GUESTRUE, resulttype.toString());
                        updateUIStatus(bundle);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }finally {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    acceleratorData.clear();
                }
//                Log.e(TAG, "x = " + accData[0] + " y = " + accData[1] + " z = " + accData[2] + " time " + System.currentTimeMillis());
//                    publish(attrs,accData);
            }
        }
    }


    private boolean ensureServiceChangedEnabled(final BluetoothGatt gatt) {
        if (gatt == null)
            return false;

        // The Service Changed indications have sense only on bonded devices
        final BluetoothDevice device = gatt.getDevice();
        if (device.getBondState() != BluetoothDevice.BOND_BONDED)
            return false;

        final BluetoothGattService gaService = gatt.getService(GENERIC_ATTRIBUTE_SERVICE);
        if (gaService == null)
            return false;

        final BluetoothGattCharacteristic scCharacteristic = gaService.getCharacteristic(SERVICE_CHANGED_CHARACTERISTIC);
        if (scCharacteristic == null)
            return false;

        Log.i(TAG, "Service Changed characteristic found on a bonded device");
        return enableIndications(scCharacteristic);
    }

    protected final boolean enableIndications(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0)
            return false;

        Log.d(TAG, "gatt.setCharacteristicNotification(" + characteristic.getUuid() + ", true)");
        gatt.setCharacteristicNotification(characteristic, true);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            Log.v(TAG, "Enabling indications for " + characteristic.getUuid());
            Log.d(TAG, "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID + ", value=0x02-00)");
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    protected final boolean enableNotifications(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
            return false;

        Log.d(TAG, "gatt.setCharacteristicNotification(" + characteristic.getUuid() + ", true)");
        gatt.setCharacteristicNotification(characteristic, true);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.v(TAG, "Enabling notifications for " + characteristic.getUuid());
            Log.d(TAG, "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID + ", value=0x01-00)");
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }
    //mqtt

    @Override
    public void onCreate() {
        super.onCreate();

        clientId = UUID.randomUUID().toString();
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                } else {
                    Log.i(TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                    } catch (Exception e) {
                        Log.e(TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        mqttTryConnect();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final String topic = "$aws/things/androidIotSensor/shadow/update/accepted";
//        final String topic = "$aws/things/Gary1/shadow/update/accepted";

        Log.d(TAG, "topic = " + topic);

        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            try {
                                String message = new String(data, "UTF-8");
//                                Log.e(TAG, "we receive the msg from subscribe "+ message);
                                if(message.contains("humidity")){
                                    String my = message.substring(message.indexOf("humidity") + 10, message.indexOf("}", message.indexOf("humidity") ) );
                                    Bundle bundle = new Bundle();
                                    bundle.putString(Constant.BROADCAST_AWS_HUMIDITY, my);
                                    updateUIStatus(bundle);
//                                    Log.e(TAG, "we recieve humidity of " + my);
                                }else if(message.contains("temperature")){
                                    String my = message.substring(message.indexOf("temperature") + 13, message.indexOf("}", message.indexOf("temperature") ) );
                                    Bundle bundle = new Bundle();
                                    bundle.putString(Constant.BROADCAST_AWS_TEMPERATURE, my);
                                    updateUIStatus(bundle);
//                                    Log.e(TAG, "we recieve temperature of " + my);
                                }
//                                String my = message.substring(message.indexOf("temp") + 6 , message.indexOf("temp") + 9);
//                                Log.e(TAG, "exact the message of " + my);
//                                onSampleValueReceived(Integer.parseInt(my.trim()));
                                // notify the activity
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Subscription error.", e);
        }

        acceleratorData = new ArrayList<List<Long>>();

    }

    public void mqttTryConnect(){
        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(TAG, "Status = " + String.valueOf(status));
                }
            });
        } catch (final Exception e) {
            Log.e(TAG, "Connection error.", e);
        }
    }

    public void publish(int number){
        final String topic = "$aws/things/androidIotSensor/shadow/update";
//        final String topic = "$aws/things/Gary1/shadow/update";
        final String msg = "{\"state\":{\"reported\":{\"temp\":"+ number +"}}}";

        try {
            mqttTryConnect();
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(TAG, "Publish error.", e);
        }
    }

    public void publish(String attr, float number){
        String topic = "$aws/things/androidIotSensor/shadow/update";
//        String topic = "$aws/things/Gary1/shadow/update";
        String msg = "{\"state\":{\"reported\":{\"" + attr + "\":"+ number +"}}}";
//        Log.e(TAG, msg);
        try {
            mqttTryConnect();
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(TAG, "Publish error.", e);
        }
    }

    public void publish(String[] attrs, int[] datas){
        String topic = "$aws/things/androidIotSensor/shadow/update";
//        String topic = "$aws/things/Gary1/shadow/update";
        StringBuilder pushData = new StringBuilder();
        if(attrs.length != datas.length){
            return;
        }
        for(int i = 0 ; i < attrs.length; i ++){
            pushData.append("\"" + attrs[i] + "\":" + datas[i] + ",");
        }
        String msg = "{\"state\":{\"reported\":{" + pushData.toString().substring(0, pushData.toString().length() - 1) + "}}}";
//        Log.e(TAG, msg);
//        String msg = "{\"state\":{\"reported\":{\"" + attr + "\":"+ number +"}}}";

        try {
            mqttTryConnect();
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(TAG, "Publish error.", e);
        }
    }

    private void updateUIStatus(Bundle args){
        final Intent broadcast = new Intent(Constant.SERVICE_UPDATE_VIEW);
        broadcast.putExtras(args);
        sendBroadcast(broadcast);
    }
}
