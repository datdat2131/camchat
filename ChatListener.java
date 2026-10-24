package vn.astrax.chatfilter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class ChatListener implements Listener {

    private final ChatFilterPlugin plugin;

    public ChatListener(ChatFilterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!plugin.getConfig().getBoolean("enabled", true)) return;

        Player p = e.getPlayer();
        if (p.hasPermission("astrax.chatfilter.bypass")) return;

        String original = e.getMessage();
        String hit = plugin.getFilter().find(original);
        if (hit == null) return;

        boolean censor = plugin.getConfig().getString("filter.mode", "BLOCK").equalsIgnoreCase("CENSOR");
        boolean handled = false;

        if (censor) {
            String censored = plugin.getFilter().censor(original);
            if (!censored.equals(original)) {
                e.setMessage(censored);
                p.sendMessage(plugin.msg("censored"));
                handled = true;
            }
        }
        if (!handled) {
            e.setCancelled(true);
            p.sendMessage(plugin.msg("blocked"));
        }

        int count = plugin.getViolations().merge(p.getUniqueId(), 1, Integer::sum);
        plugin.getLogger().info(p.getName() + " vi phạm chat (" + hit + "): " + original);

        if (plugin.getConfig().getBoolean("notify-staff", true)) {
            String note = plugin.msg("staff-notify")
                    .replace("{player}", p.getName())
                    .replace("{word}", hit)
                    .replace("{count}", String.valueOf(count));
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("astrax.chatfilter.notify")) staff.sendMessage(note);
            }
        }

        if (plugin.getConfig().getBoolean("punishment.enabled", false)
                && count >= plugin.getConfig().getInt("punishment.violations", 3)) {
            plugin.getViolations().remove(p.getUniqueId());
            List<String> cmds = plugin.getConfig().getStringList("punishment.commands");
            // Chat event chạy async -> phải chạy lệnh trên main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String c : cmds) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.replace("{player}", p.getName()));
                }
            });
        }
    }
}
