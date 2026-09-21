import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')
# Find where preToolResult is used after intent detection
idx = content.find('preToolResult != null')
if idx >= 0:
    print(content[idx-100:idx+400])
else:
    print('NOT FOUND')
    # Search for where the result should be injected
    idx2 = content.find('preToolResult')
    while idx2 >= 0:
        print(f'preToolResult at {idx2}:')
        print(content[max(0,idx2-20):idx2+80])
        idx2 = content.find('preToolResult', idx2+1)
