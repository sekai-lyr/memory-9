import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\templates\live2d.html')
content = path.read_text('utf-8')
print(f'File size: {len(content)} chars')
# Find chat window section
idx = content.find('chat-window')
if idx >= 0:
    print(content[max(0,idx-50):idx+2000])
