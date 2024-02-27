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
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

public class ConvertApiResponseContainerToContent extends Recipe {

//    private static final AnnotationMatcher ANNOTATION_MATCHER = new AnnotationMatcher("@io.swagger.v3.oas.annotations.responses.ApiResponse(response = *, responseContainer = \"List\")");
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

                boolean contentWasAlreadyAdded = currentArgs.stream().anyMatch(arg
                        -> ((J.Identifier) ((J.Assignment) arg).getVariable()).getSimpleName().equalsIgnoreCase("content"));
                if (contentWasAlreadyAdded) return an;

                Optional<Expression> mayBeResponse = currentArgs.stream()
                        .filter(arg -> ((J.Identifier) ((J.Assignment) arg).getVariable()).getSimpleName().equalsIgnoreCase("response"))
                        .findFirst();

                if (!mayBeResponse.isPresent()) {
                    return an;
                }

                Expression response = currentArgs.stream()
                        .filter(arg -> ((J.Identifier) ((J.Assignment) arg).getVariable()).getSimpleName().equalsIgnoreCase("response"))
                        .findFirst().get();

                String newAttributeValue = ((J.Assignment) response).getAssignment().toString();
                //noinspection ConstantConditions
                J.Assignment as = (J.Assignment) ((J.Annotation) JavaTemplate.builder("#{} = @Content(array = @ArraySchema(uniqueItems = false, schema = @Schema(implementation = #{})))")
                        .contextSensitive()
                        .imports(
                                "io.swagger.v3.oas.annotations.media.Content"
                                , "io.swagger.v3.oas.annotations.media.Schema"
                                , "io.swagger.v3.oas.annotations.media.ArraySchema"
                        )
                        .build()
                        .apply(getCursor(), a.getCoordinates().replaceArguments(), attributeName, newAttributeValue)
                ).getArguments().get(0);
                List<Expression> newArguments = ListUtils.concat(as, a.getArguments());


//                boolean contains = cu.getMarkers().findFirst(JavaSourceSet.class).get().getClasspath().stream().map(JavaType.FullyQualified::getFullyQualifiedName)
//                        .collect(Collectors.toList()).contains("io.swagger.v3.oas.annotations.media.Content");

                maybeAddImport("io.swagger.v3.oas.annotations.media.Content");
                maybeAddImport("io.swagger.v3.oas.annotations.media.Schema");
                maybeAddImport("io.swagger.v3.oas.annotations.media.ArraySchema");

                J.CompilationUnit cu = getCursor().dropParentUntil(J.CompilationUnit.class::isInstance).getValue();

                for (Iterator<J.Import> iterator = cu.getImports().iterator(); iterator.hasNext(); ) {
                    J.Import next = iterator.next();
                    System.out.println("---->" + next.toString());
                }

//                J.Import importToAdd = new J.Import(randomId(),
//                        Space.EMPTY,
//                        Markers.EMPTY,
//                        new JLeftPadded<>(member == null ? Space.EMPTY : Space.SINGLE_SPACE,
//                                member != null, Markers.EMPTY),
//                        TypeTree.build(fullyQualifiedName +
//                                (member == null ? "" : "." + member)).withPrefix(Space.SINGLE_SPACE),
//                        null);

//                doAfterVisit(new AddImport<>("io.swagger.v3.oas.annotations.media.Content", null, true));
//                doAfterVisit(new AddImport<>("io.swagger.v3.oas.annotations.media.Schema", null, true));
//                doAfterVisit(new AddImport<>("io.swagger.v3.oas.annotations.media.ArraySchema", null, true));

                //make sure to remove legacy attribute arguments
                Expression responseContainer = currentArgs.stream()
                        .filter(arg -> ((J.Identifier) ((J.Assignment) arg).getVariable()).getSimpleName().equalsIgnoreCase("responseContainer"))
                        .findFirst().get();

                newArguments.remove(responseContainer);
                newArguments.remove(response);

                an = an.withArguments(newArguments);
                an = autoFormat(an, ctx);
//                System.out.println(TreeVisitingPrinter.printTree(getCursor()));
                return an;
            }
        });
    }
}
