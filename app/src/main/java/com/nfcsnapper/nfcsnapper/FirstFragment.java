package com.nfcsnapper.nfcsnapper;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;


/**
 * A simple {@link Fragment} subclass.
 */
public class FirstFragment extends Fragment {

    ProgressBar progressBar;
    public LinearLayout layout;
    //public int showFlag = 0;
    public FirstFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        progressBar = (ProgressBar)view.findViewById(R.id.progressbar);
        layout = (LinearLayout)view.findViewById(R.id.linearvertical);
        if (progressBar != null) {
            //progressBar.setVisibility(View.VISIBLE);
            //progressBar.setIndeterminate(true);
            progressBar.getIndeterminateDrawable().setColorFilter(0xFF1B53AF, android.graphics.PorterDuff.Mode.MULTIPLY);
        }
        return view;
    }
    public void showProgressBar()
    {
        progressBar.setVisibility(View.VISIBLE);
        layout.setVisibility(View.INVISIBLE);
    }

    public void hideProgressBar()
    {
        progressBar.setVisibility(View.INVISIBLE);

    }

}
