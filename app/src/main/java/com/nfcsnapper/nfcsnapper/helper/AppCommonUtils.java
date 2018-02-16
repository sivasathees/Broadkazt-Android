package com.nfcsnapper.nfcsnapper.helper;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.math.BigDecimal;
import java.math.MathContext;


/**
 * Created by macbookpro on 13/02/2018.
 */

public class AppCommonUtils {

    private ProgressDialog dialog;
    private static AppCommonUtils _instance = null;
    private static Context mContext;

    private AppCommonUtils(){

    }

    public static AppCommonUtils getInstance(Context context){
        mContext = context;
        if (_instance == null){
            _instance = new AppCommonUtils();
        }
        return _instance;
    }

    public void showProgressDialog(String message){
        dialog = new ProgressDialog(mContext);
        dialog.setCancelable(false);
        if (message != null)
            dialog.setMessage(message);
        else
            dialog.setMessage("Please Wait...");

        try {
            if (dialog != null)
                if (!dialog.isShowing())
                        dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void dismissProgressDialog() {
        try {
            if (dialog != null)
                if (dialog.isShowing())
                    dialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showNoConnectionDialog(final Context ctx1) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx1);
        builder.setCancelable(true);
        builder.setMessage("Please check your internet connection settings to proceed");
        builder.setTitle("Connection Error!");
        builder.setPositiveButton("Setting",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ctx1.startActivity(new Intent(
                                Settings.ACTION_SETTINGS));
                    }
                });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            }
        });


        builder.show();
    }

    public boolean isOnline(Context context) {

        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

}
