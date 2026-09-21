import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')
# Check if init is async
idx = content.find('function init()')
print(content[idx:idx+200])
