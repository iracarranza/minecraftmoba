package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/**
 * Hand every arriving player the pack, rather than hoping they installed it.
 *
 * The pack was previously a generator with no destination: `build_pack.py`
 * wrote wherever `--output` pointed and nothing ever put the result in a
 * client's resourcepacks folder or served it. So the glyph HUD -- the hunger
 * row, and anything built on it later -- was never actually on any client, and
 * the symptom was simply not seeing the pack in the options list.
 *
 * Pushing from the server also makes the pack a property of the SERVER rather
 * than of each player's setup, which is what it has to be: the plugin blanks
 * vanilla sprites and draws replacements from private-use codepoints, so a
 * client without the pack sees missing hunger icons and tofu, not a degraded
 * version of the intended HUD.
 *
 * The hash is not optional. Clients cache by hash, so a rebuilt pack with a
 * stale hash is silently not applied -- which looks exactly like the pack not
 * working. publish.py writes the zip and the hash together for that reason.
 */
public final class ResourcePackPush implements Listener {
    private final MobaPlugin plugin;

    public ResourcePackPush(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.resourcePack.enabled"); }

    private String url() { return plugin.getConfig().getString("features.resourcePack.url", ""); }
    private String sha1() { return plugin.getConfig().getString("features.resourcePack.sha1", ""); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void join(PlayerJoinEvent e) {
        if (!enabled()) return;
        send(e.getPlayer());
    }

    public void send(Player p) {
        String url = url(), sha1 = sha1();
        if (url.isBlank()) {
            plugin.getLogger().warning("features.resourcePack.enabled is true but url is empty");
            return;
        }
        if (sha1.isBlank() || sha1.length() != 40) {
            // Refused rather than sent unhashed: an unhashed pack is
            // re-downloaded every join and cached wrongly, and the failure mode
            // is invisible.
            plugin.getLogger().warning("features.resourcePack.sha1 is missing or not a SHA-1; "
                    + "run implementation/resourcepack/publish.py to write it");
            return;
        }
        boolean required = plugin.getConfig().getBoolean("features.resourcePack.required", false);
        String prompt = plugin.getConfig().getString("features.resourcePack.prompt",
                "Minecraft MOBA uses a resource pack for its HUD.");
        try {
            p.setResourcePack(url, hex(sha1), Component.text(prompt), required);
        } catch (Exception ex) {
            plugin.getLogger().warning("could not send the resource pack: " + ex.getMessage());
        }
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++)
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return out;
    }

    /** Log the outcome: a declined or failed pack explains a broken-looking HUD. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void status(PlayerResourcePackStatusEvent e) {
        var status = e.getStatus();
        if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) return;
        plugin.getLogger().info("resource pack for " + e.getPlayer().getName() + ": " + status);
    }

    public String report() {
        return "RESOURCE_PACK enabled=" + enabled() + " url=" + url()
                + " sha1=" + (sha1().isBlank() ? "MISSING" : sha1().substring(0, 8) + "...")
                + " required=" + plugin.getConfig().getBoolean("features.resourcePack.required", false);
    }
}
