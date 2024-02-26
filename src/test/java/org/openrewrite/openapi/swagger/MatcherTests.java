package org.openrewrite.openapi.swagger;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

public class MatcherTests {
    @Test
    void shouldMatchApiResponseWithResponseAttribute(){
        var toMatch = "@ApiResponse(response = org.openrewrite.openapi.swagger.Donut.class, responseContainer = \"List\")";


        var javaTemplate = JavaTemplate.builder("@ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = false, schema = @Schema(implementation = #{any(org.openrewrite.openapi.swagger.Donut)}))))")/*[Rewrite8 migration]`contextSensitive()` could be unnecessary and can be removed, please double-check manually*/.contextSensitive()
          .build();

        String template = "@ApiResponse(response = org.openrewrite.openapi.swagger.Donut.class, responseContainer = \"List\")";



        var annotationMatcher = new AnnotationMatcher("@ApiResponse\\(response = ([\\w\\.]+)\\.class, responseContainer = \"List\"\\)");
//        assertThat(annotationMatcher.matches())
    }
}