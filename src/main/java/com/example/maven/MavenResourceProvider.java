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

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.rtinfo.internal.DefaultRuntimeInformation;

public class MavenResourceProvider implements ResourceProvider {
    @Override
    public Resource createResource(ConfigProperties config) {
        // TODO verify if there is solution to retrieve the RuntimeInformation instance loaded by the
        //  Maven Plexus Launcher
        RuntimeInformation runtimeInformation = new DefaultRuntimeInformation();
        return Resource.builder()
                .put(ResourceAttributes.SERVICE_NAME, "maven")
                .put(ResourceAttributes.SERVICE_VERSION, runtimeInformation.getMavenVersion())
                .build();
    }
}
