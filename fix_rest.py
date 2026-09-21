import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')

fixes = [
    ('function doTrain(', 'async function doTrain('),
    ('function startIdle(', 'async function startIdle('),
    ('function clearChatHistory(', 'async function clearChatHistory('),
    ('function isImageGenCommand(', 'async function isImageGenCommand('),
]
for old, new in fixes:
    content = content.replace(old, new)

path.write_text(content, 'utf-8')

# Final verification
import re
funcs = re.findall(r'(async\s+)?function\s+(\w+)', content)
issues = []
for is_async, name in funcs:
    if not is_async:
        idx = content.find(f'function {name}')
        end = content.find('\nfunction ', idx + 10)
        if end == -1: end = content.find('\nasync function', idx + 10)
        if end == -1: end = len(content)
        if 'await ' in content[idx:end]:
            issues.append(name)
print(f'Remaining: {issues if issues else "NONE - all clean!"}')
