/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.example.maven;

import java.util.Optional;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(role = ExecutionListener.class, hint = "otel-execution-listener")
public final class OtelExecutionListener extends AbstractExecutionListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(OtelExecutionListener.class);
    OpenTelemetrySdk openTelemetrySdk;

    @Override
    public void sessionStarted(ExecutionEvent event) {
        LOGGER.warn("Started...");
        System.out.println(getClass().getName() + "#sessionStarted...");

        AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk = AutoConfiguredOpenTelemetrySdk.builder()
                .setServiceClassLoader(getClass().getClassLoader())
                .build();
        openTelemetrySdk = autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk();
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        LOGGER.warn("Shutdown...");
        openTelemetrySdk.close();
    }

    @Override
    public void mojoStarted(ExecutionEvent executionEvent) {
        MojoExecution mojoExecution = executionEvent.getMojoExecution();

        LOGGER.warn("Context: " + Context.current());
        ContextStorage contextStorage = ContextStorage.get();
        Class<? extends ContextStorage> contextStorageClass = contextStorage.getClass();
        // logger.warn("Span: " + mojoExecuteSpan);
        LOGGER.warn("ContextStorage: " + contextStorage + " - " + contextStorageClass + "@"
                + System.identityHashCode(contextStorage));
        LOGGER.warn("ContextStorage class " + System.identityHashCode(contextStorageClass) + " loaded "
                + " by: "
                + contextStorageClass.getClassLoader() + " from: "
                + Optional.of(contextStorageClass.getProtectionDomain().getCodeSource())
                        .map(source -> source.getLocation().toString())
                        .orElse("#unknown#"));

        String spanName = mojoExecution.getArtifactId() + ":" + mojoExecution.getGoal();

        Tracer tracer = openTelemetrySdk.getTracer("com.example.maven.opentelemetry-maven-extension");
        Span span = tracer.spanBuilder(spanName).startSpan();
        Scope scope = span.makeCurrent();
        THREAD_LOCAL_SCOPE.set(scope);
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        Span.current().end();
        THREAD_LOCAL_SCOPE.get().close();
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        Span.current().end();
        THREAD_LOCAL_SCOPE.get().close();
    }

    static final ThreadLocal<Scope> THREAD_LOCAL_SCOPE = new ThreadLocal<>();

    /**
     * Register in given {@link OtelExecutionListener} to the lifecycle of the given {@link
     * MavenSession}
     *
     * @see org.apache.maven.execution.MavenExecutionRequest#setExecutionListener(ExecutionListener)
     */
    public static void registerOtelExecutionListener(
            MavenSession session, OtelExecutionListener otelExecutionListener) {

        ExecutionListener initialExecutionListener = session.getRequest().getExecutionListener();
        if (initialExecutionListener instanceof ChainedExecutionListener
                || initialExecutionListener instanceof OtelExecutionListener) {
            // already initialized
            LOGGER.debug(
                    "OpenTelemetry: OpenTelemetry extension already registered as execution listener, skip.");
        } else if (initialExecutionListener == null) {
            session.getRequest().setExecutionListener(otelExecutionListener);
            LOGGER.debug(
                    "OpenTelemetry: OpenTelemetry extension registered as execution listener. No execution listener initially defined");
        } else {
            session
                    .getRequest()
                    .setExecutionListener(
                            new ChainedExecutionListener(otelExecutionListener, initialExecutionListener));
            LOGGER.debug(
                    "OpenTelemetry: OpenTelemetry extension registered as execution listener. InitialExecutionListener: "
                            + initialExecutionListener);
        }
    }
}
