package com.example.yoursleeping.support;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class SendingSampleRequest extends StringRequest {
    final static private String URL = "http://10.0.2.2/"; // 전송할 데이터 URL - 10.0.2.2는 localhost
    private Map<String, String> parameters;

    public SendingSampleRequest(int date, int time, int heartRate, int sleepType, Response.Listener<String> listener) {
        super(Method.POST, URL, listener, null);

        parameters = new HashMap<>();
        parameters.put("date", Integer.toString(date));
        parameters.put("time", Integer.toString(time));
        parameters.put("heartRate", Integer.toString(heartRate));
        parameters.put("sleepType", Integer.toString(sleepType)); // 2는 얇은 수면, 4는 깊은 수면
    }

    @Override
    protected Map<String, String> getParams() {
        return parameters;
    }
}
