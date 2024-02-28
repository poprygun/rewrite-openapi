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

                List<Expression> currentArgs = an.getArguments();

                //has been already upgraded
                boolean contentWasAlreadyAdded = currentArgs.stream().anyMatch(arg
                        -> ((J.Identifier) ((J.Assignment) arg).getVariable()).getSimpleName().equalsIgnoreCase("content"));
                if (contentWasAlreadyAdded) return an;

                Optional<Expression> mayBeResponseContainer = currentArgs.stream()
                        .filter(arg -> ((J.Identifier) ((J.Assignment) arg).getVariable()).getSimpleName().equalsIgnoreCase("responseContainer"))
                        .findFirst();

                if (!mayBeResponseContainer.isPresent()) {
                    return an;
                }

                Optional<Expression> mayBeResponse = currentArgs.stream()
                        .filter(arg -> ((J.Identifier) ((J.Assignment) arg).getVariable()).getSimpleName().equalsIgnoreCase("response"))
                        .findFirst();

                J.Assignment as;
                if (mayBeResponse.isPresent()) {
                    Expression response = mayBeResponse.get();

                    String newAttributeValue = ((J.Assignment) response).getAssignment().toString();
                    as = (J.Assignment) ((J.Annotation) JavaTemplate.builder("#{} = @io.swagger.v3.oas.annotations.media.Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(uniqueItems = false, schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = #{})))")
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), a.getCoordinates().replaceArguments(), attributeName, newAttributeValue)
                    ).getArguments().get(0);
                } else {
                    as = (J.Assignment) ((J.Annotation) JavaTemplate.builder("#{} = @io.swagger.v3.oas.annotations.media.Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(uniqueItems = false))")
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), a.getCoordinates().replaceArguments(), attributeName)
                    ).getArguments().get(0);
                }

                List<Expression> newArguments = ListUtils.concat(as, a.getArguments());

                //make sure to remove legacy attribute arguments
                mayBeResponse.ifPresent(r -> newArguments.remove(r));
                mayBeResponseContainer.ifPresent(rc -> newArguments.remove(rc));

                an = an.withArguments(newArguments);
                an = autoFormat(an, ctx);
//                System.out.println(TreeVisitingPrinter.printTree(getCursor()));
                return an;
            }
        });
    }
}
