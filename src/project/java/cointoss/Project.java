/*
 * Copyright (C) 2021 cointoss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss;

public class Project extends bee.api.Project {

    {
        product("cointoss", "cointoss", ref("version.txt"));

        require("com.github.teletha", "sinobu");
        require("com.github.teletha", "viewtify");
        require("com.github.teletha", "icymanipulator").atAnnotation();
        require("com.github.teletha", "antibug").atTest();
        require("com.pgs-soft", "HttpClientMock").atTest();
        require("org.apache.commons", "commons-lang3");
        require("org.apache.commons", "commons-math3");
        require("commons-net", "commons-net", "3.7.2");
        require("org.apache.logging.log4j", "log4j-core");
        require("com.google.guava", "guava");
        unrequire("com.google.code.findbugs", "jsr305");
        unrequire("com.google.errorprone", "error_prone_annotations");
        unrequire("com.google.guava", "listenablefuture");
        unrequire("com.google.j2objc", "j2objc-annotations");
        unrequire("org.checkerframework", "checker-qual");
        require("com.univocity", "univocity-parsers");
        require("com.github.luben", "zstd-jni");
        require("ch.obermuhlner", "big-math");
        require("org.decimal4j", "decimal4j").atTest();

        versionControlSystem("https://github.com/teletha/cointoss");
    }
}