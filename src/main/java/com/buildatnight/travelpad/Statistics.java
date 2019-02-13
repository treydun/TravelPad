package com.buildatnight.travelpad;

import net.h31ix.travelpad.Travelpad;

import java.util.HashMap;

public class Statistics {

    private static HashMap<String, Integer> statsMap = new HashMap<>();

    public static void getStats(){
        for (String key:statsMap.keySet()){
            Travelpad.log(key+":"+statsMap.get(key));
        }
    }

    public static void tickStat(String key){
        Integer stat = statsMap.getOrDefault(key, 0);
        stat++;
        statsMap.put(key, stat);
    }
}
