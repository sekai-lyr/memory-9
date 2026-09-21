import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\templates\live2d.html')
content = path.read_text('utf-8')
# Add pixi-live2d-display after cubism4
old_scripts = '<script src=\"/js/pixi.min.js\"></script>\n<script src=\"/js/live2dcubismcore.min.js\"></script>\n<script src=\"/js/cubism4.min.js\"></script>\n<script src=\"/js/live2d-app.js?v=8\"></script>'
new_scripts = '<script src=\"/js/pixi.min.js\"></script>\n<script src=\"/js/live2dcubismcore.min.js\"></script>\n<script src=\"/js/pixi-live2d-display.min.js\"></script>\n<script src=\"/js/cubism4.min.js\"></script>\n<script src=\"/js/live2d-app.js?v=9\"></script>'
content = content.replace(old_scripts, new_scripts)
path.write_text(content, 'utf-8')
print('Added pixi-live2d-display.min.js to HTML')
