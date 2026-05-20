package com.llm.ai.project.debuggingAI.common.util;

import java.net.InetAddress;

public class SystemInfo {
    public static String getDeviceName() {
        try {
            String deviceName = System.getenv("DEVICENAME");
            if (deviceName != null && !deviceName.isEmpty()) {
                return deviceName;
            }

            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Unknown-Device";
        }
    }

    public static String getOsName() {
        return System.getProperty("os.name");
    }
}
