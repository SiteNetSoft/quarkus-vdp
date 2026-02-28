# Development Guide

## Project Setup

Clone the repository and use the included Gradle wrapper:

```bash
git clone <repo-url>
cd quarkus-vdp
./gradlew build
```

### Requirements

- **Java 25** or later
- **Gradle 9.3.1** (provided via wrapper)

## Module Overview

| Module              | Purpose                                    | Gradle Plugin        |
|---------------------|--------------------------------------------|----------------------|
| `runtime`           | Annotation + response filter (ships in app)| `io.quarkus.extension` |
| `deployment`        | Build-time processor (augmentation only)   | `java-library`       |
| `integration-tests` | End-to-end tests with a real Quarkus app   | `io.quarkus`         |

## Building

```bash
# Full build (all modules)
./gradlew build

# Build without tests
./gradlew build -x test
```

## Running Tests

```bash
# All tests
./gradlew test

# Integration tests only
./gradlew :integration-tests:test
```

The integration tests start a Quarkus application with the extension installed, then exercise all four transport modes via HTTP.

## Running the Integration Test App in Dev Mode

```bash
cd integration-tests
../gradlew quarkusDev
```

This starts the test application with live reload. You can then manually test the endpoints:

```bash
curl -v http://localhost:8080/api/article
curl -v http://localhost:8080/api/dashboard
curl -v http://localhost:8080/api/inline
curl -v http://localhost:8080/api/inline-descriptor
```

## Adding a New Transport Mode

1. Add the new enum value to `Transport` (inside `VDP.java`)
2. Handle the new case in `VdpResponseFilter.applyVdp()`
3. Add a test endpoint in `VdpResource.java`
4. Add a test case in `VdpResourceTest.java`

## Adding New Annotation Attributes

1. Add the attribute to the `@VDP` annotation in `VDP.java`
2. Read the attribute in `VdpResponseFilter` and implement the behavior
3. Update tests accordingly

## Extension Registration

The deployment module (`VdpProcessor.java`) handles two build steps:

1. **Feature registration** — Declares `"vdp"` so Quarkus logs it at startup
2. **Filter registration** — Creates `VdpResponseFilter` as a CDI singleton and registers it with Resteasy Reactive's filter chain via `CustomContainerResponseFilterBuildItem`

If you add new CDI beans to the runtime module, register them in `VdpProcessor` using `AdditionalBeanBuildItem`.

## Code Conventions

- **Compiler flags**: `-parameters` is enabled for all modules, allowing Jackson and CDI to read method parameter names without explicit annotations
- **Encoding**: UTF-8
- **Java version**: Source and target set to Java 25
