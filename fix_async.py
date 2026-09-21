import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\static\js\live2d-app.js')
content = path.read_text('utf-8')

# Fix: make init() async
content = content.replace('function init() {', 'async function init() {')

# Also fix switchModel - it uses await but might not be async
content = content.replace('function switchModel(name) {', 'async function switchModel(name) {')

# Fix loadModelList
content = content.replace('function loadModelList() {', 'async function loadModelList() {')

# Fix reloadModels  
content = content.replace('async function reloadModels(selectName) {', 'async function reloadModels(selectName) {')

path.write_text(content, 'utf-8')
print('Fixed async functions')
