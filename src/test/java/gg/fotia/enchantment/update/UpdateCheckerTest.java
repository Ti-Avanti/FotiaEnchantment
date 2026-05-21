package gg.fotia.enchantment.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void releaseVersionIsNewerThanSnapshotBuild() {
        assertTrue(UpdateVersion.isNewer("v1.0.0", "1.0.0-SNAPSHOT"));
        assertTrue(UpdateVersion.isNewer("1.2.0", "1.1.9"));
        assertFalse(UpdateVersion.isNewer("v1.0.0", "1.0.0"));
        assertFalse(UpdateVersion.isNewer("1.0.0-beta.1", "1.0.0"));
    }

    @Test
    void parsesLatestReleasePayload() {
        String payload = """
                {
                  "html_url": "https://github.com/Ti-Avanti/FotiaEnchantment/releases/tag/v1.2.0",
                  "tag_name": "v1.2.0",
                  "name": "FotiaEnchantment 1.2.0"
                }
                """;

        GitHubRelease release = GitHubRelease.fromJson(payload);

        assertEquals("v1.2.0", release.tagName());
        assertEquals("https://github.com/Ti-Avanti/FotiaEnchantment/releases/tag/v1.2.0", release.htmlUrl());
    }
}
