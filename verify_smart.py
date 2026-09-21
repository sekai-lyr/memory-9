import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')
idx = content.find('function smartReply')
snippet = content[idx:idx+500]
# Check if there are actual Chinese chars
has_chinese = any('\u4e00' <= c <= '\u9fff' for c in snippet)
print(f'Has Chinese: {has_chinese}')
print(snippet[:300])
