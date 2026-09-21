import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Add tool_choice: auto after tools
old_tools = 'if (!toolRegistry.isEmpty()) {\n                    body.set(\"tools\", toolRegistry.getToolDefinitions());\n                }'
new_tools = 'if (!toolRegistry.isEmpty()) {\n                    body.set(\"tools\", toolRegistry.getToolDefinitions());\n                    body.put(\"tool_choice\", \"auto\");\n                }'

if old_tools in content:
    content = content.replace(old_tools, new_tools)
    path.write_text(content, 'utf-8')
    print('ADDED: tool_choice auto')
else:
    print('Pattern not found')
    idx = content.find('toolRegistry.isEmpty()')
    if idx >= 0:
        print(content[idx-50:idx+200])
