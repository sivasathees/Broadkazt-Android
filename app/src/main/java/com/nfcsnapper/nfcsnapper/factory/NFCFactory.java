package com.nfcsnapper.nfcsnapper.factory;

import android.nfc.NdefRecord;
import android.util.Log;

import com.nfcsnapper.nfcsnapper.model.BaseRecord;
import com.nfcsnapper.nfcsnapper.model.RtdTextRecord;

import java.util.Arrays;

/**
 * Created by eivarsso on 07.05.2017.
 */

public class NFCFactory {

    public static BaseRecord createRecord(NdefRecord record) {
        short tnf = record.getTnf();
        byte[] cont = record.getPayload();

        if (tnf == NdefRecord.TNF_WELL_KNOWN) {
            Log.d("Nfc", "Well Known");
            // Check if it is TEXT
            if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                RtdTextRecord result = RtdTextRecord.createRecord(record.getPayload());
                return result;
            } else if (Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                Log.d("Nfc", "RTD_URI");
                //Handle URL specific NFC Tag
            }
        }
        return null;
    }
}