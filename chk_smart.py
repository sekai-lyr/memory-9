import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
print('File size:', path.stat().st_size, 'bytes')
content = path.read_text('utf-8')
idx = content.find('function smartReply')
if idx >= 0:
    end_idx = content.find('\nfunction ', idx + 50)
    if end_idx == -1:
        end_idx = content.find('\nasync function', idx + 50)
    print(f'smartReply at {idx}, next function at {end_idx}')
    print(content[idx:end_idx])
