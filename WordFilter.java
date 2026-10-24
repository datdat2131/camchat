package vn.astrax.chatfilter;

import org.bukkit.configuration.file.FileConfiguration;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class WordFilter {

    private final ChatFilterPlugin plugin;

    private List<String> words = new ArrayList<>();
    private List<Pattern> regexes = new ArrayList<>();
    private List<String> whitelist = new ArrayList<>();
    private boolean strict;
    private boolean leet;

    public WordFilter(ChatFilterPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();
        strict = c.getBoolean("filter.strict-mode", false);
        leet = c.getBoolean("filter.leet-detection", true);

        words = new ArrayList<>();
        for (String w : c.getStringList("banned-words")) {
            String n = clean(w);
            if (!n.isEmpty() && !words.contains(n)) words.add(n);
        }

        whitelist = new ArrayList<>();
        for (String w : c.getStringList("whitelist")) {
            String n = clean(w);
            if (!n.isEmpty()) whitelist.add(n);
        }

        regexes = new ArrayList<>();
        for (String r : c.getStringList("banned-regex")) {
            try {
                regexes.add(Pattern.compile(r, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            } catch (PatternSyntaxException ex) {
                plugin.getLogger().warning("Regex sai, bỏ qua: " + r);
            }
        }
    }

    public int size() {
        return words.size() + regexes.size();
    }

    /** Bỏ dấu tiếng Việt, hạ chữ thường, (tùy chọn) đổi leet. */
    private String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        if (leet) {
            StringBuilder sb = new StringBuilder(n.length());
            for (char ch : n.toCharArray()) {
                switch (ch) {
                    case '0': sb.append('o'); break;
                    case '1': sb.append('i'); break;
                    case '3': sb.append('e'); break;
                    case '4': case '@': sb.append('a'); break;
                    case '5': case '$': sb.append('s'); break;
                    default: sb.append(ch);
                }
            }
            n = sb.toString();
        }
        return n;
    }

    /** Chuẩn hóa + thay ký tự lạ bằng khoảng trắng. */
    private String clean(String s) {
        return normalize(s).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Trả về từ cấm đầu tiên tìm thấy, hoặc null nếu sạch. */
    public String find(String message) {
        String n = " " + clean(message) + " ";

        for (String wl : whitelist) {
            n = n.replace(" " + wl + " ", " ");
        }
        n = n.trim();

        if (strict) {
            String compact = n.replace(" ", "");
            for (String w : words) {
                if (compact.contains(w.replace(" ", ""))) return w;
            }
        } else {
            String padded = " " + n + " ";
            for (String w : words) {
                if (padded.contains(" " + w + " ")) return w;
            }
        }

        for (Pattern p : regexes) {
            if (p.matcher(message).find()) return p.pattern();
        }
        return null;
    }

    /** Thay từ cấm bằng *** trên tin nhắn gốc (giữ nguyên phần còn lại). */
    public String censor(String message) {
        String text = Normalizer.normalize(message, Normalizer.Form.NFD);
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        String sepAny = "[^\\p{L}\\p{N}]";

        for (String w : words) {
            StringBuilder sb = new StringBuilder();
            if (!strict) sb.append("(?<![\\p{L}\\p{N}])");
            for (int i = 0; i < w.length(); i++) {
                char c = w.charAt(i);
                if (c == ' ') {
                    sb.append(sepAny).append('+');
                    continue;
                }
                sb.append(c == 'd' ? "[dđ]" : Pattern.quote(String.valueOf(c))).append("\\p{M}*");
                if (strict && i < w.length() - 1 && w.charAt(i + 1) != ' ') {
                    sb.append(sepAny).append('*');
                }
            }
            if (!strict) sb.append("(?![\\p{L}\\p{N}])");

            Matcher m = Pattern.compile(sb.toString(), flags).matcher(text);
            StringBuffer out = new StringBuffer();
            while (m.find()) {
                int len = m.group().replaceAll("\\p{M}+", "").length();
                m.appendReplacement(out, Matcher.quoteReplacement("*".repeat(Math.max(1, len))));
            }
            m.appendTail(out);
            text = out.toString();
        }

        for (Pattern p : regexes) {
            Matcher m = p.matcher(text);
            StringBuffer out = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(out, Matcher.quoteReplacement("*".repeat(Math.max(1, m.group().length()))));
            }
            m.appendTail(out);
            text = out.toString();
        }

        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }
}
