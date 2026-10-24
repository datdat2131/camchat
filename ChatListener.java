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

        // Ký tự cấm tuyệt đối (vd: &) -> chỉ cần có là chặn luôn, không che
        String hit = findBlockedChar(original);
        boolean forceBlock = hit != null;
        if (hit == null) hit = plugin.getFilter().find(original);
        if (hit == null) return;

        boolean censor = !forceBlock
                && plugin.getConfig().getString("filter.mode", "BLOCK").equalsIgnoreCase("CENSOR");
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
            p.sendMessage(plugin.msg(forceBlock ? "blocked-char" : "blocked"));
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

    /** Trả về ký tự cấm đầu tiên có trong tin nhắn, hoặc null. */
    private String findBlockedChar(String message) {
        if (!plugin.getConfig().getBoolean("blocked-characters.enabled", true)) return null;
        for (String ch : plugin.getConfig().getStringList("blocked-characters.characters")) {
            if (!ch.isEmpty() && message.contains(ch)) return ch;
        }
        return null;
    }
}
