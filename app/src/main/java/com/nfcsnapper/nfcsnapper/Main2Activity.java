package com.nfcsnapper.nfcsnapper;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.nfcsnapper.nfcsnapper.api.ApiClient;
import com.nfcsnapper.nfcsnapper.api.ApiInterface;
import com.nfcsnapper.nfcsnapper.helper.AppCommonUtils;
import com.nfcsnapper.nfcsnapper.model.Data;
import com.nfcsnapper.nfcsnapper.model.HerokuService;
import com.nfcsnapper.nfcsnapper.model.Video;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Main2Activity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int RC_BARCODE_CAPTURE = 9001;
    public static final String TAG = "URL";
    public static final int NFC_CAPTURE = 9002;

    Fragment fragment = null;
    Class fragmentClass;
    NavigationView navigationView;




    ProgressBar bar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if (!AppCommonUtils.getInstance(Main2Activity.this).isOnline(Main2Activity.this)){
            AppCommonUtils.getInstance(Main2Activity.this).showNoConnectionDialog(Main2Activity.this);
        }
        //ImageView image = (ImageView)findViewById(R.id.imageView3);
        //image.setVisibility(View.VISIBLE);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        fragmentClass = FirstFragment.class;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();



        String nfcTagId = getIntent().getStringExtra("nfc");
//        Log.d(TAG, "********NFC TAG IN MAINACTIVITY2 : " + nfcTagId);
//        if(nfcTagId != null && nfcTagId != ""){
//            callApiWithId(nfcTagId);
//        }
//        else {
//            Toast.makeText(this, "No content found for this tag", Toast.LENGTH_SHORT).show();
//        }


    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_qr_code:
                openBarcodeCapture();
                return false;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Fragment fragment = null;
        Class fragmentClass = null;

        int id = item.getItemId();

        if (id == R.id.about) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        } else if (id == R.id.language) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        }

        return true;
    }

    public void showProgress()
    {
        ((FirstFragment)fragment).showProgressBar();
    }
    public void hideProgress()
    {
        ((FirstFragment)fragment).hideProgressBar();

    }

    private void openBarcodeCapture() {
        Intent intent = new Intent(Main2Activity.this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);
        intent.putExtra(BarcodeCaptureActivity.AutoCapture, true);
        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);

                    //when success to read barcode
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    drawer.closeDrawer(GravityCompat.START);
                    showProgress();
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                    if (AppCommonUtils.getInstance(Main2Activity.this).isOnline(Main2Activity.this)){
                        callApiWithId(barcode.displayValue);
                    }
                    else {
                        AppCommonUtils.getInstance(Main2Activity.this).showNoConnectionDialog(Main2Activity.this);
                    }
                } else {
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {

            }
        }
        else {
            Log.d(TAG, "*****Before: super.onActivityResult(requestCode, resultCode, data)");
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void callApiWithId(String id){
        Log.d(TAG, "*****call API with ID: " + id);
        ApiInterface apiService =
                ApiClient.getClient().create(ApiInterface.class);

        Call<HerokuService> call = apiService.getVideos(id);

        call.enqueue(new Callback<HerokuService>() {
            ArrayList<Video> videos = new ArrayList<Video>();;

            @Override
            public void onResponse(Call<HerokuService>call, Response<HerokuService> response) {


                if(response != null && response.body() != null){
                    videos.clear();
                    Data d = response.body().getData();
                    videos = d.getVideos();

                    //mAdapter.notifyDataSetChanged();

                    Log.e(TAG, videos.toString());

                    Bundle bundle = new Bundle();
                    bundle.putSerializable("videos", videos);
                    bundle.putSerializable("asset",d.getAsset());
                    bundle.putInt("position", 0);
                    bundle.putString("logoUrl", d.getLogo());

                    FragmentTransaction ft = Main2Activity.this.getSupportFragmentManager().beginTransaction();
                    SlideshowDialogFragment newFragment = SlideshowDialogFragment.newInstance();
                    newFragment.first = fragment;
                    newFragment.setArguments(bundle);

                    newFragment.show(ft, "slideshow");

                }else{
                    showNoContentDialog();
                }
                hideProgress();
            }

            @Override
            public void onFailure(Call<HerokuService>call, Throwable t) {
                // Log error here since request failed
                Log.e(TAG, t.toString());
                //spinner.setVisibility(View.GONE);
                //pDialog.hide();
                Toast.makeText(Main2Activity.this,
                        t.toString(),
                        Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    private void showNoContentDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set title
        alertDialogBuilder.setTitle("Alert!");

        // set dialog message
        alertDialogBuilder
                .setMessage("No content found for this tag")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        openBarcodeCapture();

                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

}
