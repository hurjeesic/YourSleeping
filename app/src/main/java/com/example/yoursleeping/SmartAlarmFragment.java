package com.example.yoursleeping;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.example.yoursleeping.support.DeletedSampleRequest;
import com.example.yoursleeping.support.SendingSampleRequest;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.AbstractChartFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivitySleepChartFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsData;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsHost;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static android.content.Context.ALARM_SERVICE;

public class SmartAlarmFragment extends AbstractChartFragment {
    private String TAG = "miband";
    private static boolean bUpdate = false;
    private static int initCount = 0, Count = 0;
    private static long presentTime = 0, sendedDelay = 0;
    private static List<SendingData> dataLst = new ArrayList<>();
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);
    AlarmManager alarm_manager;
    TimePicker alarm_timepicker;
    Context context;
    PendingIntent pendingIntent;
    private TextView mSleepchartInfo;

    class SendingData {
        public int date, time, heartRate, state;

        public SendingData(int date, int time, int heartRate, int state) {
            this.date = date;
            this.time = time;
            this.heartRate = heartRate;
            this.state = state;
        }
    }

    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Prefs prefs = GBApplication.getPrefs();
        List<? extends ActivitySample> samples;
        if (prefs.getBoolean("chart_sleep_range_24h", false)) {
            samples = getSamples(db, device);
        }else{
            samples = getSamplesofSleep(db, device);
        }

        if (presentTime == 0 && samples.size() > 0) presentTime = samples.get(0).getTimestamp();
        if (!bUpdate) {
            String DATE_PREV_DAY = ChartsActivity.class.getName().concat(".date_prev_day");
            String DATE_NEXT_DAY = ChartsActivity.class.getName().concat(".date_next_day");
            //54540 - timestamp에서 하루 차이
            if (samples.size() > 0 && presentTime - samples.get(0).getTimestamp() < 54540 * 60) {
                for (int i = samples.size() - 1; i >= 0; i--) {
                    String[] dateInfo = DateTimeUtils.formatDateTime(DateTimeUtils.parseTimeStamp(samples.get(i).getTimestamp())).split(" ");
                    int year = Calendar.getInstance().get(Calendar.YEAR);
                    if (Integer.parseInt(dateInfo[0].substring(0, dateInfo[0].length() - 1)) > Calendar.getInstance().get(Calendar.MONTH) + 1) {
                        year--;
                    }
                    int date = Integer.parseInt(year + dateInfo[0].substring(0, dateInfo[0].length() - 1) + dateInfo[1].substring(0, dateInfo[1].length() - 1));
                    String[] timeAry = dateInfo[3].split(":");
                    int hour = Integer.parseInt(timeAry[0]), minute = Integer.parseInt(timeAry[1]);
                    if (Integer.parseInt(timeAry[0]) == 12) {
                        if (dateInfo[2].equals("오전")) {
                            hour = 0;
                        }
                    }
                    else if (dateInfo[2].equals("오후")) {
                        hour += 12;
                    }
                    int time = hour * 60 + minute;
                    if (dataLst.size() > 0 && date == dataLst.get(dataLst.size() - 1).date && time == dataLst.get(dataLst.size() - 1).time) {
                        continue;
                    }
                    else {
                        dataLst.add(new SendingData(date, time, samples.get(i).getHeartRate(), samples.get(i).getKind()));
                    }
                }
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(DATE_PREV_DAY));
            }
            else {
                bUpdate = true;
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(DATE_NEXT_DAY));

                Collections.sort(dataLst, new Comparator<SendingData>() {
                    @Override
                    public int compare(SendingData data1, SendingData data2) {
                        if (data1.date == data2.date) {
                            if (data1.time == data2.time) {
                                return 0;
                            }
                            else if (data1.time > data2.time) {
                                return -1;
                            }
                            else {
                                return 1;
                            }
                        }
                        else if (data1.date > data2.date) {
                            return -1;
                        }
                        else {
                            return 1;
                        }
                    }
                });

                final Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success"); // 전송 성공 - 단 수시로 일어나므로 다른 동작 x
                            boolean completed = jsonResponse.getBoolean("completed");
                            if (success) {
                                if (completed) {
                                    Log.d(TAG, "전송 완료");
                                }
                            }
                            else {
                                Log.d(TAG, "Fail sending");
                            }
                        }
                        catch (Exception e)  {
                            e.printStackTrace();
                        }
                    }
                };

                final Response.ErrorListener errorListener = new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error != null && error.networkResponse != null) {
                            Log.d(TAG, "" + error.networkResponse.statusCode);
                        }
                    }
                };

                final String url = "http://yoursleeping.pythonanywhere.com";
                Log.d(TAG, "전송 시작");
                final RequestQueue queue = Volley.newRequestQueue(getActivity());
                Response.Listener<String> deleteResponseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success"); // 전송 성공 - 단 수시로 일어나므로 다른 동작 x
                            if (success) {
                                int startedSleep = 0;
                                int beforeTime = 0;
                                for (int i = 0; i < dataLst.size(); i++) {
                                    final SendingData tempData = dataLst.get(i);
                                    if (tempData.state == ActivityKind.TYPE_ACTIVITY || tempData.state == ActivityKind.TYPE_DEEP_SLEEP || tempData.state == ActivityKind.TYPE_LIGHT_SLEEP) {
//                                        String timeStr = tempData.time / 60 + ":" + tempData.time % 60;
//                                        Log.d(TAG, "Data : " + tempData.date + " - " + timeStr + " : Heart Rate = " + tempData.heartRate + ", State = " + tempData.state);

                                        if (!(beforeTime - tempData.time != 1 || tempData.time - beforeTime != 1439) && (tempData.state == ActivityKind.TYPE_DEEP_SLEEP || tempData.state == ActivityKind.TYPE_LIGHT_SLEEP)) {
                                            startedSleep = tempData.time;
                                        }

                                        beforeTime = tempData.time;

                                        final int sendedStarting = startedSleep;
                                        final Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                SendingSampleRequest request = new SendingSampleRequest(url, tempData.date, tempData.time, tempData.heartRate, tempData.state, sendedStarting, responseListener, errorListener);
                                                queue.add(request);
                                            }
                                        }, sendedDelay);
                                        sendedDelay += 10;
                                    }
                                }
                            }
                            else {
                                Log.d(TAG, "Fail deleting");
                            }
                        }
                        catch (Exception e)  {
                            e.printStackTrace();
                        }
                    }
                };

                DeletedSampleRequest deletedRequest = new DeletedSampleRequest(url, deleteResponseListener);
                queue.add(deletedRequest);
            }
        }
        else if (initCount < 60){
            if (samples.get(0).getTimestamp() == presentTime) {
                initCount = 60;
            }

            String DATE_NEXT_DAY = ChartsActivity.class.getName().concat(".date_next_day");
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(DATE_NEXT_DAY));
            initCount++;
        }

        SmartAlarmFragment.MySleepChartsData mySleepChartsData = refreshSleepAmounts(device, samples);

        if (!prefs.getBoolean("chart_sleep_range_24h", false)) {
            if (mySleepChartsData.sleepSessions.size() > 0) {
                long tstart = mySleepChartsData.sleepSessions.get(0).getSleepStart().getTime() / 1000;
                long tend = mySleepChartsData.sleepSessions.get(mySleepChartsData.sleepSessions.size() - 1).getSleepEnd().getTime() / 1000;

                for (Iterator<ActivitySample> iterator = (Iterator<ActivitySample>) samples.iterator(); iterator.hasNext(); ) {
                    ActivitySample sample = iterator.next();
                    if (sample.getTimestamp() < tstart || sample.getTimestamp() > tend) {
                        iterator.remove();
                    }
                }
            }
        }
        AbstractChartFragment.DefaultChartsData chartsData = refresh(device, samples);

        return new SmartAlarmFragment.MyChartsData(mySleepChartsData, chartsData);
    }

    private SmartAlarmFragment.MySleepChartsData refreshSleepAmounts(GBDevice mGBDevice, List<? extends ActivitySample> samples) {
        SleepAnalysis sleepAnalysis = new SleepAnalysis();
        List<SleepAnalysis.SleepSession> sleepSessions = sleepAnalysis.calculateSleepSessions(samples);

        PieData data = new PieData();

        final long lightSleepDuration = calculateLightSleepDuration(sleepSessions);
        final long deepSleepDuration = calculateDeepSleepDuration(sleepSessions);

        final long totalSeconds = lightSleepDuration + deepSleepDuration;

        final List<PieEntry> entries;
        final List<Integer> colors;

        if (sleepSessions.isEmpty()) {
            entries = Collections.emptyList();
            colors = Collections.emptyList();
        } else {
            entries = Arrays.asList(
                    new PieEntry(lightSleepDuration, getActivity().getString(R.string.abstract_chart_fragment_kind_light_sleep)),
                    new PieEntry(deepSleepDuration, getActivity().getString(R.string.abstract_chart_fragment_kind_deep_sleep))
            );
            colors = Arrays.asList(
                    getColorFor(ActivityKind.TYPE_LIGHT_SLEEP),
                    getColorFor(ActivityKind.TYPE_DEEP_SLEEP)
            );
        }


        String totalSleep = DateTimeUtils.formatDurationHoursMinutes(totalSeconds, TimeUnit.SECONDS);
        PieDataSet set = new PieDataSet(entries, "");
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.SECONDS);
            }
        });
        set.setColors(colors);
        set.setValueTextColor(DESCRIPTION_COLOR);
        set.setValueTextSize(13f);
        set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        data.setDataSet(set);

        //setupLegend(pieChart);
        return new SmartAlarmFragment.MySleepChartsData(totalSleep, data, sleepSessions);
    }

    private long calculateLightSleepDuration(List<SleepAnalysis.SleepSession> sleepSessions) {
        long result = 0;
        for (SleepAnalysis.SleepSession sleepSession : sleepSessions) {
            result += sleepSession.getLightSleepDuration();
        }
        return result;
    }

    private long calculateDeepSleepDuration(List<SleepAnalysis.SleepSession> sleepSessions) {
        long result = 0;
        for (SleepAnalysis.SleepSession sleepSession : sleepSessions) {
            result += sleepSession.getDeepSleepDuration();
        }
        return result;
    }

    @Override
    protected void updateChartsnUIThread(ChartsData chartsData) {
        SmartAlarmFragment.MyChartsData mcd = (SmartAlarmFragment.MyChartsData) chartsData;
        if (mcd != null) {
            SmartAlarmFragment.MySleepChartsData pieData = mcd.getPieData();

            mSleepchartInfo.setText(buildYouSleptText(pieData));
        }
        else {
            bUpdate = true;
        }
    }

    private String buildYouSleptText(SmartAlarmFragment.MySleepChartsData pieData) {
        final StringBuilder result = new StringBuilder();
        if (pieData.getSleepSessions().isEmpty()) {
            result.append(getContext().getString(R.string.you_did_not_sleep));
        } else {
            for (SleepAnalysis.SleepSession sleepSession : pieData.getSleepSessions()) {
                result.append(getContext().getString(
                        R.string.you_slept,
                        DateTimeUtils.timeToString(sleepSession.getSleepStart()),
                        DateTimeUtils.timeToString(sleepSession.getSleepEnd())));
                result.append('\n');
            }
        }
        return result.toString();
    }

    @Override
    public String getTitle() {
        return getString(R.string.sleepchart_your_sleep);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_third, container, false);

        bUpdate = false;

        mSleepchartInfo = rootView.findViewById(R.id.sleepchart_info);

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();
//        Alarm_set(context, rootView);
        return rootView;
    }
    public void Alarm_set(Context context, View view){
        alarm_manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        // 타임피커 설정
        alarm_timepicker = view.findViewById(R.id.time_picker);

        // Calendar 객체 생성
        final Calendar calendar = Calendar.getInstance();

        // 알람리시버 intent 생성
        final Intent my_intent = new Intent(context, Alarm_Reciver.class);
        final Context tempContext = context;
        // 알람 시작 버튼
        Button alarm_on = view.findViewById(R.id.btn_start);
        alarm_on.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {

                // calendar에 시간 셋팅
                calendar.set(Calendar.HOUR_OF_DAY, alarm_timepicker.getHour());
                calendar.set(Calendar.MINUTE, alarm_timepicker.getMinute());

                // 시간 가져옴
                int hour = alarm_timepicker.getHour();
                int minute = alarm_timepicker.getMinute();
                Toast.makeText(tempContext,"Alarm 예정 " + hour + "시 " + minute + "분",Toast.LENGTH_SHORT).show();

                // reveiver에 string 값 넘겨주기
                my_intent.putExtra("state","alarm on");

                pendingIntent = PendingIntent.getBroadcast(tempContext, 0, my_intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                // 알람셋팅
                alarm_manager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        pendingIntent);
            }
        });

        // 알람 정지 버튼
        Button alarm_off = view.findViewById(R.id.btn_finish);
        alarm_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(tempContext,"Alarm 종료",Toast.LENGTH_SHORT).show();
                // 알람매니저 취소
                alarm_manager.cancel(pendingIntent);

                my_intent.putExtra("state","alarm off");

                // 알람취소
                tempContext.sendBroadcast(my_intent);
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ChartsHost.REFRESH)) {
            refresh();
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    protected void setupLegend(Chart chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(3);
        LegendEntry lightSleepEntry = new LegendEntry();
        lightSleepEntry.label = akLightSleep.label;
        lightSleepEntry.formColor = akLightSleep.color;
        legendEntries.add(lightSleepEntry);

        LegendEntry deepSleepEntry = new LegendEntry();
        deepSleepEntry.label = akDeepSleep.label;
        deepSleepEntry.formColor = akDeepSleep.color;
        legendEntries.add(deepSleepEntry);

        if (supportsHeartrate(getChartsHost().getDevice())) {
            LegendEntry hrEntry = new LegendEntry();
            hrEntry.label = HEARTRATE_LABEL;
            hrEntry.formColor = HEARTRATE_COLOR;
            legendEntries.add(hrEntry);
        }
        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
// temporary fix for totally wrong sleep amounts
//        return super.getSleepSamples(db, device, tsFrom, tsTo);
        return super.getAllSamples(db, device, tsFrom, tsTo);
    }

    @Override
    protected void renderCharts() {

    }

    private static class MySleepChartsData extends ChartsData {
        private String totalSleep;
        private final PieData pieData;
        private final List<SleepAnalysis.SleepSession> sleepSessions;

        public MySleepChartsData(String totalSleep, PieData pieData, List<SleepAnalysis.SleepSession> sleepSessions) {
            this.totalSleep = totalSleep;
            this.pieData = pieData;
            this.sleepSessions = sleepSessions;
        }

        public PieData getPieData() {
            return pieData;
        }

        public CharSequence getTotalSleep() {
            return totalSleep;
        }

        public List<SleepAnalysis.SleepSession> getSleepSessions() {
            return sleepSessions;
        }
    }

    private static class MyChartsData extends ChartsData {
        private final AbstractChartFragment.DefaultChartsData<LineData> chartsData;
        private final SmartAlarmFragment.MySleepChartsData pieData;

        public MyChartsData(SmartAlarmFragment.MySleepChartsData pieData, AbstractChartFragment.DefaultChartsData<LineData> chartsData) {
            this.pieData = pieData;
            this.chartsData = chartsData;
        }

        public SmartAlarmFragment.MySleepChartsData getPieData() {
            return pieData;
        }

        public AbstractChartFragment.DefaultChartsData<LineData> getChartsData() {
            return chartsData;
        }
    }
}
