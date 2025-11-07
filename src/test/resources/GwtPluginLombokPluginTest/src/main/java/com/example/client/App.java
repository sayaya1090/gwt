package com.example.client;

import com.google.gwt.core.client.EntryPoint;

import static com.google.gwt.core.client.GWT.log;

public class App implements EntryPoint {
    private final TestMessage message = new TestMessage();
    public void onModuleLoad() { log(message.getMessage()); }
}