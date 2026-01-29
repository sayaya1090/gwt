package com.example.client;
import com.google.gwt.core.client.EntryPoint;
import elemental2.dom.DomGlobal;
import lombok.Getter;

@Getter public class AppTest implements EntryPoint {
    private final String message = "Hello from Main";
    public void onModuleLoad() { DomGlobal.console.log(message); }
}