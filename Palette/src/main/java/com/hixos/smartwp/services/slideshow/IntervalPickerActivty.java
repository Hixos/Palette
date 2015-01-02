package com.hixos.smartwp.services.slideshow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.TimeMath;
import com.hixos.smartwp.widget.IntervalPicker;

public class IntervalPickerActivty extends Activity implements IntervalPicker.OnIntervalSelectedListener, AdapterView.OnItemSelectedListener{

    public static final String EXTRA_INTERVAL = "com.hixos.smartwp.EXTRA_INTERVAL";
    public static final String RESULT_INTERVAL = "com.hixos.smartwp.RESULT_INTERVAL";

    private static final int UNIT_MINUTES = 0;
    private static final int UNIT_HOURS = 1;
    private static final int UNIT_DAYS = 2;

    private IntervalSpinnerAdapter mAdapter;

    private TextView mValueText;
    private Spinner mUnitSpinner;
    private IntervalPicker mIntervalPicker;
    private int mValue = 1;
    private int mUnit = UNIT_HOURS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interval_picker);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        int mw = getResources().getDimensionPixelSize(R.dimen.picker_max_width);

        if(width > mw){
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = mw;
            this.getWindow().setAttributes(params);
        }

        if(savedInstanceState != null){
            mValue = savedInstanceState.getInt("current_value");
            mUnit = savedInstanceState.getInt("current_unit");
        }else{
            Intent i = getIntent();
            long interval = i.getLongExtra(EXTRA_INTERVAL, 0);
            mValue = TimeMath.getTimeInUnit(interval);
            mUnit = TimeMath.getTimeUnitValue(interval);
        }

        initView();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("current_value", mValue);
        outState.putInt("current_unit", mUnit);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    private void initView() {
        mUnitSpinner = (Spinner)findViewById(R.id.spinner_unit);
        mValueText = (TextView)findViewById(R.id.textview_interval_value);
        mAdapter = new IntervalSpinnerAdapter(this);
        mAdapter.setQuantity(mValue);

        mUnitSpinner.setAdapter(mAdapter);
        mUnitSpinner.setSelection(mUnit);
        mUnitSpinner.setOnItemSelectedListener(this);

        mIntervalPicker = (IntervalPicker)findViewById(R.id.interval_picker);
        mIntervalPicker.setInterval(mValue);
        updateRange(mUnit);
        mIntervalPicker.setOnIntervalSelectedListener(this);

        findViewById(R.id.set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long interval = 0;
                switch (mUnit) {
                    case 0:
                        interval = mValue * 60;
                        break;
                    case 1:
                        interval = mValue * 60 * 60;
                        break;
                    case 2:
                        interval = mValue * 60 * 60 * 24;
                        break;
                }

                Intent result = new Intent();
                result.putExtra(RESULT_INTERVAL, interval * 1000);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });
    }

    @Override
    public void onIntervalSelected(int interval) {
        Integer i = interval;
        mValue = interval;
        mAdapter.setQuantity(interval);
        mValueText.setText(i.toString());
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        updateRange(i);
        mUnit = i;
    }

    private void updateRange(int unit) {
        switch (unit) {
            case 0:
                mIntervalPicker.setRange(60);
                break;
            case 1:
                mIntervalPicker.setRange(24);
                break;
            case 2:
                mIntervalPicker.setRange(10);
                break;

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public class IntervalSpinnerAdapter extends BaseAdapter {
        private Context mContext;

        private int mQuantity = 0;

        public IntervalSpinnerAdapter(Context context) {
            mContext = context;
        }

        private void setQuantity(int count){
            mQuantity = count;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View v = inflater.inflate(R.layout.spinner_item_interval, null);
            TextView t = (TextView)v.findViewById(R.id.spinner_item_text);
            t.setText(getString(i));
            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View v = inflater.inflate(R.layout.spinner_item_interval_dropdown, null);
            TextView t = (TextView)v.findViewById(R.id.spinner_item_text);
            t.setText(getString(position));
            return v;
        }

        private String getString(int unit){
            switch (unit){
                default:
                case UNIT_MINUTES:
                    return mContext.getResources().getQuantityString(R.plurals.minute, mQuantity);
                case UNIT_HOURS:
                    return mContext.getResources().getQuantityString(R.plurals.hour, mQuantity);
                case UNIT_DAYS:
                    return mContext.getResources().getQuantityString(R.plurals.day, mQuantity);
            }
        }
    }


}
