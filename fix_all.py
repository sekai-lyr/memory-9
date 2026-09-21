import pathlib, re
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')

# Replace ALL 'function ' with 'async function ' but skip ones already async
# Use a negative lookbehind: function not preceded by 'async '
content = re.sub(r'(?<!async )function ', 'async function ', content)

path.write_text(content, 'utf-8')
print('All functions now async')

# Verify no issues remain
funcs = re.findall(r'(?<!async )function (\w+)', content)
print(f'Non-async functions remaining: {len(funcs)}')
if funcs:
    print(funcs[:10])
