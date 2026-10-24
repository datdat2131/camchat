package vn.astrax.chatfilter;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatFilterPlugin extends JavaPlugin {

    private WordFilter filter;
    private final Map<UUID, Integer> violations = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        filter = new WordFilter(this);
        filter.load();

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        PluginCommand cmd = getCommand("chatfilter");
        if (cmd != null) {
            FilterCommand handler = new FilterCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
        getLogger().info("Astrax ChatFilter đã bật. Số từ cấm: " + filter.size());
    }

    public void reload() {
        reloadConfig();
        filter.load();
        violations.clear();
    }

    public WordFilter getFilter() {
        return filter;
    }

    public Map<UUID, Integer> getViolations() {
        return violations;
    }

    /** Lấy message từ config, đã thay màu. */
    public String msg(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String text = getConfig().getString("messages." + key, key);
        return color(prefix + text);
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
