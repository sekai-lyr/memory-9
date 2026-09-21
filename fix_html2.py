import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\templates\live2d.html')
content = path.read_text('utf-8')

# Fix #chat-area HTML: reorder to put img-btn before the input (WeChat style)
old_area = '''<div id="chat-area">
    <input id="chat-input" placeholder="\u548c\u770b\u677f\u5a18\u804a\u5929... (Enter\u53d1\u9001)" onkeydown="if(event.key==='Enter')sendChat()">
    <button type="button" id="chat-img-btn" onclick="document.getElementById('chat-img-upload-bottom').click()" title="\u53d1\u9001\u56fe\u7247">\U0001f5bc\ufe0f</button>
    <input type="file" id="chat-img-upload-bottom" accept="image/*" onchange="onChatImageUpload(this)" style="position:fixed;top:-100px;left:-100px">
    <button type="button" id="chat-send" onclick="sendChat()">\u53d1\u9001</button>
</div>'''

new_area = '''<div id="chat-area">
    <button type="button" id="chat-img-btn" onclick="document.getElementById('chat-img-upload-bottom').click()" title="\u53d1\u9001\u56fe\u7247">\U0001f5bc\ufe0f</button>
    <input type="file" id="chat-img-upload-bottom" accept="image/*" onchange="onChatImageUpload(this)" style="position:fixed;top:-100px;left:-100px">
    <input id="chat-input" placeholder="\u548c\u770b\u677f\u5a18\u804a\u5929... (Enter\u53d1\u9001)" onkeydown="if(event.key==='Enter')sendChat()">
    <button type="button" id="chat-send" onclick="sendChat()">\u53d1\u9001</button>
</div>'''

if old_area in content:
    content = content.replace(old_area, new_area)
    print('Bottom chat area HTML reordered')
else:
    print('Could not find exact old_area match')
    # Try to find it loosely
    idx = content.find('chat-area')
    if idx >= 0:
        print(content[idx:idx+500])

path.write_text(content, 'utf-8')
print('HTML final save done')
