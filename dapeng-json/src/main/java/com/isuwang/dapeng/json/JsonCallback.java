package com.isuwang.dapeng.json;

public interface JsonCallback {

    void onStartObject();
    void onEndObject();

    void onStartArray();
    void onEndArray();

    void onStartField(String name);
    void onEndField();

    void onBoolean(boolean value);
    void onNumber(double value);
    void onNull();

    void onString(String value);
}
