# Quarkus VDP Extension

A Quarkus extension that implements the **View Descriptor Protocol (VDP)** — a standard mechanism for REST endpoints to advertise view templates and descriptors to clients through HTTP headers or inline JSON.

## Overview

VDP bridges server-side data APIs with client-side view rendering. Annotate your JAX-RS endpoints with `@VDP` to automatically attach view metadata to responses, telling clients *how* to render the data they receive.

```java
@GET
@Path("/dashboard")
@VDP(descriptor = "views/dashboard.json", transport = Transport.LINK_HEADER)
public Dashboard getDashboard() {
    return new Dashboard("My Dashboard", List.of("Sales", "Traffic"));
}
```

## Features

- **`@VDP` annotation** for declarative view binding on JAX-RS endpoints
- **Multiple transport modes**: HTTP headers, Link headers, or inline JSON
- **View descriptors**: JSON files describing complex, composable view layouts with slots
- **Classpath descriptor loading** with thread-safe caching
- **Quarkus-native**: build-time registration, CDI integration, Resteasy Reactive filter

## Getting Started

### Prerequisites

- Java 25+
- Gradle 9.x (wrapper included)
- Quarkus 3.31.4+

### Installation

Add the runtime dependency to your Quarkus application's `build.gradle`:

```gradle
dependencies {
    implementation 'org.sitenetsoft:quarkus-vdp:1.0.0-SNAPSHOT'
}
```

### Build

```bash
./gradlew build
```

### Run integration tests

```bash
./gradlew :integration-tests:test
```

## Usage

### 1. Simple Template Header

Send a `View-Template` header pointing clients to an HTML template:

```java
@GET
@Path("/article")
@VDP(template = "https://example.com/templates/article.html", transport = Transport.VIEW_TEMPLATE)
public Article getArticle() {
    return new Article("Hello", "World");
}
```

**Response:**
```
HTTP/1.1 200 OK
View-Template: https://example.com/templates/article.html
Content-Type: application/json

{"title":"Hello","body":"World"}
```

### 2. Link Header with Descriptor

Send a `Link` header referencing a view descriptor URL:

```java
@GET
@Path("/dashboard")
@VDP(descriptor = "views/dashboard.json", transport = Transport.LINK_HEADER)
public Dashboard getDashboard() {
    return new Dashboard("My Dashboard", List.of("Sales", "Traffic"));
}
```

**Response:**
```
HTTP/1.1 200 OK
Link: <views/dashboard.json>; rel="view-descriptor"
Content-Type: application/json

{"title":"My Dashboard","widgets":["Sales","Traffic"]}
```

### 3. Inline Template

Wrap the response body with a `_view` key containing the template URL:

```java
@GET
@Path("/product")
@VDP(template = "https://example.com/templates/product.html", transport = Transport.INLINE)
public Product getProduct() {
    return new Product("Widget", 9.99);
}
```

**Response:**
```json
{
  "_view": {
    "template": "https://example.com/templates/product.html"
  },
  "name": "Widget",
  "price": 9.99
}
```

### 4. Inline Descriptor (Loaded from Classpath)

Load a JSON descriptor from the classpath and merge it into the response:

```java
@GET
@Path("/inline-descriptor")
@VDP(descriptor = "views/dashboard.json", transport = Transport.INLINE)
public Dashboard getInlineDashboard() {
    return new Dashboard("My Dashboard", List.of("Sales", "Traffic"));
}
```

With `src/main/resources/views/dashboard.json`:
```json
{
  "template": "https://example.com/templates/dashboard.html",
  "slots": {
    "header": {
      "template": "https://example.com/templates/header.html"
    },
    "widgets": [
      { "template": "https://example.com/templates/stat-card.html" },
      { "template": "https://example.com/templates/chart.html" }
    ]
  }
}
```

**Response:**
```json
{
  "_view": {
    "template": "https://example.com/templates/dashboard.html",
    "slots": {
      "header": { "template": "https://example.com/templates/header.html" },
      "widgets": [
        { "template": "https://example.com/templates/stat-card.html" },
        { "template": "https://example.com/templates/chart.html" }
      ]
    }
  },
  "title": "My Dashboard",
  "widgets": ["Sales", "Traffic"]
}
```

### Auto Transport

Use `Transport.AUTO` (the default) to let the extension choose the transport automatically:

- If `descriptor` is set → `LINK_HEADER`
- If only `template` is set → `VIEW_TEMPLATE`

## `@VDP` Annotation Reference

| Attribute     | Type        | Default          | Description                                          |
|---------------|-------------|------------------|------------------------------------------------------|
| `template`    | `String`    | `""`             | URL of the view template                             |
| `descriptor`  | `String`    | `""`             | Classpath path to a JSON view descriptor             |
| `transport`   | `Transport` | `Transport.AUTO` | How view metadata is delivered to the client          |

### Transport Modes

| Mode             | Mechanism                                  | Use Case                           |
|------------------|--------------------------------------------|------------------------------------|
| `VIEW_TEMPLATE`  | `View-Template` HTTP header                | Simple single-template views       |
| `LINK_HEADER`    | `Link` header with `rel="view-descriptor"` | External descriptor references     |
| `INLINE`         | Merged into response JSON body             | Self-contained responses           |
| `AUTO`           | Selects based on annotation values         | Sensible defaults                  |

## Project Structure

```
quarkus-vdp/
├── runtime/               # Extension runtime code
│   └── src/main/java/
│       └── VDP.java                 # @VDP annotation
│       └── VdpResponseFilter.java   # Response filter (transport logic)
├── deployment/            # Build-time processor
│   └── src/main/java/
│       └── VdpProcessor.java        # Registers feature & filter
├── integration-tests/     # Integration test module
│   └── src/main/java/
│       └── VdpResource.java         # Example REST endpoints
│   └── src/test/java/
│       └── VdpResourceTest.java     # Tests for all transport modes
└── build.gradle           # Root build configuration
```

## Technology Stack

- **Quarkus 3.31.4** — Supersonic Subatomic Java framework
- **Resteasy Reactive** — Non-blocking JAX-RS implementation
- **Jackson** — JSON serialization/deserialization
- **Gradle 9.3.1** — Build tool
- **Java 25** — Language target
- **JUnit 5 + RestAssured** — Testing

## License

Copyright SiteNetSoft. All rights reserved.
