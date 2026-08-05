package org.sitenetsoft.quarkus.vdp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attaches a View Descriptor Protocol (VDP) view descriptor or template URL
 * to a JAX-RS endpoint response.
 *
 * <p>Method-level {@code @VDP} overrides class-level {@code @VDP}.</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface VDP {

    /**
     * Template URL for simple single-template responses.
     * Sent as a {@code View-Template} header (or inlined) depending on transport.
     */
    String template() default "";

    /**
     * View descriptor resource URL for complex compositions with slots.
     * Sent as a {@code Link} header with {@code rel="view-descriptor"} (or inlined)
     * depending on transport. For {@link Transport#INLINE}, this is a classpath path
     * (e.g. {@code /views/dashboard.json}) whose content is loaded and merged into
     * the response body as {@code _view}.
     */
    String descriptor() default "";

    /**
     * A VDP 0.2 transform (spec Section 3.8) as a JSON string — the declarative
     * mapping adapting this response's representation into the model the
     * template expects, e.g. {@code {"title": "/name", "value": "/role"}}.
     *
     * <p>Honored only for {@link Transport#INLINE} with {@link #template()}:
     * the emitted {@code _view} becomes
     * {@code {"template": ..., "transform": ...}}. The other forms carry their
     * own transforms — a descriptor file declares them per node, and the
     * {@code View-Template} header is by definition the bare
     * {@code {"template": url}} shorthand — so the attribute is ignored there.</p>
     */
    String transform() default "";

    /**
     * Transport mechanism for delivering the view metadata.
     */
    Transport transport() default Transport.AUTO;

    enum Transport {
        /**
         * Automatic: {@code descriptor} set → {@link #LINK_HEADER};
         * {@code template} set → {@link #VIEW_TEMPLATE}.
         */
        AUTO,

        /**
         * {@code Link: <url>; rel="view-descriptor"} HTTP header.
         */
        LINK_HEADER,

        /**
         * {@code View-Template: <url>} HTTP header.
         */
        VIEW_TEMPLATE,

        /**
         * Wraps response body with {@code _view} key inline in JSON.
         */
        INLINE
    }
}
