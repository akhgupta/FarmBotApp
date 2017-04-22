package com.farm.bot;


class InComingCallEvent {
    private final String callId;

    public InComingCallEvent(String callId) {
        this.callId=callId;
    }

    public String getCallId() {
        return callId;
    }
}
