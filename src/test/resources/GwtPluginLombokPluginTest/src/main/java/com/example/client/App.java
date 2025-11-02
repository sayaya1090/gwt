package com.example.client;

import com.google.gwt.core.client.EntryPoint;

public class App implements EntryPoint {
    private final TestMessage message = new TestMessage();
    public void onModuleLoad() { System.out.println(message.getMessage()); }
}