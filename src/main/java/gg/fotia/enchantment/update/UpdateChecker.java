package gg.fotia.enchantment.update;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.util.SchedulerUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateChecker {

    private final FotiaEnchantment plugin;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public UpdateChecker(FotiaEnchantment plugin) {
        this.plugin = plugin;
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "FotiaEnchantment Update Checker");
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newSingleThreadExecutor(factory);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .executor(executor)
                .build();
    }

    public void checkOnStartup() {
        if (!plugin.getConfigManager().isUpdateCheckerEnabled()
                || !plugin.getConfigManager().isUpdateCheckOnStartup()) {
            return;
        }

        long delay = Math.max(0L, plugin.getConfigManager().getUpdateCheckDelaySeconds()) * 20L;
        SchedulerUtils.runTaskLater(plugin, this::checkNow, delay);
    }

    public void checkNow() {
        if (shutdown.get() || !plugin.getConfigManager().isUpdateCheckerEnabled()) {
            return;
        }
        String url = plugin.getConfigManager().getUpdateCheckerApiUrl();
        if (url == null || url.isBlank()) {
            plugin.getLogger().warning("Update checker skipped because update-checker.api-url is empty.");
            return;
        }

        CompletableFuture
                .supplyAsync(() -> fetchLatestRelease(url), executor)
                .thenAccept(result -> SchedulerUtils.runTask(plugin, () -> handleResult(result)))
                .exceptionally(error -> {
                    if (!shutdown.get()) {
                        plugin.getLogger().warning("Failed to check GitHub releases: " + error.getMessage());
                    }
                    return null;
                });
    }

    public void shutdown() {
        shutdown.set(true);
        executor.shutdownNow();
    }

    private GitHubRelease fetchLatestRelease(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "FotiaEnchantment/" + plugin.getPluginMeta().getVersion())
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                plugin.getLogger().info("No GitHub release found for update checker repository yet.");
                return new GitHubRelease("", "");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("GitHub API returned HTTP " + response.statusCode());
            }
            return GitHubRelease.fromJson(response.body());
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", ex);
        }
    }

    private void handleResult(GitHubRelease release) {
        if (shutdown.get() || release.tagName().isBlank()) {
            return;
        }

        String current = plugin.getPluginMeta().getVersion();
        String latest = release.tagName();
        if (!UpdateVersion.isNewer(latest, current)) {
            plugin.getLogger().info("FotiaEnchantment is up to date. Current: " + current + ", latest: " + latest);
            return;
        }

        String url = release.htmlUrl().isBlank()
                ? plugin.getConfigManager().getUpdateCheckerDownloadUrl()
                : release.htmlUrl();
        if (plugin.getConfigManager().isUpdateNotifyConsole()) {
            plugin.getLogger().warning("New FotiaEnchantment release available: " + latest
                    + " (current " + current + ") " + url);
        }
        if (plugin.getConfigManager().isUpdateNotifyOps()) {
            notifyOnlineAdmins(latest, current, url);
        }
    }

    private void notifyOnlineAdmins(String latest, String current, String url) {
        String raw = plugin.getLanguageManager().getMessage(plugin.getLanguageManager().getDefaultLanguage(), "update-available");
        Map<String, String> placeholders = Map.of(
                "latest", latest,
                "current", current,
                "url", url
        );
        Component message = plugin.getMessageHelper().parseText(null, raw, placeholders);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("fotia.enchantment.update")) {
                player.sendMessage(message);
            }
        }
    }
}
