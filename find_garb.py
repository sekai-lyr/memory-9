import pathlib, re
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')
for m in re.finditer(r'\?{3,}', content):
    start = max(0, m.start()-30)
    end = min(len(content), m.end()+30)
    print(f'Garbled at {m.start()}: ...{content[start:end]}...')
