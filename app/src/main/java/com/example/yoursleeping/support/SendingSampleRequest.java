package com.example.yoursleeping.support;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class SendingSampleRequest extends StringRequest {
    final static private String URL = "/data/add/";
    private Map<String, String> parameters;

    public SendingSampleRequest(String baseUrl, int date, int time, int heartRate, int sleepType, int startedSleep, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(Method.POST, baseUrl + URL, listener, errorListener);

        parameters = new HashMap<>();
        parameters.put("date", Integer.toString(date));
        parameters.put("time", Integer.toString(time));
        parameters.put("heart_rate", Integer.toString(heartRate)); // heartRate
        parameters.put("type", Integer.toString(sleepType)); // sleepType - 2는 얇은 수면, 4는 깊은 수면
        parameters.put("sleep_time", Integer.toString(startedSleep));
    }

    @Override
    protected Map<String, String> getParams() {
        return parameters;
    }
}
