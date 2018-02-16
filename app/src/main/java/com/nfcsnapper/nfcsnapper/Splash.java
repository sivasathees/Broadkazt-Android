package com.nfcsnapper.nfcsnapper;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nfcsnapper.nfcsnapper.api.ApiClient;
import com.nfcsnapper.nfcsnapper.api.ApiInterface;
import com.nfcsnapper.nfcsnapper.factory.NFCFactory;
import com.nfcsnapper.nfcsnapper.model.BaseRecord;
import com.nfcsnapper.nfcsnapper.model.Data;
import com.nfcsnapper.nfcsnapper.model.HerokuService;
import com.nfcsnapper.nfcsnapper.model.Model;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.nfcsnapper.nfcsnapper.R.id.textViewNFCid;


public class Splash extends AppCompatActivity {
    //CommonClass common;
    public static final int MY_PERMISSIONS_REQUEST_WRITE_FIELS = 102;
    AlertDialog dialog;
    ProgressBar progressBar;

    //From MainActivity
    public static final String EXTRA_MESSAGE = "URL";

    private NfcAdapter nfcAdpt;
    PendingIntent nfcPendingIntent;
    IntentFilter[] intentFiltersArray;

    private TextView textViewNFCid;
    private String nfcTagPayload;

    private ListView videoList;
    private ProgressBar spinner;
    private ArrayList<Model> data;
    private Context mCtx;

    private String nfcTagId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        progressBar = (ProgressBar)findViewById(R.id.progressbar);

        //From MainActivity *****************
        nfcAdpt = NfcAdapter.getDefaultAdapter(this);

        // Check if the smartphone has NFC
        if (nfcAdpt == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show();
            finish();
        }

        // Check if NFC is enabled
        if (!nfcAdpt.isEnabled()) {
            Toast.makeText(this, "Enable NFC before using the app", Toast.LENGTH_LONG).show();
        }

        Intent nfcIntent = new Intent(this, getClass());
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        nfcPendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, 0);

        // Create an Intent Filter limited to the URI or MIME type to
        // intercept TAG scans from.
        IntentFilter tagIntentFilter =
                new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            tagIntentFilter.addDataScheme("tel");
            intentFiltersArray = new IntentFilter[]{tagIntentFilter};
        } catch (Throwable t) {
            t.printStackTrace();
        }

        mCtx = this;

        //*****************************


        Thread background = new Thread() {
            public void run() {

                try {
                    // Thread will sleep for 5 seconds
                    sleep(2 * 1000);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        checkAppPermissions();

                    //if(nfcTagId.isEmpty()){
                        go_next();
                    //}


                    // After 5 seconds redirect to another intent

                } catch (Exception e) {

                }
            }
        };

        // start thread
        background.start();


    }
    public void checkAppPermissions(){

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.INTERNET)
                        != PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_NETWORK_STATE)
                        != PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.NFC)
                        != PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.INTERNET) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_NETWORK_STATE) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.NFC)) {
                //go_next();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.INTERNET,
                                Manifest.permission.ACCESS_NETWORK_STATE,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.CAMERA,
                                Manifest.permission.NFC
                        },
                        MY_PERMISSIONS_REQUEST_WRITE_FIELS);
            }
        }else{
            //go_next();
        }

        //go_next();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_WRITE_FIELS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                go_next();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(Splash.this);
                builder.setMessage("App required some permission please enable it")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                                openPermissionScreen();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                                dialog.dismiss();
                            }
                        });
                dialog = builder.show();
            }
            return;
        }
    }
    public  void go_next(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent;
        if(preferences.getBoolean("first open", true)) {
            intent = new Intent(Splash.this, Main2Activity.class);
        } else {

            intent = new Intent(Splash.this, Main2Activity.class);
            intent.putExtra("nfc", nfcTagId);
            //Send the NFC Tag ID
        }
        startActivity(intent);
        finish();

    }
    public void openPermissionScreen(){
        // startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", Splash.this.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if(dialog!=null){
            dialog.dismiss();
            dialog = null;
        }
        super.onDestroy();
    }

    //From MainActivity *********************
    @Override
    public void onNewIntent(Intent intent) {
        Log.d("*****Nfc", "ON NEW INTENT *******");
        getNFCTag(intent);
    }

    private void handleIntent(Intent i) {
        Log.d("*****Nfc", "HANDLE INTENT *******");
        getNFCTag(i);
    }

    @Override
    protected void onResume() {
        super.onResume();

        nfcAdpt.enableForegroundDispatch(
                this,
                // Intent that will be used to package the Tag Intent.
                nfcPendingIntent,
                // Array of Intent Filters used to declare the Intents you
                // wish to intercept.
                intentFiltersArray,
                // Array of Tag technologies you wish to handle.
                null);
        Log.d("*****Nfc", "nfcPendingIntent : " + nfcPendingIntent);
        Log.d("*****Nfc", "intentFiltersArray : " + intentFiltersArray);
        handleIntent(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdpt.disableForegroundDispatch(this);
    }

    private void getNFCTag(Intent i) {
        Log.d("*****Nfc", "getNFCTag starting");
        if (i == null){
            Log.d("Nfc", "**** Return null???");
            return;
        }

        String action = i.getAction();
        Log.d("Nfc", "**** action?" + action);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Log.d("Nfc", "Action NDEF Found");
            Parcelable[] parcs = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            for (Parcelable p : parcs) {
                NdefMessage msg = (NdefMessage) p;

                NdefRecord[] records = msg.getRecords();
                BaseRecord result = NFCFactory.createRecord(records[0]);
                nfcTagPayload = result.payload;

            }

            //String txt = (String)textViewNFCid.getText();
            //We get the tag OK.... 11.08.2017 - 08.20
            Log.d("Nfc****** ", "nfcTagPayload : " + nfcTagPayload);
            if(!nfcTagPayload.isEmpty()){// && !txt.contains(nfcTagPayload)){
                //callApiWithId(nfcTagPayload); //"BR1995N"
                //textViewNFCid.setText("Code: " + nfcTagPayload);
                nfcTagId = nfcTagPayload;
                //go_next();

            }
        }
    }



    //***********************
}
