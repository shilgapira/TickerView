package com.shilgapira.ticker;

import java.util.Random;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DemoActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        
        TickerView tickerView = (TickerView) findViewById(R.id.ticker);
        tickerView.setAdapter(mAdapter);
    }
    
    private Adapter mAdapter = new BaseAdapter() {
        
        Random mRand = new Random();
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(DemoActivity.this).inflate(R.layout.ticker_item, null);
            TextView textView = (TextView) view.findViewById(R.id.text);
            float[] hsv = new float[] {mRand.nextInt(360), 0.5f, 0.5f};
            textView.setTextColor(Color.HSVToColor(hsv));
            return view;
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public Object getItem(int position) {
            return new Object();
        }
        
        @Override
        public int getCount() {
            return 5;
        }
    };

}
