package com.termux.shared.termux;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TermuxLinksTest {

    private static final String[][] PUBLIC_MODERN_TERMUX_REPOS = new String[][] {
        { "Modern Termux", "https://github.com/Furkan00487/modern-termux" },
        { "Modern Termux Advanced", "https://github.com/Furkan00487/modern-termux-advanced" },
        { "Modern Termux TV", "https://github.com/Furkan00487/modern-termux-tv" },
        { "Modern Termux:MOTD", "https://github.com/Furkan00487/modern-termux-motd" }
    };

    @Test
    public void testNullContextHandling() {
        assertEquals("null", TermuxUtils.getImportantLinksMarkdownString(null));
        assertEquals("null", TermuxUtils.getReportIssueMarkdownString(null));
    }

    @Test
    public void testUpstreamLabelsAreNotMaskedAsModernTermux() {
        assertEquals("Termux", TermuxConstants.TERMUX_UPSTREAM_APP_NAME);
        assertEquals("Termux Site", TermuxConstants.TERMUX_SITE);
        assertEquals("Termux Wiki", TermuxConstants.TERMUX_WIKI);
        assertEquals("https://github.com/termux/termux-app", TermuxConstants.TERMUX_GITHUB_REPO_URL);
        assertEquals("https://github.com/termux/termux-app/wiki", TermuxConstants.TERMUX_GITHUB_WIKI_REPO_URL);
    }

    @Test
    public void testImportantLinksIncludeAllPublicVariantsWithIssuesAndReleases() {
        String links = TermuxUtils.getImportantLinksMarkdownString();

        assertTrue(links.contains("### Modern Termux (GitHub)"));
        for (String[] repo : PUBLIC_MODERN_TERMUX_REPOS) {
            String name = repo[0];
            String url = repo[1];
            assertTrue("Missing repo link for " + name, links.contains("[" + name + "](" + url + ")"));
            assertTrue("Missing issues link for " + name, links.contains("[" + name + " Issues](" + url + "/issues)"));
            assertTrue("Missing releases link for " + name, links.contains("[" + name + " Releases](" + url + "/releases)"));
        }

        assertFalse(links.contains("modern-termux-gamepad"));
        assertFalse(links.contains("Modern Termux:Gamepad"));

        assertTrue(links.contains("### Upstream Termux (GitHub)"));
        assertTrue(links.contains("[Termux (Upstream)](https://github.com/termux/termux-app)"));
        assertTrue(links.contains("[Termux Wiki (wiki.termux.com)](https://wiki.termux.com)"));
        assertTrue(links.contains("[termux-app (GitHub Wiki)](https://github.com/termux/termux-app/wiki)"));
        assertTrue(links.contains("[termux-packages (GitHub Wiki)](https://github.com/termux/termux-packages/wiki)"));

        assertFalse(links.contains("[Termux (Upstream) Wiki](https://github.com/termux/termux-app/wiki)"));
        assertFalse(links.contains("[Modern Termux](https://github.com/termux/termux-app)"));
        assertFalse(links.contains("[Modern Termux Wiki](https://wiki.termux.com)"));
        assertFalse(links.contains("[Modern Termux](https://github.com/termux/termux-app/wiki)"));
    }

    @Test
    public void testReportIssueLinksIncludeAllPublicModernTermuxVariantsOnly() {
        String report = TermuxUtils.buildReportIssueMarkdownString("Report details");

        assertTrue(report.contains("### GitHub Issues for Modern Termux"));
        for (String[] repo : PUBLIC_MODERN_TERMUX_REPOS) {
            assertTrue("Missing issues link for " + repo[0], report.contains("[" + repo[0] + "](" + repo[1] + "/issues)"));
        }

        assertFalse(report.contains("modern-termux-gamepad"));
        assertFalse(report.contains("Modern Termux:Gamepad"));
        assertTrue(report.contains("[Termux (Upstream)](https://github.com/termux/termux-app/issues)"));
        assertFalse(report.contains("[Modern Termux](https://github.com/termux/termux-app/issues)"));
    }
}
