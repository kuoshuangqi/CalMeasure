package itu.edu.embeddedlab.calmeasure;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.text.Text;

import android.Manifest;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

import itu.edu.embeddedlab.calorieCaculation.CalorieCaculation;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment implements LocationListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = MainFragment.class.getSimpleName();

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private GoogleMap mgoogleMap;
    private MapView mMapView;
    private ImageView speedImageView;
    private AnimationDrawable speedAnimationDrawble;
    private LocationManager locationManager;
    private int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TextView humidityTextView;
    private TextView temperatureTextView;
    private TextView totalCalorieTextView;
    private TextView caloriePassTimeTextView;
    private View startCalorieCountButton;
    private View resetCalorieCountButton;
    private View stopCalorieCountButton;


    private OnFragmentInteractionListener mListener;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
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
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main_v2, container, false);
        mMapView = (MapView) v.findViewById(R.id.fragment_main_mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();// needed to get the map to display immediately
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mgoogleMap = googleMap;
                mgoogleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker").snippet("snippet"));
                enableLocation();

//                mgoogleMapsetOnMyLocationChangeListener.();
            }
        });


        //set animation
        speedImageView = (ImageView) v.findViewById(R.id.main_fragment_speed_icon);
        speedImageView.setBackgroundColor(0xFFFFFF);
        speedImageView.setBackgroundResource(R.drawable.main_fragment_standing);

        //textview to be updated
        humidityTextView = (TextView)v.findViewById(R.id.main_fragment_humidity_text);
        temperatureTextView = (TextView)v.findViewById(R.id.main_fragment_temperature_text);

        //caculate calorie
        totalCalorieTextView = (TextView)v.findViewById(R.id.main_fragment_calories_count);
        caloriePassTimeTextView = (TextView)v.findViewById(R.id.main_activity_colaries_time);
        startCalorieCountButton = (RelativeLayout)v.findViewById(R.id.main_fragment_start_count_calorie);
        resetCalorieCountButton = (RelativeLayout)v.findViewById(R.id.main_fragment_rest_count_calorie);
        stopCalorieCountButton = (RelativeLayout)v.findViewById(R.id.main_fragment_stop_count_calorie);
        startCalorieCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "activity send out start count");
                Intent broadcast = new Intent(Constant.ACTIVITY_NOTIFY_SERVICE);
                Bundle bundle = new Bundle();
                bundle.putString(Constant.ACTIVITY_NOTIFY_START_COUNT, "");
                broadcast.putExtras(bundle);
                getActivity().sendBroadcast(broadcast);
            }
        });
        resetCalorieCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent broadcast = new Intent(Constant.ACTIVITY_NOTIFY_SERVICE);
                Bundle bundle = new Bundle();
                bundle.putString(Constant.ACTIVITY_NOTIFY_RESET_COUNT, "");
                broadcast.putExtras(bundle);
                getActivity().sendBroadcast(broadcast);
                updatetotalCalorieTextView("0");
                updatecaloriePassTimeTextView("00 : 00 : 00");
            }
        });

        stopCalorieCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent broadcast = new Intent(Constant.ACTIVITY_NOTIFY_SERVICE);
                Bundle bundle = new Bundle();
                bundle.putString(Constant.ACTIVITY_NOTIFY_STOP_COUNT, "");
                broadcast.putExtras(bundle);
                getActivity().sendBroadcast(broadcast);
            }
        });

        return v;
    }

    private void enableLocation() {
        //set location
        //provide permission
        if (ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            mgoogleMap.setMyLocationEnabled(true);
            locationManager = (LocationManager) MainFragment.this.getActivity().getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, true);
            Log.e(TAG, "we are ready to request update");
            locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, this);
//            Location myLocation = locationManager.getLastKnownLocation((provider));
//            mgoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//            double latitude = myLocation.getLatitude();
//            double longtitude = myLocation.getLongitude();
//            LatLng latLng = new LatLng(latitude, longtitude);
//            mgoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//            mgoogleMap.animateCamera(CameraUpdateFactory.zoomTo(14));
//            mgoogleMap.addMarker((new MarkerOptions().position(new LatLng(latitude, longtitude)).title("you are here").snippet("consider yourself")));
        } else {
            ActivityCompat.requestPermissions(this.getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        boolean isCOARSEPer = false;
        boolean isFINEPer = false;
        for (int i = 0; i < permissions.length; i++) {
            if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) {
                isCOARSEPer = true;
            }
            if(Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])){
                isFINEPer = true;
            }
        }
        if(isCOARSEPer && isFINEPer){
            enableLocation();
        }

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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onLocationChanged(Location location) {
//        Log.e(MainFragment.this.getClass().getSimpleName(), "we have received location changed");
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            Log.e(TAG, "permissioned");
//            locationManager.removeUpdates(this);
            mgoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            double latitude = location.getLatitude();
            double longtitude = location.getLongitude();
            LatLng latLng = new LatLng(latitude, longtitude);
            mgoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mgoogleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
            mgoogleMap.clear();
            mgoogleMap.addMarker((new MarkerOptions().position(new LatLng(latitude, longtitude)).title("you are here").snippet("consider yourself")));
            return;
        }

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

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

    public void updateTemperature(String number){
        temperatureTextView.setText(number);
    }

    public void updateHumidity(String number){
        humidityTextView.setText(number);
    }

    public void updateGeasture(String status){
        if(status.equals("sitting")){
            if(speedAnimationDrawble != null)
                speedAnimationDrawble.stop();
            speedImageView.setBackgroundColor(0xFFFFFF);
            speedImageView.setBackgroundResource(R.drawable.main_fragment_standing);
        }else if(status.equals("walking")){
            speedImageView.setBackgroundColor(0xFFFFFF);
            speedImageView.setBackgroundResource(R.drawable.main_fragment_walking_animation);
            speedAnimationDrawble = (AnimationDrawable) speedImageView.getBackground();
            speedAnimationDrawble.start();
        }else if(status.equals("running")){
            speedImageView.setBackgroundColor(0xFFFFFF);
            speedImageView.setBackgroundResource(R.drawable.main_fragment_running_animation);
            speedAnimationDrawble = (AnimationDrawable) speedImageView.getBackground();
            speedAnimationDrawble.start();
        }
    }

    public void updatetotalCalorieTextView(String number){
        totalCalorieTextView.setText(number);
    }

    public void updatecaloriePassTimeTextView(String number){
        caloriePassTimeTextView.setText(number);
    }
}
