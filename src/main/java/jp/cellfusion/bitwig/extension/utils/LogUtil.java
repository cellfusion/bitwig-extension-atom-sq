package jp.cellfusion.bitwig.extension.utils;

import com.bitwig.extension.controller.api.ControllerHost;

public class LogUtil {
    private static ControllerHost _host = null;

    public static void init(ControllerHost host) {
        _host = host;
    }

    public static void print(String s) {
        _host.println(s);
    }

    public static void println(String s) {
        _host.println(s);
    }
}
