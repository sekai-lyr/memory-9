import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')
if 'preToolResult' in content:
    print('preToolResult EXISTS')
if 'extractCity' in content:
    print('extractCity EXISTS')
if 'Intent Detection' in content:
    print('Intent Detection EXISTS')
