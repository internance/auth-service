package com.internance.auth.infrastructure.config.event;

import java.util.Set;

/**
 * Auth-side view of the {@code config.changed} event published by config-service.
 *
 * <p>config-service emits its own {@code ConfigChangedEvent} wrapped in common-lib's
 * {@code EventEnvelope}, with Jackson type-info disabled on the wire — so the payload
 * is plain JSON that we deserialize structurally from the listener method signature
 * ({@code EventEnvelope<ConfigChangedEvent>}). This record only has to mirror the
 * producer's field names; we keep a local copy rather than depending on config-service.
 *
 * @param application the config application whose files changed (e.g. {@code auth-service})
 * @param label       the Git branch/label the change landed on (e.g. {@code main})
 * @param paths       the changed file paths belonging to {@code application}
 * @param commitId    the head commit SHA of the push, or {@code null} if unknown
 */
public record ConfigChangedEvent(String application, String label, Set<String> paths, String commitId) {}
