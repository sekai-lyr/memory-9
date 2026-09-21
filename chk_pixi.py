import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')
# Find the PIXI Application init
idx = content.find('new PIXI.Application')
print(content[idx:idx+300])
print()
# Find the document ready handler
idx2 = content.find('DOMContentLoaded')
if idx2 >= 0:
    print(content[idx2-30:idx2+80])
else:
    print('No DOMContentLoaded found')
