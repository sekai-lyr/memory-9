import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')
content = content.replace('    } }\n\n    private ObjectNode chatMessage', '    }\n\n    private ObjectNode chatMessage')
path.write_text(content, 'utf-8')

# Verify
lines = content.split('\n')
print(f'Line 298: {repr(lines[297])}')
print(f'Line 299: {repr(lines[298])}')
print(f'Line 300: {repr(lines[299])}')
