import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\data.sql')
content = path.read_text('utf-8')

# Add model inserts
new_inserts = '''

-- ===== Live2D Models =====
INSERT INTO live2d_model(model_name, model_path, display_name)
SELECT 'haru_ja', 'models\\2d\\haru_ja', 'Haru (Cubism 2)'
WHERE NOT EXISTS (SELECT 1 FROM live2d_model WHERE model_name = 'haru_ja');

INSERT INTO live2d_model(model_name, model_path, display_name)
SELECT 'hiyori_en', 'models\\2d\\hiyori_en', 'Hiyori (Cubism 2/4)'
WHERE NOT EXISTS (SELECT 1 FROM live2d_model WHERE model_name = 'hiyori_en');

INSERT INTO live2d_model(model_name, model_path, display_name)
SELECT 'hatsune_miku', 'models\\2d\\hatsune_miku', '初音未来 (DivaStage 自建 2D)'
WHERE NOT EXISTS (SELECT 1 FROM live2d_model WHERE model_name = 'hatsune_miku');
'''

if 'live2d_model' not in content:
    content += new_inserts
    path.write_text(content, 'utf-8')
    print('Added model inserts to data.sql')
else:
    print('Models already in data.sql?')
    print(content[content.find('live2d_model'):][:200] if 'live2d_model' in content else 'not found')
