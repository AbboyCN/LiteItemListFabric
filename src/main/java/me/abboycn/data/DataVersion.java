// me/abboycn/data/DataVersion.java
package me.abboycn.data;

public class DataVersion {
    public static final int CURRENT_VERSION = 2;
    public static final int MIN_SUPPORTED_VERSION = 1;
    public static final int MAX_SUPPORTED_VERSION = 2;

    public static boolean isSupported(int version) {
        return version >= MIN_SUPPORTED_VERSION && version <= MAX_SUPPORTED_VERSION;
    }

    public static boolean isCurrent(int version) {
        return version == CURRENT_VERSION;
    }
}