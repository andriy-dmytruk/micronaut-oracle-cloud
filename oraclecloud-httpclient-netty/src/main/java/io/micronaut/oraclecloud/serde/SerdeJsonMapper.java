/*
 * Copyright 2017-2023 original authors
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

import com.oracle.bmc.http.client.JsonMapper;
import io.micronaut.serde.ObjectMapper;

import java.io.IOException;

public class SerdeJsonMapper implements JsonMapper {

    private final ObjectMapper objectMapper;

    public SerdeJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T readValue(String s, Class<T> aClass) throws IOException {
        return objectMapper.readValue(s, aClass);
    }

    @Override
    public <T> T readValue(byte[] bytes, Class<T> aClass) throws IOException {
        return objectMapper.readValue(bytes, aClass);
    }

    @Override
    public String writeValueAsString(Object o) throws IOException {
        return objectMapper.writeValueAsString(o);
    }
}
