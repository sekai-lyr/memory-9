import pathlib
# Check ToolRegistry output format
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\tool\ToolRegistry.java')
print(path.read_text('utf-8'))
print('---')
path2 = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\tool\Tool.java')
print(path2.read_text('utf-8'))
