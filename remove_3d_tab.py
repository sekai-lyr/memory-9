import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\templates\live2d.html')
content = path.read_text('utf-8')

# Remove the 3D nav tab link
old_tab = '    <a href=\"/\" class=\"nav-tab\">3D \u6a21\u578b</a>\n    <a href=\"/live2d\" class=\"nav-tab active\">2D \u770b\u677f</a>'
new_tab = '    <a href=\"/live2d\" class=\"nav-tab active\">2D \u770b\u677f</a>'
content = content.replace(old_tab, new_tab)
path.write_text(content, 'utf-8')
print('Removed 3D nav tab')
