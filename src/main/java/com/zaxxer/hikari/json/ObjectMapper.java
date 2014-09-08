/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2013-2014 Richard M. Hightower
 * Copyright 2014 Brett Wooldridge
 */

package com.zaxxer.hikari.json;


import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;

/**
 * This mapper (or, data binder, or codec) provides functionality for converting between Java objects
 * (instances of JDK provided core classes, beans), and matching JSON constructs.
 */
public interface ObjectMapper
{
    /**
     * Method to deserialize JSON content into a non-container
     * type typically a bean or wrapper type.
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected when using this method.
     * @param <T> the type of the value to deserialize
     * @param src a JSON string
     * @param valueType the type of the value to deserialize
     * @return the deserialized Java object
     */
    <T> T readValue(String src, Class<T> valueType);

    /**
     * Method to deserialize JSON content into a non-container
     * type typically a bean or wrapper type.
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected when using this method.
     * @param <T> the type of the value to deserialize
     * @param src a Reader providing JSON data
     * @param valueType the type of the value to deserialize
     * @return the deserialized Java object
     */
    <T> T readValue(InputStream src, Class<T> valueType);

    /**
     * Method to deserialize JSON content into a container like Set or List.
     *<p>
     * Note: this method should  be used if the result type is a
     * container ({@link java.util.Collection}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected without using this method.
     * @param <T> the Collection type to deserialize into
     * @param <C> the component type to deserialize into
     * @param src a JSON string
     * @param valueType the type of the Collection
     * @param componentType the type of the value to deserialize 
     * @return the deserialized Java object
     */
    <T extends Collection<C>, C> T readValue(String src, Class<T> valueType, Class<C> componentType);

    /**
     * Method to deserialize JSON content into a container like Set or List.
     *<p>
     * Note: this method should  be used if the result type is a
     * container ({@link java.util.Collection}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected without using this method.
     * @param <T> the Collection type to deserialize into
     * @param <C> the component type to deserialize into
     * @param src a Reader providing JSON data
     * @param valueType the type of the Collection
     * @param componentType the type of the value to deserialize 
     * @return the deserialized Java object
     */
    <T extends Collection<C>, C> T readValue(InputStream src, Class<T> valueType, Class<C> componentType);

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using Writer provided.
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here.
     * @param dest the destination Write to serialize the specified value to
     * @param value the value to serialize
     */
     void writeValue(Writer dest, Object value);

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     * @param value the value to serialize
     * @return a String containing the serialized value object
     */
     String writeValueAsString(Object value);

//     JsonParserAndMapper parser();

//     JsonSerializer serializer();
}