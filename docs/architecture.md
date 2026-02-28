# Architecture

## Quarkus Extension Model

This project follows the standard [Quarkus extension](https://quarkus.io/guides/building-my-first-extension) two-module pattern:

### Runtime Module

Contains code that runs in the live application:

- **`VDP.java`** — The `@VDP` annotation. Applied to JAX-RS resource methods (or classes) to declare view metadata. Retained at runtime for reflective lookup by the response filter.

- **`VdpResponseFilter.java`** — A Resteasy Reactive `@ServerResponseFilter` that intercepts every outgoing response. It inspects the matched resource method for a `@VDP` annotation and, if present, attaches view metadata to the response using the configured transport.

### Deployment Module

Contains build-time logic that runs during the Quarkus augmentation phase:

- **`VdpProcessor.java`** — Registers the `vdp` feature and wires up `VdpResponseFilter` as both a CDI singleton bean and a Resteasy Reactive container response filter. This ensures the filter is discovered without classpath scanning.

### Integration Tests Module

A standalone Quarkus application that exercises every transport mode end-to-end using RestAssured.

## Request Flow

```
Client Request
      │
      ▼
┌─────────────┐
│  JAX-RS     │  Route matched, resource method invoked
│  Resource   │
└─────┬───────┘
      │ Response entity
      ▼
┌─────────────────────┐
│  VdpResponseFilter   │  Checks for @VDP on the matched method/class
│                     │
│  ┌───────────────┐  │
│  │ VIEW_TEMPLATE │──│──▶ Adds View-Template header
│  ├───────────────┤  │
│  │ LINK_HEADER   │──│──▶ Adds Link header with rel="view-descriptor"
│  ├───────────────┤  │
│  │ INLINE        │──│──▶ Wraps body: merges _view object into response
│  └───────────────┘  │
└─────────┬───────────┘
          │
          ▼
     Client Response
```

## Descriptor Loading and Caching

When a `@VDP` annotation specifies a `descriptor` path and the transport requires the descriptor content (i.e., `INLINE`), the filter:

1. Checks a `ConcurrentHashMap` cache for the descriptor path
2. On cache miss, loads the JSON file from the classpath via `Thread.currentThread().getContextClassLoader().getResourceAsStream()`
3. Parses it with Jackson's `ObjectMapper` into a `Map<String, Object>`
4. Stores the parsed result in the cache for subsequent requests

For header-based transports (`LINK_HEADER`, `VIEW_TEMPLATE`), the descriptor path is sent as-is without loading the file.

## Annotation Resolution

The filter resolves the `@VDP` annotation with the following precedence:

1. **Method-level** — checked first via `ResourceInfo.getResourceMethod()`
2. **Class-level** — fallback via `ResourceInfo.getResourceClass()`

If no `@VDP` annotation is found at either level, the filter is a no-op.

## Transport Selection (AUTO mode)

When `transport = Transport.AUTO`:

- If `descriptor` is non-empty → uses `LINK_HEADER`
- If only `template` is non-empty → uses `VIEW_TEMPLATE`
- If neither is set → no-op
