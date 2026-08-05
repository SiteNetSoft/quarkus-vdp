package org.sitenetsoft.quarkus.vdp;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerResponseContext;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

@Singleton
public class VdpResponseFilter {

    /** The VDP protocol version this extension implements and advertises (spec Sections 13.1, 15.1). */
    public static final String VDP_VERSION = "0.2";

    private static final String HEADER_VDP_VERSION = "VDP-Version";

    @Inject
    ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Map<String, Object>> descriptorCache = new ConcurrentHashMap<>();

    @ServerResponseFilter
    public void filter(ContainerResponseContext responseContext, SimpleResourceInfo resourceInfo)
            throws IOException {

        VDP vdp = resolveAnnotation(resourceInfo);
        if (vdp == null) {
            return;
        }

        String template = vdp.template();
        String descriptor = vdp.descriptor();
        VDP.Transport transport = resolveTransport(vdp);

        // Spec Section 13.1: VDP-Version may ride on any response carrying a
        // view descriptor; a server SHOULD advertise its support (Section 15.1).
        responseContext.getHeaders().putSingle(HEADER_VDP_VERSION, VDP_VERSION);

        switch (transport) {
            case VIEW_TEMPLATE -> {
                String url = !template.isEmpty() ? template : descriptor;
                responseContext.getHeaders().putSingle("View-Template", url);
            }
            case LINK_HEADER -> {
                String url = !descriptor.isEmpty() ? descriptor : template;
                responseContext.getHeaders().putSingle("Link", "<" + url + ">; rel=\"view-descriptor\"");
            }
            case INLINE -> {
                Object entity = responseContext.getEntity();
                if (entity == null) {
                    return;
                }

                Object viewValue = buildViewValue(template, descriptor, vdp.transform());

                @SuppressWarnings("unchecked")
                Map<String, Object> entityMap = objectMapper.convertValue(entity, Map.class);

                LinkedHashMap<String, Object> wrapped = new LinkedHashMap<>();
                wrapped.put("_view", viewValue);
                wrapped.putAll(entityMap);

                responseContext.setEntity(wrapped);
            }
            default -> {
                // AUTO is resolved before reaching here
            }
        }
    }

    private VDP resolveAnnotation(SimpleResourceInfo resourceInfo) {
        // Try method-level first by scanning declared methods matching the name
        Class<?> resourceClass = resourceInfo.getResourceClass();
        String methodName = resourceInfo.getMethodName();

        for (Method method : resourceClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                VDP methodLevel = method.getAnnotation(VDP.class);
                if (methodLevel != null) {
                    return methodLevel;
                }
            }
        }

        // Fall back to class-level
        return resourceClass.getAnnotation(VDP.class);
    }

    private VDP.Transport resolveTransport(VDP vdp) {
        if (vdp.transport() != VDP.Transport.AUTO) {
            return vdp.transport();
        }
        if (!vdp.descriptor().isEmpty()) {
            return VDP.Transport.LINK_HEADER;
        }
        return VDP.Transport.VIEW_TEMPLATE;
    }

    private Object buildViewValue(String template, String descriptor, String transform) throws IOException {
        if (!template.isEmpty()) {
            if (transform.isEmpty()) {
                return Map.of("template", template);
            }
            // VDP 0.2 (spec Section 3.8): the transform travels on the
            // descriptor node, adapting this representation to the template's
            // model. Parsed here so a malformed literal fails fast at first
            // use rather than shipping an invalid descriptor.
            LinkedHashMap<String, Object> view = new LinkedHashMap<>();
            view.put("template", template);
            view.put("transform", objectMapper.readValue(transform, Object.class));
            return view;
        }
        return loadDescriptor(descriptor);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadDescriptor(String path) throws IOException {
        Map<String, Object> cached = descriptorCache.get(path);
        if (cached != null) {
            return cached;
        }

        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                path.startsWith("/") ? path.substring(1) : path)) {
            if (is == null) {
                throw new IOException("VDP descriptor not found on classpath: " + path);
            }
            Map<String, Object> loaded = objectMapper.readValue(is, Map.class);
            descriptorCache.put(path, loaded);
            return loaded;
        }
    }
}
