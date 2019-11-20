package com.example.yoursleeping.support;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class DeletedSampleRequest extends StringRequest {
    final static private String URL = "http://yoursleeping.pythonanywhere.com/data/delete/";
    private Map<String, String> parameters;

    public DeletedSampleRequest(Response.Listener<String> listener) {
        super(Method.GET, URL, listener, null);

        parameters = new HashMap<>();
    }

    @Override
    protected Map<String, String> getParams() {
        return parameters;
    }
}
