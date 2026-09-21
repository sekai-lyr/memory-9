import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')
# Get the full init function
idx = content.find('async function init()')
end = content.find('\nasync function switchModel', idx)
if end == -1: end = content.find('\nfunction switchModel', idx)
if end == -1: end = idx + 3000
init_code = content[idx:end]
print(init_code[:2000])
