import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Find the chat method and add pre-processing before the LLM call
# Find: "messages.add(chatMessage(\"user\", message));"
# Add tool detection and direct tool calling before this

old_line = '            messages.add(chatMessage(\"user\", message));'

new_block = '''            // === 7.24.pm Intent Detection & Direct Tool Calling ===
            String toolResult = null;
            String lowerMsg = message.toLowerCase();
            boolean isWeather = lowerMsg.contains(\"\u5929\u6c14\") || lowerMsg.contains(\"weather\") 
                             || lowerMsg.contains(\"\u6e29\u5ea6\") || lowerMsg.contains(\"\u4e0b\u96e8\")
                             || lowerMsg.contains(\"\u70ed\") && lowerMsg.contains(\"\u4e0d\") == false;
            boolean isJoke = lowerMsg.contains(\"\u7b11\u8bdd\") || lowerMsg.contains(\"joke\")
                          || lowerMsg.contains(\"\u641e\u7b11\") || lowerMsg.contains(\"\u8bb2\u4e2a\");
            
            if (isWeather) {
                // Extract city from message
                String city = extractCity(message);
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    args.put(\"city\", city);
                    Tool wt = toolRegistry.get(\"get_weather\");
                    if (wt != null) toolResult = wt.execute(args);
                } catch (Exception ignored) {}
            } else if (isJoke) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    args.put(\"count\", 1);
                    Tool jt = toolRegistry.get(\"tell_joke\");
                    if (jt != null) toolResult = jt.execute(args);
                } catch (Exception ignored) {}
            }
            
            // If tool was called, inject result as system context
            if (toolResult != null && !toolResult.isBlank()) {
                messages.add(chatMessage(\"system\", \"\u3010\u5de5\u5177\u8fd4\u56de\u7ed3\u679c\u3011\\n\" + toolResult + \"\\n\u8bf7\u6839\u636e\u4e0a\u9762\u7684\u6570\u636e\uff0c\u7528Haru\u7684\u53e3\u543b\u6e29\u6696\u5730\u56de\u590d\u7528\u6237\uff0c\u683c\u5f0f\u8981\u7f8e\u89c2\u3002\"));\n            }\n\n            messages.add(chatMessage(\"user\", message));'''

if old_line in content:
    content = content.replace(old_line, new_block)
    path.write_text(content, 'utf-8')
    print('ADDED: Intent detection + direct tool calling')
else:
    print('Pattern not found')
    idx = content.find('messages.add(chatMessage')
    if idx >= 0:
        print(content[idx-20:idx+100])
