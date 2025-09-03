package fi.hel.integration.sapfpm;

import io.quarkus.test.junit.QuarkusTestProfile;

// so a new quarkus instance is started for some tests,
// otherwise there are clashes between test classes when advicing routes that call common routes like direct:any-file-out
public class Profiles {
    public static class TositeTestProfile implements QuarkusTestProfile { }
    public static class CoTositeTestProfile implements QuarkusTestProfile { }
    public static class S4TositeTestProfile implements QuarkusTestProfile { }
}
