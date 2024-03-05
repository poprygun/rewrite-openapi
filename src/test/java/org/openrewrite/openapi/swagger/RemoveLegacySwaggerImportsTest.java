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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.RemoveUnusedImports;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveLegacySwaggerImportsTest implements RewriteTest {

    @Test
    void shouldRemoveUnusedImport() {
        rewriteRun(
          spec -> spec.recipe(new RemoveUnusedImports())
            .parser(JavaParser.fromJavaVersion().classpath("swagger-annotations-1.+")),
          //language=java
          java(
            """
              import io.swagger.annotations.*;
              
              class A {
                  void method() {}
              }
              """,
            """
              class A {
                  void method() {}
              }
              """
          )
        );
    }
}