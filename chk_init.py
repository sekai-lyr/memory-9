import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')
# Find the init/load model functions
for keyword in ['function init', 'function switchModel', 'function loadModel', 'async function loadModel', 'fetch.*models']:
    idx = content.find(keyword)
    if idx >= 0:
        end = content.find('\nfunction ', idx + 10)
        if end == -1: end = content.find('\nasync function', idx + 10)
        if end == -1: end = idx + 1500
        print(f'=== {keyword} ===')
        print(content[idx:min(end, idx+1200)])
        print()
