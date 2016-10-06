package itu.edu.embeddedlab.calmeasure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener, MedalsFragment.OnFragmentInteractionListener,
    SettingFragment.OnFragmentInteractionListener, PresentationFragment.OnFragmentInteractionListener{

    private List<Fragment> fragmentColection;
    private Fragment currentFragment;
    private static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(mBroadcastReceiver, makeIntentFilter());
        initFragment();
    }

    private void initFragment(){
        fragmentColection = new ArrayList<Fragment>();
        fragmentColection.add(MainFragment.newInstance("", ""));
        fragmentColection.add(MedalsFragment.newInstance("", ""));
        fragmentColection.add(PresentationFragment.newInstance("", ""));
        fragmentColection.add(SettingFragment.newInstance("", ""));
        onClickNavigateMain(null);
    }

    public void onClickNavigateMain(View view){
        changeFragment(0);

    }

    public void onClickNavigateSecond(View view){
        changeFragment(1);
    }

    public void onClickNavigateThird(View view){
        changeFragment(2);
    }

    public void onClickNavigateFouth(View view){
        changeFragment(3);
    }

    private void changeFragment(int index){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentColection.get(index).getClass().getName());
        if(fragment == null){
            fragment = fragmentColection.get(index);
        }else if(currentFragment == fragment){
            return;
        }
        if(currentFragment != null){
            ft.hide(currentFragment);
        }

        currentFragment = fragment;
        if(!currentFragment.isAdded()){
            ft.add(R.id.display_fragment, currentFragment, currentFragment.getClass().getName());
        }
        ft.show(currentFragment);
        ft.commit();
    }


    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.SERVICE_UPDATE_VIEW);
        return intentFilter;
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            Bundle bundle = intent.getExtras();
            if(bundle.containsKey(Constant.BROADCAST_BLE_CONNECT)){
                ((SettingFragment)fragmentColection.get(3)).updateDeviceStatus("connected");
                Log.e(TAG, "connect");
            }else if(bundle.containsKey(Constant.BROADCAST_BLE_DISCONNECT)){
                ((SettingFragment)fragmentColection.get(3)).updateDeviceStatus("disconnected");
                Log.e(TAG, "disconnect");
            }else if(bundle.containsKey(Constant.BROADCAST_AWS_HUMIDITY)){
                String number = bundle.getString(Constant.BROADCAST_AWS_HUMIDITY);
                ((MainFragment)fragmentColection.get(0)).updateHumidity(number);
                Log.e(TAG, "humidity is " + number);
            }else if(bundle.containsKey(Constant.BROADCAST_AWS_TEMPERATURE)){
                String number = bundle.getString(Constant.BROADCAST_AWS_TEMPERATURE);
                ((MainFragment)fragmentColection.get(0)).updateTemperature(number);
                Log.e(TAG, "temperature is " + number);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
