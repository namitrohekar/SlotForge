package com.slotforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A forgotten @PreAuthorize fails the build instead of silently leaving an endpoint open to any signed-in user. */
class EndpointAuthorizationCoverageTest {

    @Test
    void everyApiHandlerDeclaresWhoMayCallIt() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        // Profile-only controllers such as demo login count too.
        var environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        scanner.setEnvironment(environment);

        List<String> unguarded = new ArrayList<>();
        int checked = 0;
        for (BeanDefinition candidate : scanner.findCandidateComponents("com.slotforge")) {
            Class<?> type = Class.forName(candidate.getBeanClassName());
            for (Method method : type.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null || Arrays.stream(mapping.path()).noneMatch(path -> path.startsWith("/api"))) {
                    continue;
                }
                checked++;
                if (!AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)
                        && !AnnotatedElementUtils.hasAnnotation(type, PreAuthorize.class)) {
                    unguarded.add(type.getSimpleName() + "." + method.getName());
                }
            }
        }

        assertThat(checked).as("API handlers found").isGreaterThanOrEqualTo(5);
        assertThat(unguarded).as("API handlers without @PreAuthorize").isEmpty();
    }
}
