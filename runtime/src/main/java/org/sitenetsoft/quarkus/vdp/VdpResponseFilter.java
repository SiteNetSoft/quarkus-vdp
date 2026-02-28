package org.sitenetsoft.quarkus.vdp;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;

public class VdpResponseFilter {

    @Inject
    ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Map<String, Object>> descriptorCache = new ConcurrentHashMap<>();

    @ServerResponseFilter
    public void filter(ContainerResponseContext responseContext, ResourceInfo resourceInfo)
            throws IOException {

        VDP vdp = resolveAnnotation(resourceInfo);
        if (vdp == null) {
            return;
        }

        String template = vdp.template();
        String descriptor = vdp.descriptor();
        VDP.Transport transport = resolveTransport(vdp);

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

                Object viewValue = buildViewValue(template, descriptor);

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

    private VDP resolveAnnotation(ResourceInfo resourceInfo) {
        VDP methodLevel = resourceInfo.getResourceMethod().getAnnotation(VDP.class);
        if (methodLevel != null) {
            return methodLevel;
        }
        return resourceInfo.getResourceClass().getAnnotation(VDP.class);
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

    private Object buildViewValue(String template, String descriptor) throws IOException {
        if (!template.isEmpty()) {
            return Map.of("template", template);
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
