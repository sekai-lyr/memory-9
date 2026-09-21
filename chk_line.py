import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')
lines = content.split('\n')
for i in range(240, 255):
    print(f'{i+1}: {lines[i]}')
