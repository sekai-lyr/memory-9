import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\templates\live2d.html')
content = path.read_text('utf-8')
# Write to a temp file for reading
out = pathlib.Path(r'D:\Sekai_two\memory-9\live2d_dump.txt')
out.write_text(content, 'utf-8')
print(f'Written {len(content)} chars to live2d_dump.txt')
