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

import java.io.InputStream;
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
        CSVFile csvFile = new CSVFile(inputStream);
        List<String[]> dataList = csvFile.read();
        lineChart = (LineChart) view.findViewById(R.id.line_chart);
        ArrayList<String> xAXES = new ArrayList<>();
        String[] lastnight = dataList.get(dataList.size()-1);
        List<Entry> dayList = daydata(dataList, lastnight[0]);

        LineDataSet dataset = new LineDataSet(dayList, "수면상태");
        LineData data = new LineData(dataset);
        lineChart.setData(data);
        lineChart.animateY(5000);



        return view;
    }
    String TAG = "daydata";
    public List<Entry> daydata(List<String[]> list, String lastnight){
        List<Entry> daylist = new ArrayList<>();
        float time;
        float min;
        float total;
        for (int i = list.size() - 1; i >= 0; i--){

            Log.d(TAG, "daydata: " + list.get(i)[0]+ ", "+Float.parseFloat(list.get(i)[2]));
            if(list.get(i)[0].equals(lastnight)){
                time = Float.parseFloat(list.get(i)[1].split(":")[0]) * 60;
                min = Float.parseFloat(list.get(i)[1].split(":")[1]);
                total = time + min;
                daylist.add(new Entry(total, Float.parseFloat(list.get(i)[2])));
            }else{
                break;
            }
        }
        return daylist;
    }

}