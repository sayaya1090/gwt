package com.example.client;
import com.google.gwt.core.client.EntryPoint;
import lombok.Getter;
@Getter public class App implements EntryPoint {
    private final String message = "Hello from Main";
    public void onModuleLoad() { System.out.println(message); }
}