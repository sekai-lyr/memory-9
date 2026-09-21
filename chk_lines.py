import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\control\SekaiFormModelControl.java')
content = path.read_text('utf-8')
# Show around line 70-90
lines = content.split('\n')
for i in range(65, min(95, len(lines))):
    print(f'{i+1}: {lines[i]}')
