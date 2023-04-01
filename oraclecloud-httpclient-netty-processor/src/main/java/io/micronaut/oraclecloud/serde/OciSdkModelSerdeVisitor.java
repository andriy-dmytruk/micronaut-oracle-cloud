/*
 * Copyright 2017-2020 original authors
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

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.serde.annotation.SerdeImport;
import io.micronaut.serde.config.annotation.SerdeConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Type element visitor vising oci sdk models and enums.
 *
 * @author Andriy Dmytruk
 * @since 4.0.0
 */
@Internal
public class OciSdkModelSerdeVisitor implements TypeElementVisitor<Object, Object> {

    private static final String ANN_SERDE_CONFIG = "io.micronaut.serde.config.annotation.SerdeConfig";
    private static final String OCI_SDK_MODEL_CLASS_NAME = "com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel";
    private static final String OCI_SDK_ENUM_CLASS_NAME = "com.oracle.bmc.http.internal.BmcEnum";

    private static final String ANN_SDK_MODELS = "io.micronaut.oraclecloud.serde.SdkModels";
    private static final String OCI_SDK_MODELS_ANN = "com.oracle.bmc.SdkModels";
    private static final String AUTOMATIC_FEATURE_METADATA_CLASS = "com.oracle.bmc.graalvm.SdkAutomaticFeatureMetadata";
    private static final String AUTOMATIC_FEATURE_METADATA_ANN = "com.oracle.bmc.graalvm.SdkClientPackages";

    @Override
    public int getOrder() {
        return 10; // Should run before SerdeAnnotationVisitor
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Collections.singleton(ANN_SDK_MODELS);
    }

    @NonNull
    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING; // Save as SerdeAnnotationVisitor
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!element.hasAnnotation(ANN_SDK_MODELS)) {
            return;
        }
        context.info("Visiting class5 : " + element);

        List<AnnotationValue<SerdeImport>> serdeImports = new ArrayList<>();
         String largestClass = null;
        int mostProperties = 0;
        List<ClassElement> requireSerialization = new ArrayList<>(getModelsForSerialization(element, context));
//        context.info("Classes for serialization: " + requireSerialization);
        context.info("Found " + requireSerialization.size() + " oci sdk classes for serialization");
        requireSerialization = requireSerialization.subList(0, 1000);

        for (ClassElement type: requireSerialization) {
            visitClassInternal(type, context);
            serdeImports.add(AnnotationValue.builder(SerdeImport.class)
                .member("value", new AnnotationClassValue<>(type.getName()))
                .build());
        }


        element.annotate(SerdeImport.Repeated.class, builder ->
            builder.values(serdeImports.toArray(new AnnotationValue[0]))
        );
        context.info("Largest class: " + largestClass + ", " + mostProperties);
        element.annotate(SerdeConfig.class);
    }

    private void visitClassInternal(ClassElement element, VisitorContext context) {
        if (isOciSdkModel(element)) {
            visitOciSdkModel(element, context);
        } else if (isOciSdkEnum(element)) {
            visitOciSdkEnum(element, context);
        }
    }

    private void visitOciSdkModel(ClassElement element, VisitorContext context) {
        // Ignore the micronaut serde errors, as @JsonDeserialize(builder=...) is not supported
        ignoreMicronautSerdeValidation(element);

        // Make fields and constructor parameters nullable
        element.getFields().forEach(OciSdkModelSerdeVisitor::makeNullable);
        element.getPrimaryConstructor()
            .map(constructor -> Arrays.stream(constructor.getParameters()))
            .ifPresent(parameters -> parameters.forEach(OciSdkModelSerdeVisitor::makeNullable));
    }

    private void visitOciSdkEnum(ClassElement element, VisitorContext context) {
        // Make the parameter of the creator nullable
        element.getPrimaryConstructor()
            .map(constructor -> Arrays.stream(constructor.getParameters()))
            .ifPresent(parameters -> parameters.forEach(OciSdkModelSerdeVisitor::makeNullable));
    }

    private static void makeNullable(TypedElement element) {
        if (!element.isNonNull() && !element.isDeclaredNullable()) {
            element.annotate(AnnotationUtil.NULLABLE);
        }
    }

    private void ignoreMicronautSerdeValidation(ClassElement element) {
        element.annotate(
            AnnotationValue.builder(ANN_SERDE_CONFIG)
                .member("validate", false)
                .build()
        );
    }

    private static boolean isOciSdkModel(ClassElement element) {
        Optional<ClassElement> parent = element.getSuperType();
        while (parent.isPresent()) {
            if (parent.get().getName().equals(OCI_SDK_MODEL_CLASS_NAME)) {
                return true;
            }
            parent = parent.get().getSuperType();
        }
        return false;
    }

    private static boolean isOciSdkEnum(ClassElement element) {
        return element.getInterfaces().stream()
            .anyMatch(i -> i.getName().equals(OCI_SDK_ENUM_CLASS_NAME));
    }

    private Collection<ClassElement> getMetadataClasses(ClassElement element, VisitorContext context) {
        String[] metadataClassNames = Optional.ofNullable(element.getAnnotation(ANN_SDK_MODELS))
            .flatMap(ann -> ann.get("sdkClientMetadataClasses", Argument.of(String[].class)))
            .orElse(new String[0]);

        if (metadataClassNames.length == 0) {
            metadataClassNames = context.getClassElement(AUTOMATIC_FEATURE_METADATA_CLASS)
                .flatMap(metadata -> Optional.ofNullable(metadata.getAnnotation(AUTOMATIC_FEATURE_METADATA_ANN)))
                .flatMap(packages -> packages.getValue(String[].class))
                .orElse(new String[0]);
            metadataClassNames = Arrays.copyOf(metadataClassNames, metadataClassNames.length);
        }

        return Arrays.stream(metadataClassNames)
            .map(context::getClassElement)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet()); // To avoid duplicate elements
    }

    public Set<ClassElement> getModelsForSerialization(ClassElement element, VisitorContext context) {
        Collection<ClassElement> metadataClasses = getMetadataClasses(element, context);
        Set<ClassElement> modelsForSerialization = new HashSet<>();

        for (ClassElement metadata: metadataClasses) {
            String[] modelClassNames =
                Optional.ofNullable(metadata.getAnnotation(OCI_SDK_MODELS_ANN))
                .flatMap(ann -> ann.getValue(String[].class))
                .orElse(new String[0]);

            Arrays.stream(modelClassNames)
                .map(context::getClassElement)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(modelsForSerialization::add);
        }

        return modelsForSerialization;
    }

    /**
     * Get all the classes that require serialization.
     * @return set of the classes
     */
    public Set<Class<?>> getClassesRequiredForSerialization() {
        try {
            Class<?> alarmClass = getClass().getClassLoader().loadClass("com.oracle.bmc.monitoring.model.Alarm");
            Class<?> alarmSeverityClass = getClass().getClassLoader().loadClass("com.oracle.bmc.monitoring.model.Alarm$Severity");
            Class<?> suppressionClass = getClass().getClassLoader().loadClass("com.oracle.bmc.monitoring.model.Suppression");
            Set<Class<?>> result = new HashSet<>();
            result.add(alarmClass);
            result.add(alarmSeverityClass);
            result.add(suppressionClass);
            result.add(getClass().getClassLoader().loadClass("com.oracle.bmc.marketplace.model.LinkEnum"));
            return result;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }
}
