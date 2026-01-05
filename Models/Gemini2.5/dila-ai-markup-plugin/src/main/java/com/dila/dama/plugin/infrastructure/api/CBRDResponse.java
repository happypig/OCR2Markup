package com.dila.dama.plugin.infrastructure.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CBRDResponse {

    private final boolean success;
    private final List<String> found;
    private final String error;

    public CBRDResponse(boolean success, List<String> found, String error) {
        this.success = success;
        this.found = found != null ? Collections.unmodifiableList(new ArrayList<>(found)) : Collections.<String>emptyList();
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getFound() {
        return found;
    }

    public String getError() {
        return error;
    }

    public boolean hasResults() {
        return found != null && !found.isEmpty();
    }

    public String getFirstUrl() {
        return hasResults() ? found.get(0) : null;
    }

    public static CBRDResponse fromJson(String json) {
        JSONObject obj = new JSONObject(json);
        boolean success = obj.optBoolean("success", false);

        List<String> found = new ArrayList<>();
        JSONArray arr = obj.optJSONArray("found");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String url = arr.optString(i, null);
                if (url != null) {
                    found.add(url);
                }
            }
        }

        String error = obj.optString("error", null);
        return new CBRDResponse(success, found, error);
    }
}

