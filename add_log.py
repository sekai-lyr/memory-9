import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Add logging after intent detection
old_log = 'if (isWeather) {'
new_log = '''System.out.println(\"[Chat] message=\" + message + \" isWeather=\" + isWeather + \" isJoke=\" + isJoke);
            if (isWeather) {'''

content = content.replace(old_log, new_log)

# Add logging after tool execution
old_log2 = 'if (wt != null) preToolResult = wt.execute(args);'
new_log2 = '''if (wt != null) { preToolResult = wt.execute(args); System.out.println(\"[Chat] Weather result: \" + (preToolResult != null ? preToolResult.substring(0, Math.min(80, preToolResult.length())) : \"null\")); }'''

content = content.replace(old_log2, new_log2)

path.write_text(content, 'utf-8')
print('Added debug logging')
