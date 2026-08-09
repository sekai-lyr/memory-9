package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Random;

public class ConstellationTool implements Tool {
    private static final Random RANDOM = new Random();
    private static final String[][] CONSTELLATIONS = {
        {"aries","\u767d\u7f8a\u5ea7","\u2648"},{"taurus","\u91d1\u725b\u5ea7","\u2649"},
        {"gemini","\u53cc\u5b50\u5ea7","\u264a"},{"cancer","\u5de8\u87f9\u5ea7","\u264b"},
        {"leo","\u72ee\u5b50\u5ea7","\u264c"},{"virgo","\u5904\u5973\u5ea7","\u264d"},
        {"libra","\u5929\u79e4\u5ea7","\u264e"},{"scorpio","\u5929\u874e\u5ea7","\u264f"},
        {"sagittarius","\u5c04\u624b\u5ea7","\u2650"},{"capricorn","\u6469\u7faf\u5ea7","\u2651"},
        {"aquarius","\u6c34\u74f6\u5ea7","\u2652"},{"pisces","\u53cc\u9c7c\u5ea7","\u2653"},
    };

    @Override public String name() { return "get_constellation"; }
    @Override public String description() {
        return "\u67e5\u8be2\u6307\u5b9a\u661f\u5ea7\u7684\u4eca\u65e5\u8fd0\u52bf\uff0c\u5305\u62ec\u7efc\u5408\u3001\u7231\u60c5\u3001\u4e8b\u4e1a\u3001\u8d22\u8fd0\u3001\u5065\u5eb7";
    }
    @Override public ObjectNode getParametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        ObjectNode props = JsonNodeFactory.instance.objectNode();
        ObjectNode c = JsonNodeFactory.instance.objectNode();
        c.put("type", "string");
        c.put("description", "\u661f\u5ea7\u540d\u79f0\uff0c\u5982\u767d\u7f8a\u5ea7\u3001\u72ee\u5b50\u5ea7\u3001\u53cc\u9c7c\u5ea7\u7b49");
        props.set("constellation", c);
        params.set("properties", props);
        ArrayNode req = JsonNodeFactory.instance.arrayNode();
        req.add("constellation");
        params.set("required", req);
        return params;
    }
    @Override public String execute(ObjectNode args) {
        String key = args.has("constellation") ? args.get("constellation").asText().trim() : "";
        String[] meta = resolve(key);
        if (meta == null) return "\u672a\u8bc6\u522b\u7684\u661f\u5ea7\uff1a" + key;
        String emoji = meta[2], cnName = meta[1];
        long seed = meta[0].hashCode() ^ System.currentTimeMillis() / 86400000;
        Random rng = new Random(seed);
        int stars = rng.nextInt(3) + 3;
        StringBuilder sb = new StringBuilder();
        sb.append(emoji).append(" ").append(cnName).append(" \u4eca\u65e5\u8fd0\u52bf\n");
        sb.append("\u2605".repeat(stars)).append("\u2606".repeat(5-stars));
        sb.append(" (").append(stars).append("/5)\n\n");
        sb.append("\ud83d\udccb \u7efc\u5408\uff1a").append(pick(OVERALL, cnName, rng)).append("\n\n");
        sb.append("\ud83d\udc84 \u7231\u60c5\uff1a").append(pick(LOVE, cnName, rng)).append("\n\n");
        sb.append("\ud83d\udcbc \u4e8b\u4e1a\uff1a").append(pick(CAREER, cnName, rng)).append("\n\n");
        sb.append("\ud83d\udcb5 \u8d22\u8fd0\uff1a").append(pick(WEALTH, cnName, rng)).append("\n\n");
        sb.append("\ud83c\udfe5 \u5065\u5eb7\uff1a").append(pick(HEALTH, cnName, rng)).append("\n\n");
        String[] colors = {"\u7ea2\u8272","\u84dd\u8272","\u7eff\u8272","\u7d2b\u8272","\u91d1\u8272","\u94f6\u8272","\u7c89\u8272","\u767d\u8272","\u6a59\u8272","\u9ec4\u8272"};
        sb.append("\ud83c\udfa8 \u5e78\u8fd0\u8272\uff1a").append(colors[rng.nextInt(colors.length)]);
        sb.append("  \ud83d\udd11 \u5e78\u8fd0\u6570\u5b57\uff1a").append(rng.nextInt(99)+1);
        return sb.toString();
    }
    private String[] resolve(String key) {
        for (String[] row : CONSTELLATIONS) {
            if (row[0].equalsIgnoreCase(key) || row[1].equals(key)) return row;
            if (row[1].replace("\u5ea7","").equals(key)) return row;
        }
        return null;
    }
    private String pick(String[] pool, String cnName, Random rng) {
        return pool[rng.nextInt(pool.length)].replace("{c}", cnName);
    }
    private static final String[] OVERALL = {
        "{c}\u4eca\u5929\u8fd0\u52bf\u4e0d\u9519\uff0c\u51e1\u4e8b\u987a\u5229\uff0c\u8d35\u4eba\u8fd0\u65fa\u76db\u3002",
        "{c}\u6574\u4f53\u72b6\u6001\u6781\u4f73\uff0c\u5bb9\u6613\u83b7\u5f97\u4ed6\u4eba\u8ba4\u53ef\u3002",
        "{c}\u4eca\u65e5\u8fd0\u52bf\u4e0a\u626c\uff0c\u9002\u5408\u4e3b\u52a8\u51fa\u51fb\u3002",
        "{c}\u7efc\u5408\u8fd0\u52bf\u7a33\u4e2d\u6709\u5347\uff0c\u662f\u5904\u7406\u79ef\u538b\u4e8b\u52a1\u7684\u597d\u65f6\u673a\u3002",
        "{c}\u8fd0\u52bf\u5e73\u7a33\uff0c\u6309\u90e8\u5c31\u73ed\u5373\u53ef\uff0c\u65e0\u9700\u62c5\u5fc3\u3002",
        "{c}\u6574\u4f53\u8fd0\u52bf\u4e00\u822c\uff0c\u6ce8\u610f\u52b3\u9038\u7ed3\u5408\u3002",
        "{c}\u4eca\u65e5\u8fd0\u52bf\u7565\u663e\u4f4e\u8ff7\uff0c\u5efa\u8bae\u4fdd\u6301\u4f4e\u8c03\u3002",
    };
    private static final String[] LOVE = {
        "{c}\u6843\u82b1\u8fd0\u5f88\u65fa\uff0c\u5355\u8eab\u8005\u6709\u671b\u9047\u5230\u5fc3\u52a8\u4e4b\u4eba\u3002",
        "\u7231\u60c5\u8fd0\u751c\u871c\uff0c{c}\u4e0e\u4f34\u4fa3\u9ed8\u5951\u63d0\u5347\u3002",
        "{c}\u9b45\u529b\u503c\u98d9\u5347\uff0c\u6697\u604b\u4f60\u7684\u4eba\u53ef\u80fd\u4f1a\u4e3b\u52a8\u9760\u8fd1\u3002",
        "\u6709\u4f34\u7684{c}\u9002\u5408\u5b89\u6392\u6d6a\u6f2b\u7ea6\u4f1a\uff0c\u611f\u60c5\u5347\u6e29\u3002",
        "\u5355\u8eab{c}\u4eca\u5929\u9002\u5408\u53c2\u52a0\u793e\u4ea4\u6d3b\u52a8\u3002",
        "{c}\u611f\u60c5\u4e16\u754c\u9633\u5149\u660e\u5a9a\uff0c\u548c\u559c\u6b22\u7684\u4eba\u4e92\u52a8\u6109\u5feb\u3002",
    };
    private static final String[] CAREER = {
        "{c}\u5de5\u4f5c\u6548\u7387\u6781\u9ad8\uff0c\u9002\u5408\u5904\u7406\u590d\u6742\u4efb\u52a1\u3002",
        "{c}\u4eca\u5929\u4e8b\u4e1a\u8fd0\u4e0d\u9519\uff0c\u6709\u671b\u83b7\u5f97\u9886\u5bfc\u8d4f\u8bc6\u3002",
        "{c}\u56e2\u961f\u5408\u4f5c\u987a\u7545\uff0c\u9879\u76ee\u63a8\u8fdb\u987a\u5229\u3002",
        "{c}\u4eca\u5929\u9002\u5408\u590d\u76d8\u548c\u89c4\u5212\uff0c\u4e0d\u6025\u4e8e\u6267\u884c\u3002",
        "{c}\u804c\u573a\u4eba\u9645\u5173\u7cfb\u548c\u8c10\uff0c\u6c9f\u901a\u987a\u7545\u3002",
    };
    private static final String[] WEALTH = {
        "{c}\u8d22\u8fd0\u4e0d\u9519\uff0c\u53ef\u80fd\u6709\u610f\u5916\u6536\u5165\u3002",
        "{c}\u4eca\u65e5\u9002\u5408\u7406\u8d22\u89c4\u5212\uff0c\u4e0d\u9002\u5408\u5927\u989d\u6295\u8d44\u3002",
        "{c}\u8d22\u8fd0\u5e73\u7a33\uff0c\u6ce8\u610f\u63a7\u5236\u5f00\u652f\u3002",
        "{c}\u504f\u8d22\u8fd0\u4e0d\u9519\uff0c\u53ef\u80fd\u6536\u5230\u793c\u7269\u6216\u7ea2\u5305\u3002",
        "{c}\u8d22\u8fd0\u4e00\u822c\uff0c\u4e0d\u5b9c\u51b2\u52a8\u6d88\u8d39\u3002",
    };
    private static final String[] HEALTH = {
        "{c}\u8eab\u4f53\u72b6\u6001\u826f\u597d\uff0c\u7cbe\u529b\u5145\u6c9b\u3002",
        "{c}\u6ce8\u610f\u9888\u690e\u4fdd\u62a4\uff0c\u907f\u514d\u957f\u65f6\u95f4\u4f4e\u5934\u3002",
        "{c}\u9002\u5408\u8f7b\u5ea6\u8fd0\u52a8\uff0c\u4fdd\u6301\u6d3b\u529b\u3002",
        "{c}\u591a\u559d\u6c34\uff0c\u6ce8\u610f\u996e\u98df\u5747\u8861\u3002",
        "{c}\u7761\u7720\u8d28\u91cf\u91cd\u8981\uff0c\u65e9\u70b9\u4f11\u606f\u3002",
    };
}
