package com.example.yoursleeping;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import com.example.yoursleeping.support.SendingSampleRequest;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
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

public class SmartAlarmFragment extends AbstractChartFragment {
    private String TAG = "miband";
    private static boolean bUpdate = false;
    private static int initCount = 0, Count = 0;
    private static long presentTime = 0;
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

    private LineChart mActivityChart;
    private PieChart mSleepAmountChart;
    private TextView mSleepchartInfo;

    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;

    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Prefs prefs = GBApplication.getPrefs();
        List<? extends ActivitySample> samples;
        if (prefs.getBoolean("chart_sleep_range_24h", false)) {
            samples = getSamples(db, device);
        }else{
            samples = getSamplesofSleep(db, device);
        }

        if (presentTime == 0) presentTime = samples.get(0).getTimestamp();
        if (!bUpdate) {
            String DATE_PREV_DAY = ChartsActivity.class.getName().concat(".date_prev_day");
            String DATE_NEXT_DAY = ChartsActivity.class.getName().concat(".date_next_day");
            //54540 - timestamp에서 하루 차이
            if (samples.size() > 0 && presentTime - samples.get(0).getTimestamp() < 54540 * 60) {
                for (ActivitySample sample : samples) {
                    String dateStr = DateTimeUtils.formatDateTime(DateTimeUtils.parseTimeStamp(sample.getTimestamp()));
                    Log.d("miband", "time = " + dateStr + ", state = " + sample.getKind());
                    if (sample.getKind() == ActivityKind.TYPE_SLEEP) {
                        // 전송 코드 추가
                    }
                }
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(DATE_PREV_DAY));
            }
            else {
                bUpdate = true;
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(DATE_NEXT_DAY));
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
            mSleepAmountChart.setCenterText(pieData.getTotalSleep());
            mSleepAmountChart.setData(pieData.getPieData());

            mActivityChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
            mActivityChart.getXAxis().setValueFormatter(mcd.getChartsData().getXValueFormatter());
            mActivityChart.setData(mcd.getChartsData().getData());

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

        mActivityChart = rootView.findViewById(R.id.sleepchart);
        mSleepAmountChart = rootView.findViewById(R.id.sleepchart_pie_light_deep);
        mSleepchartInfo = rootView.findViewById(R.id.sleepchart_info);

        setupActivityChart();
        setupSleepAmountChart();

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ChartsHost.REFRESH)) {
            // TODO: use LimitLines to visualize smart alarms?
            mSmartAlarmFrom = intent.getIntExtra("smartalarm_from", -1);
            mSmartAlarmTo = intent.getIntExtra("smartalarm_to", -1);
            mTimestampFrom = intent.getIntExtra("recording_base_timestamp", -1);
            mSmartAlarmGoneOff = intent.getIntExtra("alarm_gone_off", -1);
            refresh();
        } else {
            super.onReceive(context, intent);
        }
    }

    private void setupSleepAmountChart() {
        mSleepAmountChart.setBackgroundColor(BACKGROUND_COLOR);
        mSleepAmountChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mSleepAmountChart.setEntryLabelColor(DESCRIPTION_COLOR);
        mSleepAmountChart.getDescription().setText("");
//        mSleepAmountChart.getDescription().setNoDataTextDescription("");
        mSleepAmountChart.setNoDataText("");
        mSleepAmountChart.getLegend().setEnabled(false);
    }

    private void setupActivityChart() {
        mActivityChart.setBackgroundColor(BACKGROUND_COLOR);
        mActivityChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        configureBarLineChartDefaults(mActivityChart);

        XAxis x = mActivityChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

        YAxis y = mActivityChart.getAxisLeft();
        y.setDrawGridLines(false);
//        y.setDrawLabels(false);
        // TODO: make fixed max value optional
        y.setAxisMaximum(1f);
        y.setAxisMinimum(0);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);

//        y.setLabelCount(5);
        y.setEnabled(true);

        YAxis yAxisRight = mActivityChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(supportsHeartrate(getChartsHost().getDevice()));
        yAxisRight.setDrawLabels(true);
        yAxisRight.setDrawTopYLabelEntry(true);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
        yAxisRight.setAxisMaxValue(HeartRateUtils.getInstance().getMaxHeartRate());
        yAxisRight.setAxisMinValue(HeartRateUtils.getInstance().getMinHeartRate());
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
        mActivityChart.animateX(ANIM_TIME, Easing.EaseInOutQuart);
        mSleepAmountChart.invalidate();
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
