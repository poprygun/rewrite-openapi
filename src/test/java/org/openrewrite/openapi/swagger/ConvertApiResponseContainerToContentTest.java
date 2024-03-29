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
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ConvertApiResponseContainerToContentTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.openapi.swagger.MigrateApiResponsesToApiResponses")
          .parser(JavaParser.fromJavaVersion().classpath("swagger-annotations-1.+", "swagger-annotations-2.+"));
    }

    @Test
    @DocumentExample
    void convertApiResponseContainerToContent() {
        rewriteRun(
          //language=java
          java(
            """
              import io.swagger.annotations.ApiResponse;
              import io.swagger.annotations.ApiResponses;
              
              class A {
                  @ApiResponses(value = {
                      @ApiResponse(response = org.openrewrite.openapi.swagger.Donut.class, responseContainer = "List")})
                  void method() {}
              }
              
              """,
            """
              import io.swagger.v3.oas.annotations.responses.ApiResponse;
              import io.swagger.v3.oas.annotations.responses.ApiResponses;

              class A {
                  @ApiResponses(value = {
                          @ApiResponse(content = @io.swagger.v3.oas.annotations.media.Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(uniqueItems = false, schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = org.openrewrite.openapi.swagger.Donut.class))))})
                  void method() {}
              }
              
              """
          )
        );
    }

    @Test
    @DocumentExample
    void convertApiResponseContainerToContentWithoutResponseAttr() {
        rewriteRun(
          //language=java
          java(
            """
              import io.swagger.annotations.ApiResponse;
              import io.swagger.annotations.ApiResponses;
              
              class A {
                  @ApiResponses(value = {
                      @ApiResponse(responseContainer = "List")})
                  void method() {}
              }
              
              """,
            """
              import io.swagger.v3.oas.annotations.responses.ApiResponse;
              import io.swagger.v3.oas.annotations.responses.ApiResponses;

              class A {
                  @ApiResponses(value = {
                          @ApiResponse(content = @io.swagger.v3.oas.annotations.media.Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(uniqueItems = false)))})
                  void method() {}
              }
              
              """
          )
        );
    }


    @Test
    void noChangeOnAlreadyConverted() {
        rewriteRun(
          //language=java
          java(
            """
              import io.swagger.v3.oas.annotations.responses.ApiResponse;
              import io.swagger.v3.oas.annotations.responses.ApiResponses;             

              class A {
                  @ApiResponses(value = {
                          @ApiResponse(content = @io.swagger.v3.oas.annotations.media.Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(uniqueItems = false, schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = org.openrewrite.openapi.swagger.Donut.class))))})
                  void method() {}
              }
              """
          )
        );
    }
}