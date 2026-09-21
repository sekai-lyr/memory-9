import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')
content = content.replace('// ===== ?????? =====', '// ===== Danmaku System =====')
# Also fix any other garbled comments
content = content.replace('// --- ???? ---', '// --- Init ---')
content = content.replace('// --- ???? ---', '// --- Chat ---')
path.write_text(content, 'utf-8')

# Now check HTML for garbled
path2 = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\templates\live2d.html')
html = path2.read_text('utf-8')
import re
garbled = re.findall(r'\?{3,}', html)
print(f'HTML garbled sequences: {len(garbled)}')
for m in re.finditer(r'\?{3,}', html):
    start = max(0, m.start()-20)
    end = min(len(html), m.end()+20)
    print(f'  at {m.start()}: ...{html[start:end]}...')

# Fix remaining garbled in HTML
html = html.replace('/* ===== ???????????? ===== */', '/* ===== Danmaku ===== */')
html = html.replace('/* ???bubble?????? */', '/* Thinking Bubble */')
html = html.replace('/* Chat window panel */', '/* Chat Window Panel */')
html = html.replace('/* Stats panel */', '/* Stats Panel */')
path2.write_text(html, 'utf-8')
print('HTML garbled fixed')
