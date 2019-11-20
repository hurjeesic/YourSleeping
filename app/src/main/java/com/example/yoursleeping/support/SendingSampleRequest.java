package com.example.yoursleeping.support;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class SendingSampleRequest extends StringRequest {
    final static private String URL = "http://yoursleeping.pythonanywhere.com/data/add/";
    private Map<String, String> parameters;

    public SendingSampleRequest(int date, int time, int heartRate, int sleepType, Response.Listener<String> listener) {
        super(Method.POST, URL, listener, null);

        parameters = new HashMap<>();
        parameters.put("date", Integer.toString(date));
        parameters.put("time", Integer.toString(time));
        parameters.put("heartrate", Integer.toString(heartRate)); // heartRate
        parameters.put("type", Integer.toString(sleepType)); // sleepType - 2는 얇은 수면, 4는 깊은 수면
    }

    @Override
    protected Map<String, String> getParams() {
        return parameters;
    }
}
