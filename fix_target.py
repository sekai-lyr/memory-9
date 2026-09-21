import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\control\SekaiFormModelControl.java')
content = path.read_text('utf-8')

# Initialize targetDir at the start
old_decl = 'Path targetDir;'
new_decl = 'Path targetDir = null;'
content = content.replace(old_decl, new_decl)

# Fix the else branch - now that 3D is removed, non-zip uploads should fail
old_else = '''}'''
# Find the specific context around the else branch
idx = content.find('targetDir = MODEL_BASE.resolve("2d").resolve(baseName);')
if idx >= 0:
    # Show context
    print(content[idx-50:idx+500])

path.write_text(content, 'utf-8')
