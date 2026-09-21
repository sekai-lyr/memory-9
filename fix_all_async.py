import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')

# Find ALL occurrences of 'async async' and 'async function' duplication
import re
matches = list(re.finditer(r'async\s+async', content))
print(f'Found {len(matches)} double async occurrences')
for m in matches:
    start = max(0, m.start()-10)
    end = min(len(content), m.end()+40)
    print(f'  at {m.start()}: {content[start:end]}')

# Fix all double async
content = content.replace('async async ', 'async ')
path.write_text(content, 'utf-8')

# Verify
if 'async async' in content:
    print('STILL HAS DOUBLE ASYNC!')
else:
    print('All double async fixed')
