import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Find the injection code and change it to modify the user message
old_inject = '''if (preToolResult != null && !preToolResult.isBlank()) {
                messages.add(chatMessage(\"system\", \"\u3010\u5de5\u5177\u8fd4\u56de\u7ed3\u679c\u3011\\n\" + preToolResult + \"\\n\u8bf7\u6839\u636e\u4e0a\u9762\u7684\u6570\u636e\uff0c\u7528Haru\u7684\u53e3\u543b\u6e29\u6696\u5730\u56de\u590d\u7528\u6237\uff0c\u683c\u5f0f\u8981\u7f8e\u89c2\u3002\"));
            }'''

new_inject = '''if (preToolResult != null && !preToolResult.isBlank()) {
                message = message + \"\\n\\n\u3010\u7cfb\u7edf\u5df2\u67e5\u8be2\u5230\u4ee5\u4e0b\u4fe1\u606f\uff0c\u8bf7\u76f4\u63a5\u7528\u8fd9\u4e9b\u6570\u636e\u56de\u590d\u7528\u6237\uff0c\u4e0d\u8981\u518d\u8bf4\u201c\u6211\u5e2e\u4f60\u67e5\u67e5\u201d\u4e4b\u7c7b\u7684\u8bdd\u3011\\n\" + preToolResult;
            }'''

content = content.replace(old_inject, new_inject)
path.write_text(content, 'utf-8')
print('Changed: weather data now injected into user message')
