package alchemist.fit.uom.alchemists.activities;


import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;

import alchemist.fit.uom.alchemists.Constants;
import alchemist.fit.uom.alchemists.R;
import alchemist.fit.uom.alchemists.Utility;
import alchemist.fit.uom.alchemists.database.AlchemistsDataSource;


public class AddNearestCityActivity extends AppCompatActivity {

    private PlaceAutocompleteFragment placeAutocompleteFragment;
    private String selectedNearestCity;
    private AlchemistsDataSource alchemistsDataSource;
    private static final String MY_PREFS_NAME = "alchemist";
    private String sharedPrefEmailAddress;
    private String sharedPrefUserId;
    private ProgressDialog progressDialog;
    private Boolean isPlaceSelected=false;
    private String context_identification,user_history_identification,
            user_behaviour_identification,simulation;
    private String[] retrievedBehaviouralData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        alchemistsDataSource = new AlchemistsDataSource(this);
        alchemistsDataSource.open();


        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        sharedPrefEmailAddress = prefs.getString("email_address", "No name defined");
        sharedPrefUserId = prefs.getString("user_id", "No name defined");
        retrievedBehaviouralData = alchemistsDataSource.getAllDataFromBehaviourDetails(sharedPrefEmailAddress);

        if(retrievedBehaviouralData!=null) {
            if (!retrievedBehaviouralData[0].equals("NO EXIST")) {
                if (retrievedBehaviouralData[6] != null) {
                    context_identification = retrievedBehaviouralData[6];
                }
                if (retrievedBehaviouralData[7] != null) {
                    user_history_identification = retrievedBehaviouralData[7];
                }
                if (retrievedBehaviouralData[8] != null) {
                    user_behaviour_identification = retrievedBehaviouralData[8];
                }
                if (retrievedBehaviouralData[11] != null) {
                    simulation = retrievedBehaviouralData[11];
                }
            }
        }


        if(user_history_identification.equals("on")||simulation.equals("on")) {
            updateTheme();
        }
        setContentView(R.layout.activity_add_nearest_city);



        placeAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        AutocompleteFilter autocompleteFilter = new AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES).build();

        placeAutocompleteFragment.setFilter(autocompleteFilter);

        placeAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Toast.makeText(getApplicationContext(),place.getName().toString(),Toast.LENGTH_SHORT).show();
                selectedNearestCity = place.getName().toString();
                isPlaceSelected = true;
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(getApplicationContext(),status.toString(),Toast.LENGTH_SHORT).show();

            }
        });



    }
    public void confirmNearestCity(View view){
        if(isPlaceSelected==true){
            showProgressDialog();
            new AddNearestCityActivity.AddNearestCityAsyncTask().execute(Constants.localAddress +"rest/ureportservice/updateNearestCity?user_id="+URLEncoder.encode(sharedPrefUserId)+"&nearest_city="+URLEncoder.encode(selectedNearestCity));
        }else {
            Toast.makeText(this, "Please enter your desired location!", Toast.LENGTH_SHORT).show();
        }

    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(AddNearestCityActivity.this,R.style.MyAlertDialogStyle);
        progressDialog.setMessage("Update is processing!"); // Setting Message
        progressDialog.setTitle("UReport"); // Setting Title
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
        progressDialog.show(); // Display Progress Dialog
        progressDialog.setCancelable(false);
    }

    public static String GET(String url) {
        InputStream inputStream = null;
        String result = "";
        try {
            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();
            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();
            // convert inputstream to string
            if (inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }
        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
    }

    private class AddNearestCityAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            switch(result)
            {
                case "Updated":
                    isPlaceSelected=false;
                    finish();
                    Intent intent = new Intent(AddNearestCityActivity.this, EditProfileActivity.class);
                    alchemistsDataSource.updateUserDetails(null,selectedNearestCity,null,
                            sharedPrefEmailAddress,null,null,
                            null,null,null,null,null);
                    startActivity(intent);
                    break;
                case "404":
                    System.out.println("Something went wrong!");
                    break;
                default:
                    System.out.println("Something went wrong!");
            }
        }
    }

    @Override
    public void onBackPressed(){
        finish();
        Intent intent = new Intent(this, EditProfileActivity.class);
        startActivity(intent);
    }

    public void activityAddNearestCityCancel(View view){
        finish();
        Intent intent = new Intent(this, EditProfileActivity.class);
        startActivity(intent);
    }
    public void updateTheme() {
        if (Utility.getTheme(getApplicationContext()) == 1) {
            setTheme(R.style.AppThemeForDG1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg1_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 2) {
            setTheme(R.style.AppThemeForDG2);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg2_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 3) {
            setTheme(R.style.AppThemeForDG3);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg3_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 4) {
            setTheme(R.style.AppThemeForDG4);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg4_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 5) {
            setTheme(R.style.AppThemeForDG5);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg5_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 6) {
            setTheme(R.style.AppThemeForDG6);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg6_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 7) {
            setTheme(R.style.AppThemeForDG7);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg7_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 8) {
            setTheme(R.style.AppThemeForDG8);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg8_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 9) {
            setTheme(R.style.AppThemeForDG9);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg9_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 10) {
            setTheme(R.style.AppThemeForDG10);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg10_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 11) {
            setTheme(R.style.AppThemeForDG11);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg1_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 12) {
            setTheme(R.style.AppThemeForDG12);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg2_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 13) {
            setTheme(R.style.AppThemeForDG13);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg3_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 14) {
            setTheme(R.style.AppThemeForDG14);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg4_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 15) {
            setTheme(R.style.AppThemeForDG15);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg5_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 16) {
            setTheme(R.style.AppThemeForDG16);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg6_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 17) {
            setTheme(R.style.AppThemeForDG17);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg7_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 18) {
            setTheme(R.style.AppThemeForDG18);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg8_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 19) {
            setTheme(R.style.AppThemeForDG19);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg9_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 20) {
            setTheme(R.style.AppThemeForDG20);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.dg10_status_bar_background_color));
            }
        } else if (Utility.getTheme(getApplicationContext()) == 0) {
            setTheme(R.style.AppThemeDefault0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_background_color));
            }
        } else {
            setTheme(R.style.AppThemeDefault);
        }
    }


}
