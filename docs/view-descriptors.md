# View Descriptors

## What is a View Descriptor?

A view descriptor is a JSON document that tells a client how to render a response. Unlike a simple template URL, a descriptor can define **composable layouts** with named slots, each pointing to their own template.

This enables complex UIs where a single API response drives multiple components arranged in a defined structure.

## Schema

A view descriptor is a JSON object with the following structure:

```json
{
  "template": "<URL of the root template>",
  "slots": {
    "<slot-name>": <slot-value>,
    ...
  }
}
```

### Fields

| Field      | Type                | Required | Description                                      |
|------------|---------------------|----------|--------------------------------------------------|
| `template` | `string`            | Yes      | URL of the template for this view                |
| `slots`    | `object`            | No       | Named slots mapping to child views               |

### Slot Values

A slot value can be:

- **A single view object** — `{ "template": "...", "slots": { ... } }` (recursive)
- **An array of view objects** — `[{ "template": "..." }, { "template": "..." }]`

This allows arbitrary nesting and composition.

## Example

```json
{
  "template": "https://example.com/templates/dashboard.html",
  "slots": {
    "header": {
      "template": "https://example.com/templates/header.html"
    },
    "sidebar": {
      "template": "https://example.com/templates/sidebar.html"
    },
    "widgets": [
      {
        "template": "https://example.com/templates/stat-card.html"
      },
      {
        "template": "https://example.com/templates/chart.html"
      },
      {
        "template": "https://example.com/templates/activity-feed.html",
        "slots": {
          "item": {
            "template": "https://example.com/templates/activity-item.html"
          }
        }
      }
    ]
  }
}
```

This descriptor defines a dashboard with:
- A root template for the overall layout
- A `header` slot with its own template
- A `sidebar` slot
- A `widgets` slot containing three widgets, one of which has its own nested `item` slot

## File Placement

Place descriptor JSON files in your `src/main/resources/` directory. Reference them from the `@VDP` annotation by their classpath-relative path:

```
src/main/resources/
└── views/
    ├── dashboard.json
    ├── article.json
    └── profile.json
```

```java
@VDP(descriptor = "views/dashboard.json", transport = Transport.INLINE)
```

## Inline vs. Header Transport

| Transport      | Descriptor Behavior                                   |
|----------------|-------------------------------------------------------|
| `LINK_HEADER`  | Sends the descriptor path as a URL in the `Link` header. The client fetches the descriptor separately. |
| `INLINE`       | Loads the descriptor from the classpath, parses it, and merges it into the response body under `_view`. |
| `VIEW_TEMPLATE`| Ignores `descriptor`; only uses `template`.           |

For `INLINE` transport, the descriptor is loaded once and cached in memory for the lifetime of the application.
