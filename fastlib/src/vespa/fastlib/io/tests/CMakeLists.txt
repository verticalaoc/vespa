# Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_add_executable(fastlib_bufferedstreamtest_app TEST
    SOURCES
    bufferedstreamtest.cpp
    DEPENDS
    fastlib_io
)
vespa_add_test(NAME fastlib_bufferedstreamtest_app COMMAND fastlib_bufferedstreamtest_app fastlib_bufferedstreamtest_app)
vespa_add_executable(fastlib_bufferedfiletest_app TEST
    SOURCES
    bufferedfiletest.cpp
    DEPENDS
    fastlib_io
)
vespa_add_test(NAME fastlib_bufferedfiletest_app COMMAND fastlib_bufferedfiletest_app)
