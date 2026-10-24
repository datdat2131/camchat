package vn.astrax.chatfilter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FilterCommand implements CommandExecutor, TabCompleter {

    private final ChatFilterPlugin plugin;

    public FilterCommand(ChatFilterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("astrax.chatfilter.admin")) {
            s.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (a.length == 0) {
            s.sendMessage(plugin.msg("usage"));
            return true;
        }

        List<String> list = new ArrayList<>(plugin.getConfig().getStringList("banned-words"));

        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                plugin.reload();
                s.sendMessage(plugin.msg("reloaded"));
                break;

            case "toggle": {
                boolean now = !plugin.getConfig().getBoolean("enabled", true);
                plugin.getConfig().set("enabled", now);
                plugin.saveConfig();
                s.sendMessage(plugin.msg("toggled").replace("{state}", now ? "&aBẬT" : "&cTẮT"));
                break;
            }

            case "list":
                s.sendMessage(plugin.msg("list")
                        .replace("{count}", String.valueOf(list.size()))
                        .replace("{words}", String.join(", ", list)));
                break;

            case "add": {
                if (a.length < 2) { s.sendMessage(plugin.msg("usage")); break; }
                String word = String.join(" ", Arrays.copyOfRange(a, 1, a.length));
                if (list.stream().anyMatch(w -> w.equalsIgnoreCase(word))) {
                    s.sendMessage(plugin.msg("already-exists"));
                    break;
                }
                list.add(word);
                save(list);
                s.sendMessage(plugin.msg("added").replace("{word}", word));
                break;
            }

            case "remove": {
                if (a.length < 2) { s.sendMessage(plugin.msg("usage")); break; }
                String word = String.join(" ", Arrays.copyOfRange(a, 1, a.length));
                if (!list.removeIf(w -> w.equalsIgnoreCase(word))) {
                    s.sendMessage(plugin.msg("not-found"));
                    break;
                }
                save(list);
                s.sendMessage(plugin.msg("removed").replace("{word}", word));
                break;
            }

            default:
                s.sendMessage(plugin.msg("usage"));
        }
        return true;
    }

    private void save(List<String> list) {
        plugin.getConfig().set("banned-words", list);
        plugin.saveConfig();
        plugin.getFilter().load();
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        if (!s.hasPermission("astrax.chatfilter.admin")) return new ArrayList<>();
        if (a.length == 1) {
            List<String> out = new ArrayList<>();
            for (String o : Arrays.asList("reload", "add", "remove", "list", "toggle")) {
                if (o.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(o);
            }
            return out;
        }
        if (a.length == 2 && a[0].equalsIgnoreCase("remove")) {
            List<String> out = new ArrayList<>();
            for (String w : plugin.getConfig().getStringList("banned-words")) {
                if (w.toLowerCase(Locale.ROOT).startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(w);
            }
            return out;
        }
        return new ArrayList<>();
    }
}
