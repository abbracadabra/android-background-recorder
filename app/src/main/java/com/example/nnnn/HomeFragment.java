package com.example.nnnn;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home, container, false);

        return view;
    }

    @Override
    public void onViewCreated(View view,Bundle savedInstanceState) {
        LinearLayout layout_home = (LinearLayout) getView().findViewById(R.id.layout_home);
        for(final String k : MainActivity.cameras.keySet()){
            String cname = MainActivity.config.getJSONObject("cameras").getJSONObject(k).getString("cameraname");
            Button startButton = new Button(layout_home.getContext());
            startButton.setText(cname+"start");
            Button stopButton = new Button(layout_home.getContext());
            stopButton.setText(cname+"stop");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            layout_home.addView(startButton,lp);
            layout_home.addView(stopButton,lp);

            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.cameras.get(k).start();
                }
            });
            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.cameras.get(k).stop();
                }
            });
        }
    }
}