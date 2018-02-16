package com.nfcsnapper.nfcsnapper;

/**
 * Created by eivarsso on 25.05.2017.
 */

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.nfcsnapper.nfcsnapper.model.AssetDataModel;
import com.nfcsnapper.nfcsnapper.model.Video;

import java.util.ArrayList;

import static com.nfcsnapper.nfcsnapper.MainActivity.EXTRA_MESSAGE;


public class SlideshowDialogFragment extends DialogFragment {
    private String TAG = SlideshowDialogFragment.class.getSimpleName();
    private ArrayList<Video> videos;
    private AssetDataModel assetDataModel;
    private ViewPager viewPager;
    private MyViewPagerAdapter myViewPagerAdapter;
    private int selectedPosition = 0;
    private TextView lblCount;
    private ImageButton btnClose;
    private FloatingActionButton btnSupport;

    public Fragment first;
    static SlideshowDialogFragment newInstance() {
        SlideshowDialogFragment f = new SlideshowDialogFragment();
        return f;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(first != null) {
            ((FirstFragment) first).layout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_image_slider, container, false);
        final String logoUrl = getArguments().getString("logoUrl");
        assetDataModel = (AssetDataModel) getArguments().getSerializable("asset");

        viewPager = (ViewPager) v.findViewById(R.id.viewpager);
        lblCount = (TextView) v.findViewById(R.id.lbl_count);
        btnClose = (ImageButton) v.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        btnSupport = (FloatingActionButton) v.findViewById(R.id.btn_support);
        if (assetDataModel.getCaseBool().equalsIgnoreCase("true")){
            btnSupport.setVisibility(View.VISIBLE);
            btnSupport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), WebActivity.class);
                    intent.putExtra(WebActivity.EXTRA_LOGO_URL, logoUrl);
                    intent.putExtra(WebActivity.EXTRA_WEB_URL, assetDataModel.getCaseUrl());
                    intent.putExtra(WebActivity.EXTRA_VIDEO, videos.get(viewPager.getCurrentItem()));
                    startActivity(intent);
                }
            });
        }
        else {
            btnSupport.setVisibility(View.GONE);
        }

        videos = (ArrayList<Video>) getArguments().getSerializable("videos");
        selectedPosition = getArguments().getInt("position");

        Log.e(TAG, "position: " + selectedPosition);
        Log.e(TAG, "Videos size: " + videos.size());

        myViewPagerAdapter = new MyViewPagerAdapter(logoUrl);
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        setCurrentItem(selectedPosition);

        return v;
    }


    private void setCurrentItem(int position) {
        viewPager.setCurrentItem(position, false);
        displayMetaInfo(selectedPosition);
    }

    //  page change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            displayMetaInfo(position);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    private void displayMetaInfo(int position) {
        lblCount.setText((position + 1) + " of " + videos.size());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

    }

    //  adapter
    public class MyViewPagerAdapter extends PagerAdapter {

        private LayoutInflater layoutInflater;
        private String logoUrl;

        public MyViewPagerAdapter(String logoUrl) {
            this.logoUrl = logoUrl;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.image_fullscreen_preview, container, false);

            ImageView imageViewPreview = (ImageView) view.findViewById(R.id.image_preview);
            ImageView imageLogo = (ImageView) view.findViewById(R.id.img_logo);
            ImageView videoButton = (ImageView) view.findViewById(R.id.buttonVideo);
            TextView lblTitle = (TextView) view.findViewById(R.id.title);
            TextView lblDuration = (TextView) view.findViewById(R.id.duration);
            TextView lblDescription = (TextView) view.findViewById(R.id.lblDescription);

            final Video video = videos.get(position);

            lblTitle.setText(video.getVideoName());
            lblDuration.setText(getDisplayedDuration(video.getDuration()));
            lblDescription.setText(video.getDescription());

            Glide.with(getActivity()).load(video.getThumbnail())
                    .crossFade()
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageViewPreview);

            if(logoUrl != null) {
                Glide.with(getActivity()).load(logoUrl)
                        .into(imageLogo);
            }

            videoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(video.getAwsUrl() != null && video.getAwsUrl() != ""){

//                        Intent intent = new Intent(getActivity(), VideoViewActivity.class);
//
//                        intent.putExtra(EXTRA_MESSAGE, video.getAwsUrl());
//                        startActivity(intent);

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(video.getAwsUrl()), "video/*");
                        startActivity(intent);

                    }
                }
            });

            container.addView(view);

            return view;
        }

        private String getDisplayedDuration(int sec) {
            int mins = sec / 60;
            int secs = sec - mins * 60;
            return "" + mins + ":" + (secs < 10 ? "0"+secs:secs);
        }

        @Override
        public int getCount() {
            return videos.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == ((View) obj);
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

            container.removeView((View) object);

        }
    }
}
