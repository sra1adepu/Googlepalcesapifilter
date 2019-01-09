package com.logicshore.locationfilterwithmap;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.HttpGet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,AdapterView.OnItemClickListener {

    private GoogleMap mMap;
    GPSTracker gpsTracker;
    Double latitude = 0.0 , longitude =0.0 ;
    AutoCompleteTextView search_filter;
    Geocoder geocoder;

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";
    private static final String API_KEY = "AIzaSyDbIlFUED76qD_2vzv8piTSAFKhmg3FQEs";
    double lat;
    double longt;
    String str;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String TAG = "MapActivity";
    List<Address> addresses;
    List<Address> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        gpsTracker=new GPSTracker(this);
        search_filter=(AutoCompleteTextView)findViewById(R.id.search_filter);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(gpsTracker.canGetLocation){
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();
            Log.d("Latitude",latitude.toString());
        }

        search_filter.setThreshold(1);
        search_filter.setAdapter(new GooglePlacesAutocompleteAdapter(MapsActivity.this, R.layout.list_item));
        search_filter.setOnItemClickListener(MapsActivity.this);


    }






    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude,longitude)).zoom(13).build();
        mMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));

    }



class GooglePlacesAutocompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private ArrayList<String> resultList;

        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        str = (String)parent.getItemAtPosition(position);

try{
            String url="https://maps.googleapis.com/maps/api/geocode/json?address="+ URLEncoder.encode(str, "UTF-8")+"&key=AIzaSyAg4V2JJesN5YoE4V0bUH6sJlY4AYeg2l4";
               // String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + URLEncoder.encode(str, "UTF-8") + "&sensor=true";
                new GenerateLatlong().execute(url);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

    }

    public static ArrayList<String> autocomplete(String input) {
        ArrayList<String> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&components=country:in");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());

            System.out.println("URL: " + url);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            // Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            //Log.e(LOG_TAG, "Error connecting to Places API", e);
            e.printStackTrace();
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {

            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<String>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                System.out.println("Description"+predsJsonArray.getJSONObject(i).getString("description"));
                System.out.println("============================================================");
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            // Log.e(LOG_TAG, "Cannot process JSON results", e);
            e.printStackTrace();
        }

        return resultList;
    }

    private class GenerateLatlong extends AsyncTask<String, String, String> {

        JSONObject jobj;
        private ProgressDialog progressDialog1;
        private String st;

        private String latlong;


        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            progressDialog1 = new ProgressDialog(MapsActivity.this);
            progressDialog1.setMessage("Please Wait.");
            progressDialog1.show();


            super.onPreExecute();
        }

        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub

            try {
//			String url="http://crimeproneareaservice.logicshore.co.in//LogicShore.svc/GetZoneslist";

                HttpClient httpclient = new DefaultHttpClient();
                HttpGet httppost = new HttpGet(params[0]);

                HttpResponse response = httpclient.execute(httppost);
                Log.d("http response", response.toString());
                st = EntityUtils.toString(response.getEntity());
                Log.d("HTTP status", st);

            } catch (UnknownHostException e) {

            } catch (Exception e) {
                e.printStackTrace();
            }
            return st;
        }

        @Override
        protected void onPostExecute(String st) {

            //   Log.d("main data",st);
            progressDialog1.dismiss();

            try {


                JSONObject jobj = new JSONObject(st);

                JSONArray jsarray = jobj.getJSONArray("results");

                JSONObject innerJsonObj = jsarray.getJSONObject(0);


                JSONObject geomertyobject = innerJsonObj.getJSONObject("geometry");
//
                JSONObject boundsobject = geomertyobject.getJSONObject("location");
//
                Log.d("JSONOBJECT",boundsobject.toString());
                // JSONObject ne= boundsobject.getJSONObject("northeast");
//
                lat = boundsobject.getDouble("lat");
                longt = boundsobject.getDouble("lng");

                LatLng current_postion = new LatLng(lat, longt);
               // moveCamera(new LatLng(lat, longt), DEFAULT_ZOOM);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_postion, DEFAULT_ZOOM));

                mMap.addMarker(new MarkerOptions().position(current_postion).title("Marker in Sydney"));
                



            } catch (Exception e) {
                Log.e("JSON EXCEPTION", "JSONException" + e);
                //Toast.makeText(getApplicationContext(), "Network Glitch Please try again later   jhgj", Toast.LENGTH_LONG).show();
                //Toast.makeText(getApplicationContext(), "No Internet Connection", 10).show();

            }


        }


    }





}
