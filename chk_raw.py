import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')
lines = content.split('\n')
# Show raw lines 295-305
for i in range(294, min(310, len(lines))):
    print(f'{i+1}: {repr(lines[i])}')
