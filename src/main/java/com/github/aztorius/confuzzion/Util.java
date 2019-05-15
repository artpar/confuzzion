package com.github.aztorius.confuzzion;

import soot.Local;
import soot.RefType;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.HashMap;

public class Util {
    private static HashMap<String, String> childMap;

    static {
        childMap = new HashMap<String, String>();
        childMap.put("java.util.concurrent.BlockingQueue",
                     "java.util.concurrent.ArrayBlockingQueue");
    }

    public static String abstractToConcrete(String className) {
        return childMap.get(className);
    }
}
