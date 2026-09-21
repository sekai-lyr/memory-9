import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')
# Check for other potential issues
# Look for non-async functions using await
import re
# Check all function declarations
funcs = re.findall(r'(async\s+)?function\s+(\w+)', content)
print(f'Total functions: {len(funcs)}')
for is_async, name in funcs:
    if not is_async:
        # Check if this function uses await
        idx = content.find(f'function {name}')
        end = content.find('\nfunction ', idx + 10)
        if end == -1: end = content.find('\nasync function', idx + 10)
        if end == -1: end = len(content)
        body = content[idx:end]
        if 'await ' in body:
            print(f'  WARNING: {name} uses await but is NOT async!')
