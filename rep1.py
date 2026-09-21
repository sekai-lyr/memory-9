import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

old_start = content.index('    private String getDefaultSystemPrompt() {')
old_end = content.index(';\n    }', old_start + 50) + 5
old_block = content[old_start:old_end]
print(f'Old block: {len(old_block)} chars')
print(old_block[:100])
