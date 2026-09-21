import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')

# Fix all non-async functions that use await
fixes = [
    ('function showFloatText(', 'async function showFloatText('),
    ('function startProximityCheck(', 'async function startProximityCheck('),
    ('function onChatImageUpload(', 'async function onChatImageUpload('),
    ('function extractImagePrompt(', 'async function extractImagePrompt('),
]

for old, new in fixes:
    if old in content:
        content = content.replace(old, new)
        print(f'Fixed: {old} -> {new}')
    else:
        print(f'Not found: {old}')

path.write_text(content, 'utf-8')
print('All async fixes applied')
