/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.openapi.swagger;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConvertApiResponseContainerToContent extends Recipe {

    private static final AnnotationMatcher ANNOTATION_MATCHER = new AnnotationMatcher("@io.swagger.v3.oas.annotations.responses.ApiResponse(response = *, responseContainer = \"List\")");

    @Override
    public String getDisplayName() {
        return "Convert API response container to content";
    }

    @Override
    public String getDescription() {
        return "Convert API response container to content.";
    }

    private String annotationType = "io.swagger.v3.oas.annotations.responses.ApiResponse";

    private String attributeName = "content";
    private String attributeValue = "org.openrewrite.openapi.swagger.Donut.class";
    boolean addOnly = true;


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext ctx) {
                if (!ANNOTATION_MATCHER.matches(a)) {
                    return a;
                }

                List<Expression> currentArgs = a.getArguments();

                boolean contentWasAlreadyAdded = currentArgs.stream().anyMatch(arg
                        -> ((J.Identifier) ((J.Assignment) arg).getVariable()).getSimpleName().equalsIgnoreCase("content"));
                if (contentWasAlreadyAdded) return a;

                String newAttributeValue = maybeQuoteStringArgument(attributeName, attributeValue, a);

                // First assume the value exists amongst the arguments and attempt to update it
                AtomicBoolean foundAttributeWithDesiredValue = new AtomicBoolean(false);
                final J.Annotation finalA = a;
                List<Expression> newArgs = ListUtils.map(currentArgs, it -> {
                    if (it instanceof J.Literal) {
                        // The only way anything except an assignment can appear is if there's an implicit assignment to "value"
                        if (attributeName == null || "value".equals(attributeName)) {
                            if (newAttributeValue == null) {
                                return null;
                            }
                            J.Literal value = (J.Literal) it;
                            if (newAttributeValue.equals(value.getValueSource()) || Boolean.TRUE.equals(addOnly)) {
                                foundAttributeWithDesiredValue.set(true);
                                return it;
                            }
                            return ((J.Literal) it).withValue(newAttributeValue).withValueSource(newAttributeValue);
                        } else {
                            //noinspection ConstantConditions
                            return ((J.Annotation) JavaTemplate.builder("value = #{}")
                                    .contextSensitive()
                                    .build()
                                    .apply(getCursor(), finalA.getCoordinates().replaceArguments(), it)
                            ).getArguments().get(0);
                        }
                    }
                    return it;
                });
                if (foundAttributeWithDesiredValue.get() || newArgs != currentArgs) {
                    return a.withArguments(newArgs);
                }
                // There was no existing value to update, so add a new value into the argument list
                String effectiveName = (attributeName == null) ? "value" : attributeName;
                // Try annotation as an attribute value
//                J.Annotation newAnnotationAttributeValue =
//                        (J.Annotation)JavaTemplate.builder("@Content")
////                                .imports(importComponent)
//                                .build();

                //noinspection ConstantConditions
                J.Assignment as = (J.Assignment) ((J.Annotation) JavaTemplate.builder("#{} = @Content(array = @ArraySchema(uniqueItems = false, schema = @Schema(implementation = #{})))")
                        .contextSensitive()
                        .imports("io.swagger.v3.oas.annotations.media.Content")
                        .build()
                        .apply(getCursor(), a.getCoordinates().replaceArguments(), effectiveName, newAttributeValue)
                ).getArguments().get(0);
                List<Expression> newArguments = ListUtils.concat(as, a.getArguments());
                a = a.withArguments(newArguments);
                a = autoFormat(a, ctx);


                return a;
            }
        });
    }

    @Nullable
    private static String maybeQuoteStringArgument(@Nullable String attributeName, @Nullable String attributeValue, J.Annotation annotation) {
        if ((attributeValue != null) && attributeIsString(attributeName, annotation)) {
            return "\"" + attributeValue + "\"";
        } else {
            return attributeValue;
        }
    }

    private static boolean attributeIsString(@Nullable String attributeName, J.Annotation annotation) {
        String actualAttributeName = (attributeName == null) ? "value" : attributeName;
        JavaType.Class annotationType = (JavaType.Class) annotation.getType();
        if (annotationType != null) {
            for (JavaType.Method m : annotationType.getMethods()) {
                if (m.getName().equals(actualAttributeName)) {
                    return TypeUtils.isOfClassType(m.getReturnType(), "java.lang.String");
                }
            }
        }
        return false;
    }
}
