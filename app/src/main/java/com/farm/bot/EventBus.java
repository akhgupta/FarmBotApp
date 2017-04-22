package com.farm.bot;


import com.squareup.otto.Bus;

public class EventBus {
    private static EventBus instance;
    private Bus bus;

    private EventBus() {
        bus = new Bus();
    }

    public static EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    public Bus getBus() {
        return bus;
    }
}
