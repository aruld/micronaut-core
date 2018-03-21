/*
 * Copyright 2017 original authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package io.micronaut.inject.writer;

import io.micronaut.context.AbstractBeanDefinition;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.Executable;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.configuration.ConfigurationMetadataBuilder;
import org.objectweb.asm.Type;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.configuration.ConfigurationMetadataBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Interface for {@link BeanDefinitionVisitor} implementations such as {@link BeanDefinitionWriter}
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface BeanDefinitionVisitor {
    /**
     * The suffix use for generated AOP intercepted types
     */
    String PROXY_SUFFIX = "$Intercepted";

    /**
     * Visits a no arguments constructor. Either this method or {@link #visitBeanDefinitionConstructor(Map, Map, Map)} should be called at least once
     */
    void visitBeanDefinitionConstructor();

    /**
     * @return Whether the provided type an interface
     */
    boolean isInterface();

    /**
     * @return Is the bean singleton
     */
    boolean isSingleton();


    /**
     * Visit a marker interface on the generated bean definition
     *
     * @param interfaceType The interface type
     */
    void visitBeanDefinitionInterface(Class<? extends BeanDefinition> interfaceType);

    /**
     * Alter the super class of this bean definition. The passed class should be a subclass of {@link AbstractBeanDefinition}
     *
     * @param name The super type
     */
    void visitSuperBeanDefinition(String name);

    /**
     * Alter the super class of this bean definition to use another factory bean
     *
     * @param beanName The bean name
     */
    void visitSuperBeanDefinitionFactory(String beanName);

    /**
     * @return The full class name of the bean
     */
    String getBeanTypeName();

    /**
     * The provided type of the bean. Usually this is the same as {@link #getBeanTypeName()}, except in the case of factory beans
     * which produce a different type
     *
     * @return The provided type
     */
    Type getProvidedType();

    /**
     * Make the bean definition as validated by javax.validation
     *
     * @param validated Whether the bean definition is validated
     */
    void setValidated(boolean validated);

    /**
     * @return Return whether the bean definition is validated
     */
    boolean isValidated();

    /**
     * @return The name of the bean definition class
     */
    String getBeanDefinitionName();

    /**
     * Visits the constructor used to create the bean definition.
     *
     * @param argumentTypes  The argument type names for each parameter
     * @param qualifierTypes The qualifier type names for each parameter
     * @param genericTypes   The generic types for each parameter
     */
    void visitBeanDefinitionConstructor(Map<String, Object> argumentTypes,
                                        Map<String, Object> qualifierTypes,
                                        Map<String, Map<String, Object>> genericTypes);


    /**
     * Visits the constructor of the parent class used in the case a proxied bean definition
     *
     * @param argumentTypes  The argument type names for each parameter
     * @param qualifierTypes The qualifier type names for each parameter
     * @param genericTypes   The generic types for each parameter
     */
    void visitProxiedBeanDefinitionConstructor (
            Object declaringType,
            Map<String, Object> argumentTypes,
            Map<String, Object> qualifierTypes,
            Map<String, Map<String, Object>> genericTypes
    );
    /**
     * Finalize the bean definition to the given output stream
     */
    void visitBeanDefinitionEnd();

    /**
     * Write the state of the writer to the given compilation directory
     *
     * @param compilationDir The compilation directory
     * @throws IOException If an I/O error occurs
     */
    void writeTo(File compilationDir) throws IOException;

    /**
     * Write the class to output via a visitor that manages output destination
     *
     * @param visitor the writer output visitor
     * @throws IOException If an error occurs
     */
    void accept(ClassWriterOutputVisitor visitor) throws IOException;

    /**
     * Visits an injection point for a field and setter pairing.
     *
     * @param declaringType      The declaring type
     * @param qualifierType      The qualifier type
     * @param requiresReflection Whether the setter requires reflection
     * @param fieldType          The field type
     * @param fieldName          The field name
     * @param setterName         The setter name
     * @param genericTypes       The generic types
     */
    void visitSetterInjectionPoint(Object declaringType,
                                   Object qualifierType,
                                   boolean requiresReflection,
                                   Object fieldType,
                                   String fieldName,
                                   String setterName,
                                   Map<String, Object> genericTypes);

    /**
     * Visits an injection point for a field and setter pairing.
     *
     * @param declaringType      The declaring type
     * @param qualifierType      The qualifier type
     * @param requiresReflection Whether the setter requires reflection
     * @param fieldType          The field type
     * @param fieldName          The field name
     * @param setterName         The setter name
     * @param genericTypes       The generic types
     * @param isOptional         Whether the setter is optional
     */
    void visitSetterValue(Object declaringType,
                          Object qualifierType,
                          boolean requiresReflection,
                          Object fieldType,
                          String fieldName,
                          String setterName,
                          Map<String, Object> genericTypes,
                          boolean isOptional);


    /**
     * Visits an injection point for a setter.
     *
     * @param declaringType      The declaring type
     * @param qualifierType      The qualifier type
     * @param requiresReflection Whether the setter requires reflection
     * @param valueType          The field type
     * @param setterName         The setter name
     * @param genericTypes       The generic types
     * @param isOptional         Whether the setter is optional
     */
    void visitSetterValue(Object declaringType,
                          Object qualifierType,
                          boolean requiresReflection,
                          Object valueType,
                          String setterName,
                          Map<String, Object> genericTypes,
                          boolean isOptional);
    /**
     * Visits a method injection point
     *
     * @param declaringType      The declaring type of the method. Either a Class or a string representing the name of the type
     * @param requiresReflection Whether the method requires reflection
     * @param returnType         The return type of the method. Either a Class or a string representing the name of the type
     * @param methodName         The method name
     * @param argumentTypes      The argument types. Note: an ordered map should be used such as LinkedHashMap. Can be null or empty.
     * @param qualifierTypes     The qualifier types of each argument. Can be null.
     * @param genericTypes       The generic types of each argument. Can be null.
     */
    void visitPostConstructMethod(Object declaringType,
                                  boolean requiresReflection,
                                  Object returnType,
                                  String methodName,
                                  Map<String, Object> argumentTypes,
                                  Map<String, Object> qualifierTypes,
                                  Map<String, Map<String, Object>> genericTypes);

    /**
     * Visits a method injection point
     *
     * @param declaringType      The declaring type of the method. Either a Class or a string representing the name of the type
     * @param requiresReflection Whether the method requires reflection
     * @param returnType         The return type of the method. Either a Class or a string representing the name of the type
     * @param methodName         The method name
     * @param argumentTypes      The argument types. Note: an ordered map should be used such as LinkedHashMap. Can be null or empty.
     * @param qualifierTypes     The qualifier types of each argument. Can be null.
     * @param genericTypes       The generic types of each argument. Can be null.
     */
    void visitPreDestroyMethod(Object declaringType,
                               boolean requiresReflection,
                               Object returnType,
                               String methodName,
                               Map<String, Object> argumentTypes,
                               Map<String, Object> qualifierTypes,
                               Map<String, Map<String, Object>> genericTypes);

    /**
     * Visits a method injection point
     *
     * @param declaringType      The declaring type of the method. Either a Class or a string representing the name of the type
     * @param requiresReflection Whether the method requires reflection
     * @param returnType         The return type of the method. Either a Class or a string representing the name of the type
     * @param methodName         The method name
     * @param argumentTypes      The argument types. Note: an ordered map should be used such as LinkedHashMap. Can be null or empty.
     * @param qualifierTypes     The qualifier types of each argument. Can be null.
     * @param genericTypes       The generic types of each argument. Can be null.
     */
    void visitMethodInjectionPoint(Object declaringType,
                                   boolean requiresReflection,
                                   Object returnType,
                                   String methodName,
                                   Map<String, Object> argumentTypes,
                                   Map<String, Object> qualifierTypes,
                                   Map<String, Map<String, Object>> genericTypes);

    /**
     * Visit a method that is to be made executable allow invocation of said method without reflection
     *
     * @param declaringType  The declaring type of the method. Either a Class or a string representing the name of the type
     * @param returnType     The return type of the method. Either a Class or a string representing the name of the type
     * @param methodName     The method name
     * @param argumentTypes  The argument types. Note: an ordered map should be used such as LinkedHashMap. Can be null or empty.
     * @param qualifierTypes The qualifier types of each argument. Can be null.
     * @param genericTypes   The generic types of each argument. Can be null.
     * @param annotationMetadata The annotation metadata for the method
     * @return The {@link ExecutableMethodWriter}.
     */
    ExecutableMethodWriter visitExecutableMethod(Object declaringType,
                               Object returnType,
                               Object genericReturnType,
                               Map<String, Object> returnTypeGenericTypes,
                               String methodName,
                               Map<String, Object> argumentTypes,
                               Map<String, Object> qualifierTypes,
                               Map<String, Map<String, Object>> genericTypes,
                               AnnotationMetadata annotationMetadata);

    /**
     * Visits a field injection point
     *
     * @param declaringType      The declaring type. Either a Class or a string representing the name of the type
     * @param qualifierType      The qualifier type. Either a Class or a string representing the name of the type
     * @param requiresReflection Whether accessing the field requires reflection
     * @param fieldType          The type of the field
     * @param fieldName          The name of the field
     */
    void visitFieldInjectionPoint(Object declaringType,
                                  Object qualifierType,
                                  boolean requiresReflection,
                                  Object fieldType,
                                  String fieldName);

    /**
     * Visits a field injection point
     *
     * @param declaringType      The declaring type. Either a Class or a string representing the name of the type
     * @param qualifierType      The qualifier type. Either a Class or a string representing the name of the type
     * @param requiresReflection Whether accessing the field requires reflection
     * @param fieldType          The type of the field
     * @param fieldName          The name of the field
     */
    void visitFieldValue(Object declaringType,
                         Object qualifierType,
                         boolean requiresReflection,
                         Object fieldType,
                         String fieldName,
                         boolean isOptional);

    /**
     * @return The package name of the bean
     */
    String getPackageName();

    /**
     * @return The short name of the bean
     */
    String getBeanSimpleName();

    /**
     * @return The annotation metadata
     */
    AnnotationMetadata getAnnotationMetadata();

    /**
     * Begin defining a configuration builder
     *
     * @param type The type of the builder
     * @param field The name of the field that represents the builder
     * @param annotationMetadata The annotation metadata associated with the field
     * @param metadataBuilder The {@link ConfigurationMetadataBuilder}
     * @see ConfigurationBuilder
     */
    void visitConfigBuilderField(
            Object type,
            String field,
            AnnotationMetadata annotationMetadata,
            ConfigurationMetadataBuilder metadataBuilder);

    /**
     * Begin defining a configuration builder

     * @param type The type of the builder
     * @param methodName The name of the method that returns the builder
     * @param annotationMetadata The annotation metadata associated with the field
     * @param metadataBuilder The {@link ConfigurationMetadataBuilder}

     * @see ConfigurationBuilder
     */
    void visitConfigBuilderMethod(
            Object type,
            String methodName,
            AnnotationMetadata annotationMetadata,
            ConfigurationMetadataBuilder metadataBuilder);

    /**
     * Visit a configuration builder method
     *
     * @param prefix The prefix used for the method
     * @param configurationPrefix The prefix used to retrieve the configuration value
     * @param returnType The return type
     * @param methodName The method name
     * @param paramType The method type
     * @param generics The generic types of the method
     * @see ConfigurationBuilder
     */
    void visitConfigBuilderMethod(
            String prefix,
            String configurationPrefix,
            Object returnType,
            String methodName,
            Object paramType,
            Map<String, Object> generics);
    /**
     * Visit a configuration builder method that accepts a long and a TimeUnit
     *
     * @param prefix The prefix used for the method
     * @param configurationPrefix The prefix used to retrieve the configuration value
     * @param returnType The return type
     * @param methodName The method name
     * @see ConfigurationBuilder
     */
    void visitConfigBuilderDurationMethod(
            String prefix,
            String configurationPrefix,
            Object returnType,
            String methodName);
    /**
     * Finalize a configuration builder field
     *
     * @see ConfigurationBuilder
     */
    void visitConfigBuilderEnd();

    /**
     * By default, when the {@link BeanContext} is started, the {@link BeanDefinition#getExecutableMethods()} are not processed by registered {@link ExecutableMethodProcessor}
     * instances unless this method returns true.
     *
     * @see Executable#preprocess()
     * @return Whether the bean definition requires method processing
     */
    default boolean requiresMethodProcessing() {
        return false;
    }
    /**
     * Sets whether the {@link BeanDefinition#requiresMethodProcessing()} returns true
     *
     * @param shouldPreProcess True if they should be pre-processed
     */
    void setRequiresMethodProcessing(boolean shouldPreProcess);
}