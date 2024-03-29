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
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.Optional;

public class ConvertApiResponseContainerToContent extends Recipe {
    private static final AnnotationMatcher ANNOTATION_MATCHER = new AnnotationMatcher("@io.swagger.v3.oas.annotations.responses.ApiResponse");

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


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext ctx) {
                J.Annotation an = super.visitAnnotation(a, ctx);
                if (!ANNOTATION_MATCHER.matches(an)) {
                    return an;
                }

                if (annotationAlreadyUpgraded(an)) return an;

                Optional<Expression> mayBeResponseContainer = findArgumentByName(an, "responseContainer");
                if (!mayBeResponseContainer.isPresent()) {
                    return an;
                }

                Optional<Expression> mayBeResponse = findArgumentByName(an, "response");
                J.Assignment as = createNewAssignment(a, mayBeResponse);

                List<Expression> newArguments = ListUtils.concat(as, a.getArguments());
                mayBeResponse.ifPresent(newArguments::remove);
                mayBeResponseContainer.ifPresent(newArguments::remove);

                an = an.withArguments(newArguments);
                an = autoFormat(an, ctx);
                return an;
            }

            private boolean annotationAlreadyUpgraded(J.Annotation an) {
                return an.getArguments().stream().anyMatch(arg ->
                        ((J.Identifier) ((J.Assignment) arg).getVariable()).getSimpleName().equalsIgnoreCase("content"));
            }

            private Optional<Expression> findArgumentByName(J.Annotation an, String name) {
                return an.getArguments().stream()
                        .filter(arg -> ((J.Identifier) ((J.Assignment) arg).getVariable()).getSimpleName().equalsIgnoreCase(name))
                        .findFirst();
            }

            private J.Assignment createNewAssignment(J.Annotation a, Optional<Expression> mayBeResponse) {
                String templateString = mayBeResponse.isPresent() ?
                        "#{} = @io.swagger.v3.oas.annotations.media.Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(uniqueItems = false, schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = #{})))" :
                        "#{} = @io.swagger.v3.oas.annotations.media.Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(uniqueItems = false))";

                JavaTemplate.Builder templateBuilder = JavaTemplate.builder(templateString).contextSensitive();
                Object[] templateArgs = mayBeResponse.map(expression -> new Object[]{attributeName, ((J.Assignment) expression).getAssignment().toString()}).orElseGet(() -> new Object[]{attributeName});

                return (J.Assignment) ((J.Annotation) templateBuilder.build()
                        .apply(getCursor(), a.getCoordinates().replaceArguments(), templateArgs))
                        .getArguments().get(0);
            }
        });
    }
}
