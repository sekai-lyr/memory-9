import pathlib, re
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')

# Remove async from all functions first
content = re.sub(r'async function ', 'function ', content)

# Now find which functions ACTUALLY use await and add async back
funcs = re.findall(r'function (\w+)\([^)]*\)\s*\{', content)
need_async = []
for name in funcs:
    idx = content.find(f'function {name}(')
    # Find the matching closing brace
    brace_count = 0
    started = False
    end = idx
    for i in range(idx, len(content)):
        if content[i] == '{':
            brace_count += 1
            started = True
        elif content[i] == '}':
            brace_count -= 1
            if started and brace_count == 0:
                end = i + 1
                break
    body = content[idx:end]
    if 'await ' in body:
        need_async.append(name)

print(f'Functions needing async: {len(need_async)}')
for n in need_async:
    print(f'  {n}')

# Add async to just those functions
for name in need_async:
    old = f'function {name}('
    new = f'async function {name}('
    content = content.replace(old, new)

path.write_text(content, 'utf-8')
print('Fixed - only added async where needed')
