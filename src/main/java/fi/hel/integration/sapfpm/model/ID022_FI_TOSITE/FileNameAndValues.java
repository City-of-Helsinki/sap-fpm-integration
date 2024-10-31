package fi.hel.integration.sapfpm.model.ID022_FI_TOSITE;

import java.util.Map;

public record FileNameAndValues(String fileName, Map<String, Object> values) {}
