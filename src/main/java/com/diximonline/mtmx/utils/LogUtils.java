package com.diximonline.mtmx.utils;

import lombok.extern.slf4j.Slf4j;

public class LogUtils {
    @Slf4j(topic = "com.payload.diximonline")
    public static class PayloadTrace {
        public static void write(String prefix, String payload) { log.trace("{}, payload: {}", prefix, payload); }
    }
}