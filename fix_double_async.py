import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')

# Fix double async
content = content.replace('async async function loadModelList()', 'async function loadModelList()')
# Also check for other double async
content = content.replace('async async function reloadModels', 'async function reloadModels')
content = content.replace('async async function switchModel', 'async function switchModel')

path.write_text(content, 'utf-8')
print('Fixed double async')

# Verify
if 'async async' in content:
    print('STILL HAS DOUBLE ASYNC!')
    idx = content.index('async async')
    print(content[idx:idx+80])
else:
    print('No more double async')
