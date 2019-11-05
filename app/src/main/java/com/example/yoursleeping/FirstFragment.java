package com.example.yoursleeping;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


public class FirstFragment extends Fragment {
    // Store instance variables
    private String title;
    private int page;
    private LineChart lineChart;

    // newInstance constructor for creating fragment with arguments
    public static FirstFragment newInstance(int page, String title) {
        FirstFragment fragment = new FirstFragment();
        Bundle args = new Bundle();
        args.putInt("someInt", page);
        args.putString("someTitle", title);
        fragment.setArguments(args);
        return fragment;
    }

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        page = getArguments().getInt("someInt", 0);
        title = getArguments().getString("someTitle");

    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        InputStream inputStream = getResources().openRawResource(R.raw.sleepdata);

        lineChart = (LineChart) view.findViewById(R.id.line_chart);

        CSVFile csvFile = new CSVFile(inputStream);
        List<String[]> dataList = csvFile.read();

        String[] lastNight = dataList.get(dataList.size() - 1);
        List<Entry> dayList = getAllData(dataList, lastNight[0]);

        ArrayList<String> xAXES = new ArrayList<>();

        LineDataSet dataset = new LineDataSet(dayList, "수면상태");
        ArrayList<String> labels = new ArrayList<>();
        LineData data = new LineData(dataset);
        lineChart.setData(data);
        lineChart.animateY(10000);

        return view;
    }
    String TAG = "daydata";
    public List<Entry> getAllData(List<String[]> list, String lastnight){
        List<Entry> daylist = new ArrayList<>();
        String[] timeStr;
        float time, min, total;
        for (int i = list.size() - 1; i >= 0; i--){
//            Log.d(TAG, "daydata: " + list.get(i)[0]+ ", "+Float.parseFloat(list.get(i)[2]));
            if (list.get(i)[0].equals(lastnight)) {
                timeStr = list.get(i)[1].split(":");
                time = Float.parseFloat(timeStr[0]) * 60;
                min = Float.parseFloat(timeStr[1]);
                total = time + min;
                daylist.add(new Entry(total, Float.parseFloat(list.get(i)[2])));
            }
            else {
                break;
            }
        }

        for (int i = 0; i < daylist.size(); i++) {
            Log.d(TAG, "daydata2: "+ daylist.get(i).getX() + ", " + daylist.get(i).getY());
        }

        return daylist;
    }

}