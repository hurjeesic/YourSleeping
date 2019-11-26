package com.example.yoursleeping.support;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class DeletedSampleRequest extends StringRequest {
    final static private String URL = "/data/delete/";
    private Map<String, String> parameters;

    public DeletedSampleRequest(String baseUrl, Response.Listener<String> listener) {
        super(Method.GET, baseUrl + URL, listener, null);

        parameters = new HashMap<>();
    }

    @Override
    protected Map<String, String> getParams() {
        return parameters;
    }
}
