import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')
# Find intent detection section
idx = content.find('Intent Detection')
if idx >= 0:
    print(content[idx:idx+1500])
else:
    print('Intent Detection NOT FOUND')
    # Search for preToolResult
    idx2 = content.find('preToolResult')
    if idx2 >= 0:
        print(f'preToolResult at {idx2}:')
        print(content[idx2-100:idx2+500])
