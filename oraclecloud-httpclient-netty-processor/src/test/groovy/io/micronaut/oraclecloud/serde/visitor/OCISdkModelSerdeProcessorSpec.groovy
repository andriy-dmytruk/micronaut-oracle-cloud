package io.micronaut.oraclecloud.serde.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec

class OCISdkModelSerdeProcessorSpec extends AbstractTypeElementSpec {

    void "test"() {
        given:
        def context = buildContext("io.micronaut.oraclecloud.serde.SdkModels",'''
package io.micronaut.oraclecloud.serde;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Retention(SOURCE)
public @interface SdkModels {}

@SdkModels
class SdkModelsClass {}
''')

        expect:
        context != null
    }
}
