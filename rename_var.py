import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Rename my toolResult to preToolResult
content = content.replace('String toolResult = null;', 'String preToolResult = null;')
content = content.replace('if (wt != null) toolResult = wt.execute(args);', 'if (wt != null) preToolResult = wt.execute(args);')
content = content.replace('if (jt != null) toolResult = jt.execute(args);', 'if (jt != null) preToolResult = jt.execute(args);')
content = content.replace('if (toolResult != null && !toolResult.isBlank()) {', 'if (preToolResult != null && !preToolResult.isBlank()) {')
content = content.replace('messages.add(chatMessage(\"system\", \"\u3010\u5de5\u5177\u8fd4\u56de\u7ed3\u679c\u3011\\n\" + toolResult +', 'messages.add(chatMessage(\"system\", \"\u3010\u5de5\u5177\u8fd4\u56de\u7ed3\u679c\u3011\\n\" + preToolResult +')

path.write_text(content, 'utf-8')
print('Renamed to preToolResult')
