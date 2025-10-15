package org.example.ai_analyzer.model;

import java.util.Map;

public class ReviewResponse {
    private String title;
    private Map<String, Object> data;

    public ReviewResponse(String title, Map<String, Object> data) {
        this.title = title;
        this.data = data;
    }

    public String getTitle() { return title; }
    public Map<String, Object> getData() { return data; }
}

