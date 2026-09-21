import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\templates\live2d.html')
content = path.read_text('utf-8')
# Find chat-win-input area
idx = content.find('chat-win-input')
if idx >= 0:
    print('--- chat-win-input section ---')
    print(content[max(0,idx-200):idx+800])
print()
print('=== BOTTOM chat-area ===')
idx2 = content.find('#chat-area{')
if idx2 >= 0:
    print(content[idx2:idx2+500])
