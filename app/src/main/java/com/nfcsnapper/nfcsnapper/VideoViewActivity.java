package com.nfcsnapper.nfcsnapper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.io.IOException;

/**
 * Created by eivarsso on 07.05.2017.
 */

public class VideoViewActivity extends Activity implements VideoRendererEventListener{
    private static final String TAG = "Logging VideoActivity";
    private SimpleExoPlayerView videoView;
    SimpleExoPlayer player;
    ImageView videoBackBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_viewer);

        Intent intent = getIntent();
        String url = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        playVideoFromUrl(url);

    }

    @Override
    protected void onStop() {
//        player.stop();
        super.onStop();
    }

    private void playVideoFromUrl(String awsUrl) {

        Log.e(TAG, awsUrl);
//        getWindow().setFormat(PixelFormat.TRANSLUCENT);
//        Handler mainHandler = new Handler();
//        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
//        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
//        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
//        LoadControl loadControl = new DefaultLoadControl();

//        player = ExoPlayerFactory.newSimpleInstance(this,trackSelector,loadControl);
//        videoView = new SimpleExoPlayerView(this);
//        videoView = (SimpleExoPlayerView) findViewById(R.id.video_View);
//        videoView.setUseController(true);

//        videoBackBtn = (ImageView) findViewById(R.id.videoBackBtn);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(awsUrl), "video/*");
        startActivity(intent);

//        videoBackBtn.setVisibility(View.VISIBLE);
//        videoBackBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                onBackPressed();
//            }
//        });
//
//        videoView.setPlayer(player);
//        DefaultBandwidthMeter bandwidthMeter2 = new DefaultBandwidthMeter();
//        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "exoplayer2example"), bandwidthMeter2);
////        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this.getApplicationContext(),
////        Util.getUserAgent(this.getApplicationContext(), "yourApplicationName"), bandwidthMeter2);
//
//        String userAgent = Util.getUserAgent(this, "Bakingapp");
//
////        MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(awsUrl), dataSourceFactory,new
////                DefaultExtractorsFactory(), null, null);
//
//        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
//
//        MediaSource videoSource = new HlsMediaSource(Uri.parse(awsUrl), dataSourceFactory, 1, null, null);
//        MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(awsUrl),
//                dataSourceFactory, extractorsFactory, null, null);
//        final LoopingMediaSource loopingSource = new LoopingMediaSource(mediaSource);
//
//// Prepare the player with the source.
//        player.prepare(loopingSource);
//
//        player.addListener(new ExoPlayer.EventListener() {
//            @Override
//            public void onTimelineChanged(Timeline timeline, Object manifest) {
//                Log.v(TAG, "Listener-onTimelineChanged...");
//            }
//
//            @Override
//            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
//                Log.v(TAG, "Listener-onTracksChanged...");
//            }
//
//            @Override
//            public void onLoadingChanged(boolean isLoading) {
//                Log.v(TAG, "Listener-onLoadingChanged...isLoading:"+isLoading);
//            }
//
//            @Override
//            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//                Log.v(TAG, "Listener-onPlayerStateChanged..." + playbackState);
//            }
//
//            @Override
//            public void onRepeatModeChanged(int repeatMode) {
//
//            }
//
//            @Override
//            public void onPlayerError(ExoPlaybackException error) {
//                Log.v(TAG, "Listener-onPlayerError...");
//                player.stop();
//                player.prepare(loopingSource);
//                player.setPlayWhenReady(true);
//            }
//
//            @Override
//            public void onPositionDiscontinuity() {
//                Log.v(TAG, "Listener-onPositionDiscontinuity...");
//            }
//
//            @Override
//            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
//                Log.v(TAG, "Listener-onPlaybackParametersChanged...");
//            }
//        });



//        HlsMediaSource videoSource = new HlsMediaSource(Uri.parse(awsUrl), dataSourceFactory, mainHandler, new AdaptiveMediaSourceEventListener() {
//            @Override
//            public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {
//
//            }
//
//            @Override
//            public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
//
//            }
//
//            @Override
//            public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
//
//            }
//
//            @Override
//            public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
//
//            }
//
//
//            @Override
//            public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {
//
//            }
//
//            @Override
//            public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {
//
//            }
//        });

//        player.prepare(mediaSource);
//        videoView.requestFocus();
//        videoView.setVisibility(View.VISIBLE);
//        player.setPlayWhenReady(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart()...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()...");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()...");
//        player.release();
    }

    @Override
    public void onVideoEnabled(DecoderCounters counters) {

    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

    }

    @Override
    public void onVideoInputFormatChanged(Format format) {

    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {

    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {

    }
}
