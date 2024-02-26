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
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Iterator;
import java.util.List;

public class ConvertApiResponseContainerToContext extends Recipe {

    private static final AnnotationMatcher ANNOTATION_MATCHER = new AnnotationMatcher("@io.swagger.v3.oas.annotations.responses.ApiResponse(response = *, responseContainer = \"List\")");

    @Override
    public String getDisplayName() {
        return "Convert API response container to context";
    }

    @Override
    public String getDescription() {
        return "Convert API response container to context.";
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return Preconditions.check(
                new UsesType<>("io.swagger.v3.oas.annotations.responses.ApiResponse", true)
                , new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext ctx) {
                        if (ANNOTATION_MATCHER.matches(a)) {
//                            return a.withArguments(ListUtils.map(a.getArguments(), this::maybeReplaceResponseCodeTypeAndValue));
                            return JavaTemplate.builder("#{} = #{}")
                                    .contextSensitive()
                                    .build()
                                    .apply(getCursor(), a.getCoordinates().replaceArguments(), "context", "somecrazyvalue");
                        }
                        return a;
                    }

                    private Expression maybeReplaceResponseCodeTypeAndValue(Expression arg) {
                        if (arg instanceof J.Assignment) {
                            J.Assignment assignment = (J.Assignment) arg;
                            boolean matchesField = assignment.getVariable() instanceof J.Identifier &&
                                    "response".equals(((J.Identifier) assignment.getVariable()).getSimpleName());

                            if (matchesField){
                                Expression target = ((J.FieldAccess) assignment.getAssignment()).getTarget();
                            }
                        }
                        return arg;
                    }
                }
        );
    }

    private static class ApiResponseAnnotationTransformer extends JavaIsoVisitor<ExecutionContext> {
        private static final AnnotationMatcher API_RESPONSE_MATCHER = new AnnotationMatcher("@io.swagger.v3.oas.annotations.responses.ApiResponse(response = *, responseContainer = \"List\")");

        @Override
        public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext executionContext) {
//            if (API_RESPONSE_MATCHER.matches(a)) {
//                List<Expression> currentArgs = a.getArguments();

//                return JavaTemplate.builder("#{}")/*[Rewrite8 migration]`contextSensitive()` could be unnecessary and can be removed, please double-check manually*/.contextSensitive()
//                        .build().apply(/*[Rewrite8 migration] please double-check correctness of this parameter manually, it could be updateCursor() if the value is updated somewhere*/getCursor(),
//                                a.getCoordinates().replaceArguments(),
//                                param1,
//                                param2);

//                return a;
//            }
            return super.visitAnnotation(a, executionContext);
        }
    }
}
