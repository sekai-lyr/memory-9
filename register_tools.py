import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Register new tools
old_reg = 'toolRegistry.register(new WeatherTool());\n        toolRegistry.register(new JokeTool());'
new_reg = 'toolRegistry.register(new WeatherTool());\n        toolRegistry.register(new JokeTool());\n        toolRegistry.register(new CurrentTimeTool());\n        toolRegistry.register(new TranslateTool());\n        toolRegistry.register(new NewsTool());\n        toolRegistry.register(new ConstellationTool());'
content = content.replace(old_reg, new_reg)

# Update system prompt to mention new tools
old_prompt_end = '"- \u7ed3\u5408\u5f53\u524d\u65f6\u95f4\u7ed9\u5408\u9002\u5173\u5fc3";'
new_prompt_end = '"- \u7ed3\u5408\u5f53\u524d\u65f6\u95f4\u7ed9\u5408\u9002\u5173\u5fc3\\n\" +\n               \"- \u7528\u6237\u95ee\u65f6\u95f4\u3001\u65e5\u671f -> \u8c03\u7528 get_current_time\\n\" +\n               \"- \u7528\u6237\u8981\u7ffb\u8bd1 -> \u8c03\u7528 translate\\n\" +\n               \"- \u7528\u6237\u95ee\u65b0\u95fb\u3001\u70ed\u641c -> \u8c03\u7528 get_news\\n\" +\n               \"- \u7528\u6237\u95ee\u661f\u5ea7\u8fd0\u52bf -> \u8c03\u7528 get_constellation\";'
content = content.replace(old_prompt_end, new_prompt_end)

path.write_text(content, 'utf-8')
print('Tools registered and prompt updated')
