package itu.edu.embeddedlab.calmeasure;

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
    SettingFragment.OnFragmentInteractionListener, StatusFragment.OnFragmentInteractionListener{

    private List<Fragment> fragmentColection;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initFragment();
    }

    private void initFragment(){
        fragmentColection = new ArrayList<Fragment>();
        fragmentColection.add(new MainFragment());
        fragmentColection.add(new MedalsFragment());
        fragmentColection.add(new StatusFragment());
        fragmentColection.add(new SettingFragment());
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

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
