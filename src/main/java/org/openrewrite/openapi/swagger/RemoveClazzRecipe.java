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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.openrewrite.internal.lang.NonNull;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper=false)
public class RemoveClazzRecipe extends Recipe{

    @Option(displayName = "Java Type package regex",
            description = "Class will be deleted when usage of types from this package found",
            example = "springfox\\.documentation(\\..+)?")
    @NonNull
    String typePackage;

    @JsonCreator
    public RemoveClazzRecipe(@NonNull @JsonProperty("typePackage") String typePackage) {
        this.typePackage = typePackage;
    }

    @Override
    public String getDisplayName() {
        return "Remove java class if it uses java type from specified package";
    }

    @Override
    public String getDescription() {
        return "Remove java class if it uses java type from specified package.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                if (cu.getImports().stream().anyMatch(anImport -> anImport.getPackageName().matches(typePackage))) {
                    return null;
                }
                return super.visitCompilationUnit(cu, executionContext);
            }
        };
    }
}
