package com.nfcsnapper.nfcsnapper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.nfcsnapper.nfcsnapper.crop.CropImage;
import com.nfcsnapper.nfcsnapper.crop.MarshMallowPermission;
import com.nfcsnapper.nfcsnapper.model.Video;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class WebActivity extends AppCompatActivity {
    public static final int REQUEST_IMAGE_CAPTURE = 777;
    public static final int REQUEST_VIDEO_CAPTURE = 666;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1;
    private static int LOAD_IMAGE_RESULTS = 2;
    private static int LOAD_VIDEO_RESULTS = 3;
    private static int CROP_PIC = 5;
    private static int VID_CAPTURE_SAMSUNG = 6;
    private static int IMG_CAPTURE_SAMSUNG = 7;
    private int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 2;
    private int CAMERA_PERMISSION_REQUEST_CODE = 3;
    private int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 4;

    private Uri imageUri, videoUri = null;
    private String imagePath;
    private String videoPath = "";
    private boolean isPhoto;

    String manufacturer;
    MarshMallowPermission marshMallowPermission;

    public static final String EXTRA_LOGO_URL = "logo url";
    public static final String EXTRA_VIDEO = "video";
    public static final String EXTRA_WEB_URL = "web_url";
    private boolean isFirstPageLoading = true;

    private Uri outputFileUri;

    private String awsUrlToUploadedFile;

    AmazonS3 s3Client;
    String bucket = "bktest-file-transfer";
    File uploadToS3;
    TransferUtility transferUtility;
    private boolean isFormSubmitted = false;

    WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        manufacturer = android.os.Build.MANUFACTURER;
        marshMallowPermission = new MarshMallowPermission(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (isFormSubmitted){
                    isFormSubmitted = false;
                    Toast.makeText(WebActivity.this, "Sak har blitt opprettet", Toast.LENGTH_SHORT).show();
                    finish();
                }
                else {
                    super.onPageFinished(view, url);
                    final Video video = (Video) getIntent().getSerializableExtra(EXTRA_VIDEO);

                    if (video != null && isFirstPageLoading) {
                        final String js = "javascript:" +
                                "document.getElementById('subject').value = '" + video.getVideoName() + "';";
                        Log.d("kek", js);
                        if (Build.VERSION.SDK_INT >= 19) {
                            view.evaluateJavascript(js, new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String s) {}
                            });
                        } else {
                            view.loadUrl(js);
                        }
                    }

                    isFirstPageLoading = false;
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        String caseUrl = getIntent().getStringExtra(EXTRA_WEB_URL);
        webView.loadUrl(caseUrl);

        String logoUrl = getIntent().getStringExtra(EXTRA_LOGO_URL);
        if (logoUrl != null) {
            ImageView logo = (ImageView) findViewById(R.id.img_logo);
            Glide.with(this)
                    .load(logoUrl)
                    .crossFade()
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(logo);
        }

        // callback method to call credentialsProvider method.
        s3credentialsProvider();

        // callback method to call the setTransferUtility method
        setTransferUtility();
    }

    public void s3credentialsProvider(){

        // Initialize the AWS Credential
        CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider =
                new CognitoCachingCredentialsProvider(
                        getApplicationContext(),
                        "us-east-1:216422c8-defa-44e3-98fc-5df302b08cfb", // Identity Pool ID
                        Regions.US_EAST_1 // Region
                );
        createAmazonS3Client(cognitoCachingCredentialsProvider);
    }

    /**
     *  Create a AmazonS3Client constructor and pass the credentialsProvider.
     * @param credentialsProvider
     */
    public void createAmazonS3Client(CognitoCachingCredentialsProvider
                                             credentialsProvider){

        ClientConfiguration cc = new ClientConfiguration();
            cc.setSocketTimeout(1200000);
        // Create an S3 client
        s3Client = new AmazonS3Client(credentialsProvider,cc);

        // Set the region of your S3 bucket
        s3Client.setRegion(Region.getRegion(Regions.EU_WEST_2));

    }

    public void setTransferUtility(){

        transferUtility = TransferUtility.builder().s3Client(s3Client).context(getApplicationContext()).build(); //new TransferUtility(s3Client, getApplicationContext());
    }

    /**
     * This method is used to upload the file to S3 by using TransferUtility class
     */
    public void uploadFileToS3(Uri ouri){
        outputFileUri = ouri;
        Log.e("**********error getPath",outputFileUri.getPath());
        Log.e("******error getEncodedP",outputFileUri.getEncodedPath());
//        uploadToS3 = new File("/storage/emulated/0/Pictures/1511431544075.jpg");
        uploadToS3 = new File(outputFileUri.getPath());
//        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        //uploadToS3 = new File("/storage/emulated/0/Pictures/Screenshots/Screenshot_20160627-115728.png");
        TransferObserver transferObserver = transferUtility.upload(
                bucket,          /* The bucket to upload to */
                outputFileUri.getLastPathSegment(),/* The key for the uploaded object */
                uploadToS3       /* The file where the data to upload exists */
        );

        transferObserverListener(transferObserver);
    }

    public void transferObserverListener(TransferObserver transferObserver){

        transferObserver.setTransferListener(new TransferListener(){

            @Override
            public void onStateChanged(int id, TransferState state) {
                Toast.makeText(getApplicationContext(), "" + state,
                        Toast.LENGTH_SHORT).show();
                if(state == TransferState.COMPLETED){
                    awsUrlToUploadedFile = "https://s3.eu-west-2.amazonaws.com/bktest-file-transfer/" + outputFileUri.getLastPathSegment();
                    Log.e("**********Link to aws",awsUrlToUploadedFile);
                    webView.loadUrl("javascript: (function() {document.getElementById('description').value= '" + awsUrlToUploadedFile  + "';}) ();" );
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) (bytesCurrent/bytesTotal * 100);
                Toast.makeText(getApplicationContext(), "Progress: " + percentage + "%",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e("**********error","error");
                Log.e("**********error",ex.getMessage());
                Log.e("**********error",ex.toString());
            }

        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_web, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send:
                isFormSubmitted = true;
                webView.loadUrl("javascript: (function() {document.getElementById('bkazt').submit();}) ();" );
                return false;
            case R.id.action_photo:
                capturePhoto();
                return false;
            case R.id.action_video:
                captureVideo();
                return false;
        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
//            if (outputFileUri != null) {
//                Toast.makeText(this, outputFileUri.getPath(), Toast.LENGTH_LONG).show();
//                uploadFileToS3(outputFileUri);
//            }
//        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            if (outputFileUri != null) {
//                Toast.makeText(this, outputFileUri.getPath(), Toast.LENGTH_LONG).show();
//                uploadFileToS3(outputFileUri);
//            }
//        }
//    }

    private void capturePhoto() {

        if (ContextCompat.checkSelfPermission(WebActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(WebActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(WebActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    isPhoto = true;
                    takePhoto();
                }
                else {
                    // Write external storage Allow
                    if (ActivityCompat.shouldShowRequestPermissionRationale(WebActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                        Toast.makeText(WebActivity.this, "External Storage permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
                    } else {
                        ActivityCompat.requestPermissions(WebActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                    }
                }
            }
            else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(WebActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Toast.makeText(WebActivity.this, "Read external Storage permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
                } else {
                    ActivityCompat.requestPermissions(WebActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                }
            }
        }
        else {
            // Camera allow
            if (ActivityCompat.shouldShowRequestPermissionRationale(WebActivity.this, Manifest.permission.CAMERA)){
                Toast.makeText(WebActivity.this, "Camera permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(WebActivity.this,new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION_REQUEST_CODE);
            }
        }

    }

    private void captImage(){
        outputFileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void captureVideo() {
        if (ContextCompat.checkSelfPermission(WebActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(WebActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(WebActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    isPhoto = false;
                    takePhoto();
                }
                else {
                    // Write external storage Allow
                    if (ActivityCompat.shouldShowRequestPermissionRationale(WebActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                        Toast.makeText(WebActivity.this, "External Storage permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
                    } else {
                        ActivityCompat.requestPermissions(WebActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                    }
                }
            }
            else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(WebActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Toast.makeText(WebActivity.this, "Read external Storage permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
                } else {
                    ActivityCompat.requestPermissions(WebActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                }
            }
        }
        else {
            // Camera allow
            if (ActivityCompat.shouldShowRequestPermissionRationale(WebActivity.this, Manifest.permission.CAMERA)){
                Toast.makeText(WebActivity.this, "Camera permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(WebActivity.this,new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void captVideo(){
        outputFileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);

        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(WebActivity.this, "Permission denied for Audio record", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 2:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(WebActivity.this, "Permission denied for write external storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 3:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if(ContextCompat.checkSelfPermission(WebActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        if (ActivityCompat.shouldShowRequestPermissionRationale(WebActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                            Toast.makeText(WebActivity.this, "External Storage permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
                        } else {
                            ActivityCompat.requestPermissions(WebActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                        }
                    }
                    if(ContextCompat.checkSelfPermission(WebActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(WebActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            Toast.makeText(WebActivity.this, "Read external Storage permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
                        } else {
                            ActivityCompat.requestPermissions(WebActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                        }
                    }
//                    if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
//                        if(ContextCompat.checkSelfPermission(AddPost.this, Manifest.permission.C) != PackageManager.PERMISSION_GRANTED) {
//                            if (ActivityCompat.shouldShowRequestPermissionRationale(AddPost.this, Manifest.permission.FLASHLIGHT)) {
//                                Toast.makeText(AddPost.this, "Flashlight permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
//                            } else {
//                                ActivityCompat.requestPermissions(AddPost.this, new String[]{Manifest.permission.FLASHLIGHT}, FLASHLIGHT_PERMISSION_REQUEST_CODE);
//                            }
//                        }
//                    }
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(WebActivity.this, "Permission denied for camera", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 4:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(WebActivity.this, "Read external storage permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 5:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(WebActivity.this, "Flashlight permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public Uri getOutputMediaFileUri(int type) {
        if (Build.VERSION.SDK_INT > 21) {
            return FileProvider.getUriForFile(getApplicationContext(), getPackageName()+".fileprovider", getOutputMediaFile(type));
        } else {
            return Uri.fromFile(getOutputMediaFile(type));
        }
    }

    private File getOutputMediaFile(int type) {
        final String imageDirectoryName = getString(R.string.app_name);

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                imageDirectoryName
                );

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(imageDirectoryName, "Oops! Failed create "
                        + imageDirectoryName + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".png");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private File getImageOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "BroadKaztCam");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("BroadKaztCam", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + "post" + ".png");

        return mediaFile;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("file_uri", outputFileUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        outputFileUri = savedInstanceState.getParcelable("file_uri");
    }


    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte [] b=baos.toByteArray();
        String temp= Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(getCacheDir(), "img_"+System.currentTimeMillis()));
        CropImage.of(source, destination).start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            Uri uri = CropImage.getOutput(result);
            Bitmap thePic = null;

            if (null != uri) {
                imageUri = uri;
                imagePath = new File(uri.getPath()).getAbsolutePath();
                thePic = decodeUriAsBitmap(uri);
            } else {
                thePic = (Bitmap) result.getExtras().get("data");
            }
            uploadFileToS3(Uri.parse(imagePath));

        } else if (resultCode == CropImage.RESULT_ERROR) {
            Toast.makeText(this, CropImage.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static String convertImageUriToFile(Uri imageUri, Context context) {
        Cursor cursor;
        String imagePath = null;
        int imageID = 0;
        try {
            String[] proj = {
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Thumbnails._ID,
                    MediaStore.Images.ImageColumns.ORIENTATION
            };
            cursor = ((Activity) context).managedQuery(
                    imageUri,         //  Get data for specific image URI
                    proj,             //  Which columns to return
                    null,             //  WHERE clause; which rows to return (all rows)
                    null,             //  WHERE clause selection arguments (none)
                    null              //  Order-by clause (ascending by name)
            );
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int file_ColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            if (cursor.moveToFirst()) {
                imageID = cursor.getInt(columnIndex);
                imagePath = cursor.getString(file_ColumnIndex);
                //Functions.Logger("iamgePath", imagePath);
            }
        } finally {
        }
        return imagePath;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }


    public static String getFilename() {
        File file = new File(Environment.getExternalStorageDirectory().getPath(), "MyFolder/Images");
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".png");
        return uriSting;
    }

    public static String getCompressedImagePath(String imagePathReturned) {
        String imagePath = null;
        Bitmap scaledBitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(imagePathReturned, options);
        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;
        float maxHeight = 816.0f;
        float maxWidth = 612.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;
            }
        }
        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];
        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(imagePathReturned, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;
        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
        ExifInterface exif;
        try {
            exif = new ExifInterface(imagePathReturned);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream out = null;
        imagePath = getFilename();
        try {
            out = new FileOutputStream(imagePath);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);
//            Functions.Logger("imagepathcompresse", imagePath);
//            Functions.Logger("imagepathreturn", imagePathReturned);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return imagePath;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {

            if (resultCode == Activity.RESULT_OK) {
                if(!isKitKatAndAbove()){
                    imageUri = getImageUri();
//                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 || manufacturer.equalsIgnoreCase("samsung")) {
//                        performCrop(imageUri);
//                    }
//                    else {
//                        beginCrop(imageUri);
//                    }
                    uploadFileToS3(imageUri);
                }
                else {
                    String imagePathReturned;
                    if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 || manufacturer.equalsIgnoreCase("samsung")) && !isMarshMallow()) {
                        imagePathReturned = convertImageUriToFile(imageUri, this);
                        imagePath = getCompressedImagePath(imagePathReturned);
                        //uploadFileToS3(Uri.fromFile(new File(imagePathReturned)));
                        //performCrop(imageUri);
                    }
                    else {
                        //beginCrop(imageUri);
                        imagePathReturned = convertImageUriToFile(imageUri, this);
                        imagePath = getCompressedImagePath(imagePathReturned);
                    }
                        uploadFileToS3(Uri.fromFile(new File(imagePathReturned)));
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(WebActivity.this, " Picture not taken ", Toast.LENGTH_SHORT).show();
            } else {
                imagePath = null;
                imageUri = null;
                Toast.makeText(WebActivity.this, " Picture not taken ", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOAD_IMAGE_RESULTS && resultCode == Activity.RESULT_OK && data != null) {
            Uri pickedImage = data.getData();
            String[] filePath = {MediaStore.Images.Media.DATA};
            Cursor cursor = this.getContentResolver().query(pickedImage, filePath, null, null, null);
            cursor.moveToFirst();
            imagePath = cursor.getString(cursor.getColumnIndex(filePath[0]));
            uploadFileToS3(Uri.fromFile(new File(imagePath)));
            //File f = new File(imagePath);
            //Uri myUri = Uri.fromFile(f);
//            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 || manufacturer.equalsIgnoreCase("samsung")) {
//                performCrop(myUri);
//            }
//            else {
            //beginCrop(myUri);
//            }
//            imagePath = Functions.getCompressedImagePath(imagePath);
//            if (imagePath != null) {
//                newPostPhotoImageView.setImageDrawable(new BitmapDrawable(BitmapFactory.decodeFile(imagePath)));
//                photoVisibleView.setVisibility(View.GONE);
//            }
            cursor.close();
        } else if (requestCode == LOAD_VIDEO_RESULTS && resultCode == Activity.RESULT_OK && data != null) {
            Uri pickedImage = data.getData();
            videoPath = getPath(this,pickedImage);
            try {
                if (videoPath != null) {
                    MediaPlayer mp = MediaPlayer.create(this, Uri.parse(videoPath));
                    int duration = mp.getDuration();
                    mp.release();

                    if((duration/1000) > 10){
                        // Show Your Messages
                        Toast.makeText(WebActivity.this,"Video can not be greater than of 10 sec",Toast.LENGTH_LONG).show();
                    }else{
                        uploadFileToS3(videoUri);
//                        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath,
//                                MediaStore.Images.Thumbnails.MINI_KIND);
                        //newPostVideoImageView.setImageDrawable(new BitmapDrawable(thumb));
                        //videoVisibleView.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        else if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            if(!isKitKatAndAbove()){
                videoPath = videoUri.getPath();
            }
            else{
                Uri pickedImage = data.getData();
                videoPath = getPath(this, pickedImage);
            }
            uploadFileToS3(Uri.parse(videoPath));
//            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath,
//                    MediaStore.Images.Thumbnails.MINI_KIND);

        }
        else if(requestCode == VID_CAPTURE_SAMSUNG){
            if(videoUri !=null){
                videoPath = videoUri.getPath();
                uploadFileToS3(videoUri);
//                Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath,
//                        MediaStore.Images.Thumbnails.MINI_KIND);
            }
        }

        else if(requestCode == IMG_CAPTURE_SAMSUNG){
            imageUri = getOutImageUri();
//            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 || manufacturer.equalsIgnoreCase("samsung")) {
//                performCrop(imageUri);
//            }
//            else {
//                beginCrop(imageUri);
//            }
            uploadFileToS3(imageUri);
        }
        else if(requestCode == CROP_PIC && data == null){
            imagePath = null;
            imageUri = null;
            Toast.makeText(WebActivity.this, "Result Cancelled", Toast.LENGTH_SHORT).show();
        }
        else if (requestCode == CropImage.REQUEST_CROP){
            handleCrop(resultCode, data);
        }
        else if (requestCode == CROP_PIC) {
            // get the returned data
            if (data != null) {
                Uri uri = data.getData();
                Bitmap thePic = null;

                if (null != uri) {
                    imageUri = uri;
                    imagePath = new File(uri.getPath()).getAbsolutePath();
                    thePic = decodeUriAsBitmap(uri);
                } else {
                    thePic = (Bitmap) data.getExtras().get("data");
//                thePic = data.getExtras().getParcelable("data");
                    // get uri from bitmap and do something with uri
                }
                uploadFileToS3(imageUri);
            }
        }
    }
    private Bitmap decodeUriAsBitmap(Uri uri){
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    private void performCrop(Uri picUri) {
        // take care of exceptions
        try {
            // call the standard crop action intent (the user device may not
            // support it)
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            // indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            // set crop properties
            cropIntent.putExtra("crop", "true");
            // indicate aspect of desired crop
//            cropIntent.putExtra("aspectX", 1);
//            cropIntent.putExtra("aspectY", 1);
//            // indicate output X and Y
//            cropIntent.putExtra("outputX", 500);
//            cropIntent.putExtra("outputY", 500);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
            // retrieve data on return
            cropIntent.putExtra("return-data", true);
            // start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, CROP_PIC);
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException anfe) {
            Toast toast = Toast
                    .makeText(this, "This device doesn't support the crop action!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }

    /*
     * returning image / video
     */
    private static File getOutputMediaFile() {

        // External sdcard
        // location.getExternalStoragePublicDirectory(
        // Environment.DIRECTORY_PICTURES),IMAGE_DIRECTORY_NAME);
        File mediaStorageDir = new File(
                Environment.getExternalStorageDirectory(),
                "Vid_Res");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("Vid_Res", "Oops! Failed create "
                        + "Vid_Res" + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "VID_" + timeStamp + ".mp4");
        }
        else {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "VID_" + timeStamp + ".mp4");
        }

        return mediaFile;
    }

    public Bitmap getBitmapFromUri() {

        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(new File(imagePath)), null, null);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Uri getImageUri() {
        // Store image in dcim
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "captured_img.png");
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        imagePath = file.toString();
        Uri imgUri = Uri.fromFile(file);
        imageUri = imgUri;
        return imgUri;
    }

    private String getRealPathFromUri(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = this.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private void CopyFile(File src, File dst, Activity activity) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
        if(dst.exists()){
            MediaScannerConnection.scanFile(activity, new String[] { dst.getPath() }, null, null);
        }
        //activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
    }

    private Uri getOutImageUri() {
        // Store image in dcim
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "captured_img.png");
        File fileOut = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "updated_img.png");
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!fileOut.exists()){
            try {
                fileOut.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 || manufacturer.equalsIgnoreCase("samsung")) {
            ExifInterface ei = null;
            try {
                CopyFile(file,fileOut,this);
                if(file.exists()){
                    file.delete();
                }
                imagePath = fileOut.toString();
                Uri imgUri = Uri.fromFile(fileOut);
                imageUri = imgUri;
                ei = new ExifInterface(imagePath);
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);

                switch(orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotate(getBitmapFromUri(), 90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotate(getBitmapFromUri(), 180);
                        break;
                    // etc.
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return imageUri;
    }

    private Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);
        Bitmap bmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
        return bmap;
    }

    private boolean isKitKatAndAbove() {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return true;
        }
        return false;
    }

    private boolean isMarshMallow(){
        int currentApiVersion = Build.VERSION.SDK_INT;
        if(currentApiVersion > Build.VERSION_CODES.LOLLIPOP_MR1){
            return true;
        }

        return false;
    }

    private void dispatchTakeVideoIntent() {
        if(isKitKatAndAbove()){
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
            takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
            }
        }
        else {
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            videoUri = getOutputMediaFileUri();
            // set video quality
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            if (intent.resolveActivity(getPackageManager()) != null) {
                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2){
                    startActivityForResult(intent, VID_CAPTURE_SAMSUNG);
                }
                else {
                    startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
                }
            }
        }


    }

    public void getPhotoFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File mediaStorageDir = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator
                        + "mphoto"
                        + File.separator
                        + "mcapturedPhoto"
        );

        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }
        try {
            if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 || manufacturer.equalsIgnoreCase("samsung"))) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, getImageUri());
                startActivityForResult(intent, IMG_CAPTURE_SAMSUNG);
            }
            else {
                // start camera activity
                String fileName = new Date().toString() + "png";
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, fileName);
                imageUri = this.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @param selection
     *            (Optional) Filter used in the query.
     * @param selectionArgs
     *            (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri,
                                       String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    private void takePhoto(){
        if (isPhoto) {
            if(isMarshMallow()){
                getPhotoFromCamera();
            }
            else {
                if(isKitKatAndAbove() && !manufacturer.equalsIgnoreCase("samsung")){
                    String fileName = new Date().toString() + "png";
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, fileName);
                    imageUri = this.getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                }
                else {
                    // create intent with ACTION_IMAGE_CAPTURE action
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, getImageUri());
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 || manufacturer.equalsIgnoreCase("samsung")) {
                        startActivityForResult(intent, IMG_CAPTURE_SAMSUNG);
                    }
                    else {
                        // start camera activity
                        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                    }
                }
            }
        } else {
            dispatchTakeVideoIntent();
        }
    }
}
