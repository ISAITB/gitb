package com.gitb.engine.utils;

import com.gitb.tdl.TestCase;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

public class ScriptletCache {

    private final ConcurrentHashMap<String, ScriptletInfo> cache = new ConcurrentHashMap<>();

    public ScriptletInfo getScriptlet(String from, String path, TestCase testCase, boolean required) {
        return cache.computeIfAbsent(toKey(from, path), (key) -> TestCaseUtils.lookupScriptlet(from, path, testCase, required));
    }

    private String toKey(String from, String path) {
        return StringUtils.defaultString(from)+"|"+path;
    }

}
