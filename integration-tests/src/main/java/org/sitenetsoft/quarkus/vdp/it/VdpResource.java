package org.sitenetsoft.quarkus.vdp.it;

import org.sitenetsoft.quarkus.vdp.VDP;
import org.sitenetsoft.quarkus.vdp.VDP.Transport;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class VdpResource {

    @GET
    @Path("/article")
    @VDP(template = "https://example.com/templates/article.html")
    public Article getArticle() {
        return new Article("Hello VDP", "An article rendered via VDP template binding.");
    }

    @GET
    @Path("/dashboard")
    @VDP(descriptor = "https://example.com/views/dashboard.json")
    public Dashboard getDashboard() {
        return new Dashboard("admin", 42);
    }

    @GET
    @Path("/inline")
    @VDP(template = "https://example.com/templates/card.html", transport = Transport.INLINE)
    public Product getProduct() {
        return new Product("Widget", 9.99);
    }

    @GET
    @Path("/inline-descriptor")
    @VDP(descriptor = "/views/dashboard.json", transport = Transport.INLINE)
    public Dashboard getDashboardInline() {
        return new Dashboard("admin", 42);
    }

    public record Article(String title, String body) {}

    public record Dashboard(String user, int widgetCount) {}

    public record Product(String name, double price) {}
}
