import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Add intent detection for new tools after joke detection
old_joke_end = 'lowerMsg.contains(\"\u641e\u7b11\") || lowerMsg.contains(\"\u8bb2\u4e2a\");'
new_intents = '''lowerMsg.contains("\u641e\u7b11") || lowerMsg.contains("\u8bb2\u4e2a");
            boolean isTranslate = lowerMsg.contains("\u7ffb\u8bd1") || lowerMsg.contains("translate")
                               || message.contains("\u82f1\u6587") || message.contains("\u65e5\u6587")
                               || message.contains("\u97e9\u6587") || message.contains("\u6cd5\u6587");
            boolean isNews = lowerMsg.contains("\u65b0\u95fb") || lowerMsg.contains("\u70ed\u641c")
                          || lowerMsg.contains("\u70ed\u70b9") || lowerMsg.contains("news");
            boolean isConstellation = message.contains("\u661f\u5ea7") || message.contains("\u8fd0\u52bf")
                                   || message.contains("\u767d\u7f8a") || message.contains("\u91d1\u725b")
                                   || message.contains("\u53cc\u5b50") || message.contains("\u5de8\u87f9")
                                   || message.contains("\u72ee\u5b50") || message.contains("\u5904\u5973")
                                   || message.contains("\u5929\u79e4") || message.contains("\u5929\u874e")
                                   || message.contains("\u5c04\u624b") || message.contains("\u6469\u7faf")
                                   || message.contains("\u6c34\u74f6") || message.contains("\u53cc\u9c7c");
            boolean isTime = lowerMsg.contains("\u51e0\u70b9") || lowerMsg.contains("\u65f6\u95f4")
                          || lowerMsg.contains("\u65e5\u671f") || lowerMsg.contains("\u661f\u671f\u51e0")
                          || lowerMsg.contains("\u4eca\u5929\u51e0\u53f7");'''

content = content.replace(old_joke_end, new_intents)

# Add tool execution branches after joke execution
old_joke_exec = 'if (jt != null) preToolResult = jt.execute(args);'
new_tool_execs = '''if (jt != null) preToolResult = jt.execute(args);
                } catch (Exception e) { System.err.println("[Chat] Tool pre-execution error: " + e.getMessage()); e.printStackTrace(); }
            } else if (isTranslate) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    args.put("text", message);
                    args.put("target_lang", message.contains("\u82f1\u6587") || message.contains("english") ? "en" : message.contains("\u65e5\u6587") ? "ja" : message.contains("\u97e9\u6587") ? "ko" : "zh");
                    Tool tt = toolRegistry.get("translate");
                    if (tt != null) preToolResult = tt.execute(args);
                } catch (Exception e) { System.err.println("[Chat] Translate error: " + e.getMessage()); }
            } else if (isNews) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    args.put("type", "baidu");
                    args.put("count", 5);
                    Tool nt = toolRegistry.get("get_news");
                    if (nt != null) preToolResult = nt.execute(args);
                } catch (Exception e) { System.err.println("[Chat] News error: " + e.getMessage()); }
            } else if (isConstellation) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    String[] signs = {"\u767d\u7f8a","\u91d1\u725b","\u53cc\u5b50","\u5de8\u87f9","\u72ee\u5b50","\u5904\u5973","\u5929\u79e4","\u5929\u874e","\u5c04\u624b","\u6469\u7faf","\u6c34\u74f6","\u53cc\u9c7c"};
                    String found = "\u767d\u7f8a\u5ea7";
                    for (String s : signs) { if (message.contains(s)) { found = s + "\u5ea7"; break; } }
                    args.put("constellation", found);
                    Tool ct = toolRegistry.get("get_constellation");
                    if (ct != null) preToolResult = ct.execute(args);
                } catch (Exception e) { System.err.println("[Chat] Constellation error: " + e.getMessage()); }
            } else if (isTime) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    args.put("timezone", "Asia/Shanghai");
                    Tool timet = toolRegistry.get("get_current_time");
                    if (timet != null) preToolResult = timet.execute(args);'''

content = content.replace(old_joke_exec, new_tool_execs)

path.write_text(content, 'utf-8')
print('Intent detection extended for new tools')
