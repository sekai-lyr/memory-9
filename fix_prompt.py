import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Find the old method
idx = content.find('getDefaultSystemPrompt')
if idx >= 0:
    print(f'getDefaultSystemPrompt found at position {idx}')
    # Show context
    snippet = content[idx-20:idx+800]
    print(snippet[:600])
else:
    print('NOT FOUND')
