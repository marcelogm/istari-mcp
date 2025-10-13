package com.marcelogm.istarimcp;

import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@MicronautTest
class DefaultTest {

    @Inject
    EmbeddedApplication<?> application;

    @Test
    @DisplayName("application running")
    void testItWorks() {
        Assertions.assertTrue(application.isRunning());
    }

}
