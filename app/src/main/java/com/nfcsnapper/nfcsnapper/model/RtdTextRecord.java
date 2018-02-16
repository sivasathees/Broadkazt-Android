package com.nfcsnapper.nfcsnapper.model;

import android.util.Log;

/**
 * Created by eivarsso on 07.05.2017.
 */


public class RtdTextRecord extends BaseRecord {

    public String language;
    public String encoding;

    public RtdTextRecord() {
        this.tnf = 0x01;
    }

    public String toString() {
        Log.d("Nfc","Here to string");
        StringBuffer buffer = new StringBuffer(super.toString());
        buffer.append(" Encoding:" + encoding);
        buffer.append(" Content: " + payload);

        return buffer.toString();
    }

    public static RtdTextRecord createRecord(byte[] payload) {
        Log.d("Nfc", "Text record");
        String txtContent = "";
        // Handle text

        RtdTextRecord record = new RtdTextRecord();

        byte status = payload[0];
        int enc = status & 0x80; // Bit mask 7th bit 1
        String encString = null;
        if (enc == 0)
            encString = "UTF-8";
        else
            encString = "UTF-16";

        record.encoding = encString;
        int ianaLength = status & 0x3F; // Bit mask bit 5..0
        Log.d("Nfc", "IANA Len [" + ianaLength + "]");

        try {
            String content = new String(payload, ianaLength + 1, payload.length - 1 - ianaLength, encString);
            record.payload = content;
        }
        catch(Throwable t) {
            t.printStackTrace();
        }

        return record;
    }
}