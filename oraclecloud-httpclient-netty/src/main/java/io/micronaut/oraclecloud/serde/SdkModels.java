/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oraclecloud.serde;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Internal annotation processor trigger for adding sdk model introspections.
 *
 * @author Andriy Dmytruk
 */
@Retention(CLASS)
public @interface SdkModels {
    /**
     * Provide a list of metadata classes. A metadata class lists models that require serialization
     * using the {@link com.oracle.bmc.SdkModels} annotation. The metadata class and models should
     * be added as an annotation processor dependency. If empty, the list will be deduced from
     * the oci-java-sdk-graalvm-addon.
     *
     * @return the list of metadata classes.
     */
    String[] sdkClientMetadataClasses() default {};
}
