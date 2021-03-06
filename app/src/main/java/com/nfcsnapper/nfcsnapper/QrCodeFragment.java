package com.nfcsnapper.nfcsnapper;


import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.nfcsnapper.nfcsnapper.adapter.GalleryAdapter;
import com.nfcsnapper.nfcsnapper.api.ApiClient;
import com.nfcsnapper.nfcsnapper.api.ApiInterface;
import com.nfcsnapper.nfcsnapper.factory.NFCFactory;
import com.nfcsnapper.nfcsnapper.model.BaseRecord;
import com.nfcsnapper.nfcsnapper.model.Data;
import com.nfcsnapper.nfcsnapper.model.HerokuService;
import com.nfcsnapper.nfcsnapper.model.Model;
import com.nfcsnapper.nfcsnapper.model.Video;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class QrCodeFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = "Logging MainActivity";
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

    private ArrayList<Video> videos;
    private ProgressDialog pDialog;
    private GalleryAdapter mAdapter;
    private RecyclerView recyclerView;

    private static final int RC_BARCODE_CAPTURE = 9001;

    public QrCodeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_qr_code, container, false);

        view.findViewById(R.id.read_barcode).setOnClickListener(this);


        textViewNFCid = (TextView) view.findViewById(R.id.textViewNFCid);
        spinner = (ProgressBar) view.findViewById(R.id.progressBar);

        nfcAdpt = NfcAdapter.getDefaultAdapter(this.getActivity());

        // Check if the smartphone has NFC
        if (nfcAdpt == null) {
            Toast.makeText(this.getActivity(), "NFC not supported", Toast.LENGTH_LONG).show();
            this.getActivity().finish();
        }

        // Check if NFC is enabled
        if (!nfcAdpt.isEnabled()) {
            Toast.makeText(this.getActivity(), "Enable NFC before using the app", Toast.LENGTH_LONG).show();
        }

        Intent nfcIntent = new Intent(this.getActivity(), getClass());
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        nfcPendingIntent = PendingIntent.getActivity(this.getActivity(), 0, nfcIntent, 0);

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

        mCtx = this.getContext();

        videoList = (ListView) view.findViewById(R.id.listViewVideos);

        videoList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(data.get(position).getAWSUrl() != null){

                    Intent intent = new Intent(mCtx, VideoViewActivity.class);

                    intent.putExtra(EXTRA_MESSAGE, data.get(position).getAWSUrl());
                    startActivity(intent);

                }else{
                    Toast.makeText(QrCodeFragment.this.getActivity(),
                            "No Url available",
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        pDialog = new ProgressDialog(this.getActivity());
        videos = new ArrayList<>();
        mAdapter = new GalleryAdapter(this.getActivity().getApplicationContext(), videos);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this.getActivity().getApplicationContext(), 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new GalleryAdapter.RecyclerTouchListener(this.getActivity().getApplicationContext(), recyclerView, new GalleryAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        //show scanner
        Intent intent = new Intent(this.getActivity(), BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);
        intent.putExtra(BarcodeCaptureActivity.AutoCapture, true);
        startActivityForResult(intent, RC_BARCODE_CAPTURE);

        return view;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.read_barcode) {
            // launch barcode activity.
            Intent intent = new Intent(this.getActivity(), BarcodeCaptureActivity.class);
            intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
            intent.putExtra(BarcodeCaptureActivity.UseFlash, false);
            intent.putExtra(BarcodeCaptureActivity.AutoCapture, true);
            startActivityForResult(intent, RC_BARCODE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);

                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                    callApiWithId(barcode.displayValue);
                } else {
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {

            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void callApiWithId(String id){

        ApiInterface apiService =
                ApiClient.getClient().create(ApiInterface.class);

        Call<HerokuService> call = apiService.getVideos(id);

        call.enqueue(new Callback<HerokuService>() {
            @Override
            public void onResponse(Call<HerokuService>call, Response<HerokuService> response) {
                if(response != null && response.body() != null){
                    Data d = response.body().getData();
                    videos = d.getVideos();

                    mAdapter.notifyDataSetChanged();

                    Log.e(TAG, videos.toString());

                    Bundle bundle = new Bundle();
                    bundle.putSerializable("videos", videos);
                    bundle.putInt("position", 0);

                    FragmentTransaction ft = QrCodeFragment.this.getActivity().getSupportFragmentManager().beginTransaction();
                    SlideshowDialogFragment newFragment = SlideshowDialogFragment.newInstance();
                    newFragment.setArguments(bundle);

                    newFragment.show(ft, "slideshow");

                    spinner.setVisibility(View.GONE);
                    textViewNFCid.setText("");
                }else{
                    Toast.makeText(QrCodeFragment.this.getActivity(),
                            "No Videos for the Scanned Tag",
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<HerokuService>call, Throwable t) {
                // Log error here since request failed
                Log.e(TAG, t.toString());
                //spinner.setVisibility(View.GONE);
                pDialog.hide();
                Toast.makeText(QrCodeFragment.this.getActivity(),
                        t.toString(),
                        Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    /*
    @Override
    public void onBackPressed() {


        if (getFragmentManager().getBackStackEntryCount() == 0) {
            //this.finish();
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce=false;
                }
            }, 2000);
        } else {
            getFragmentManager().popBackStack();
        }
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
        handleIntent(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdpt.disableForegroundDispatch(this);
    }
    */


    public void onNewIntent(Intent intent) {
        getNFCTag(intent);
    }

    private void handleIntent(Intent i) {
        getNFCTag(i);
    }

    boolean doubleBackToExitPressedOnce = false;

    private void getNFCTag(Intent i) {
        if (i == null){
            return;
        }

        String action = i.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Log.d("Nfc", "Action NDEF Found");
            Parcelable[] parcs = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            for (Parcelable p : parcs) {
                NdefMessage msg = (NdefMessage) p;

                NdefRecord[] records = msg.getRecords();
                BaseRecord result = NFCFactory.createRecord(records[0]);
                nfcTagPayload = result.payload;


            }

            String txt = (String)textViewNFCid.getText();

            if(!nfcTagPayload.isEmpty() && !txt.contains(nfcTagPayload)){
                callApiWithId(nfcTagPayload); //"BR1995N"
                textViewNFCid.setText("Code: " + nfcTagPayload);
            }
        }
    }
}
