import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')
# Find ALL occurrences of preToolResult
pos = 0
while True:
    idx = content.find('preToolResult', pos)
    if idx == -1: break
    print(f'--- preToolResult at {idx} ---')
    print(repr(content[idx:idx+100]))
    print()
    pos = idx + 10
