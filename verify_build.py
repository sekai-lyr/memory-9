import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')
# Check if preToolResult exists
if 'preToolResult' in content:
    print('preToolResult FOUND - code is in source')
    # Check if extractCity exists
    if 'extractCity' in content:
        print('extractCity FOUND')
else:
    print('preToolResult NOT FOUND!')
    
# Check class file timestamp
import os
src_time = os.path.getmtime(str(path))
class_path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\target\classes\com\sekai\sekai_form\service\impl\Live2DServiceImpl.class')
if class_path.exists():
    class_time = os.path.getmtime(str(class_path))
    print(f'Source time: {src_time}')
    print(f'Class time: {class_time}')
    print(f'Source newer: {src_time > class_time}')
