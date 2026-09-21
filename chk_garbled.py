import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')
# Check for garbled areas (lots of ???)
import re
garbled = re.findall(r'\?{3,}', content)
print(f'Garbled sequences found: {len(garbled)}')
# Check overall Chinese content quality
chinese_chars = sum(1 for c in content if '\u4e00' <= c <= '\u9fff')
print(f'Chinese characters: {chinese_chars}')
