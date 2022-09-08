package com.gitb.engine.remote;

import java.util.Properties;

public final class RemoteCallContext {

    private static final ThreadLocal<Properties> tl = new ThreadLocal<>();

    private RemoteCallContext() {}

    public static void setCallProperties(Properties properties) {
        tl.set(properties);
    }

    public static Properties getCallProperties() {
        return tl.get();
    }

    public static void clearCallProperties() {
        tl.remove();
    }

}
