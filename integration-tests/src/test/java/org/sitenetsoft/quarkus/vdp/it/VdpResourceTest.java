package org.sitenetsoft.quarkus.vdp.it;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class VdpResourceTest {

    @Test
    void articleReturnsViewTemplateHeader() {
        given()
            .when().get("/api/article")
            .then()
                .statusCode(200)
                .header("View-Template", "https://example.com/templates/article.html")
                .body("title", is("Hello VDP"))
                .body("body", is("An article rendered via VDP template binding."));
    }

    @Test
    void dashboardReturnsLinkHeader() {
        given()
            .when().get("/api/dashboard")
            .then()
                .statusCode(200)
                .header("Link", "<https://example.com/views/dashboard.json>; rel=\"view-descriptor\"")
                .body("user", is("admin"))
                .body("widgetCount", is(42));
    }

    @Test
    void inlineTemplateWrapsBody() {
        given()
            .when().get("/api/inline")
            .then()
                .statusCode(200)
                .body("_view.template", is("https://example.com/templates/card.html"))
                .body("name", is("Widget"))
                .body("price", is(9.99f));
    }

    @Test
    void inlineDescriptorLoadsFromClasspath() {
        given()
            .when().get("/api/inline-descriptor")
            .then()
                .statusCode(200)
                .body("_view.template", is("https://example.com/templates/dashboard.html"))
                .body("_view.slots.header.template", is("https://example.com/templates/header.html"))
                .body("_view.slots.widgets", hasSize(2))
                .body("_view.slots.widgets[0].template", is("https://example.com/templates/stat-card.html"))
                .body("_view.slots.widgets[1].template", is("https://example.com/templates/chart.html"))
                .body("user", is("admin"))
                .body("widgetCount", is(42));
    }
}
