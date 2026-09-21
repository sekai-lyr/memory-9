INSERT INTO sekai_form_user(user_name, password)
SELECT 'sekai', 'NbCIA6n9SCV5t6P8t42QBOCoLaCCaAFcLSgNIJvnR6s='
WHERE NOT EXISTS (SELECT 1 FROM sekai_form_user WHERE user_name = 'sekai');


-- ===== Live2D Models =====
DELETE FROM live2d_chat_config
WHERE model_id IN (SELECT id FROM live2d_model WHERE model_name IN ('aidang_2', 'biaoqiang_3'));

DELETE FROM live2d_dialogue
WHERE model_id IN (SELECT id FROM live2d_model WHERE model_name IN ('aidang_2', 'biaoqiang_3'));

DELETE FROM live2d_model
WHERE model_name IN ('aidang_2', 'biaoqiang_3');

INSERT INTO live2d_model(model_name, model_path, display_name)
SELECT 'haru_ja', 'models\2d\haru_ja', 'Haru (Cubism 2)'
WHERE NOT EXISTS (SELECT 1 FROM live2d_model WHERE model_name = 'haru_ja');

INSERT INTO live2d_model(model_name, model_path, display_name)
SELECT 'hiyori_en', 'models\2d\hiyori_en', 'Hiyori (Cubism 2/4)'
WHERE NOT EXISTS (SELECT 1 FROM live2d_model WHERE model_name = 'hiyori_en');

INSERT INTO live2d_model(model_name, model_path, display_name)
SELECT 'hatsune_miku', 'models\2d\hatsune_miku', '初音未来 (DivaStage 自建 2D)'
WHERE NOT EXISTS (SELECT 1 FROM live2d_model WHERE model_name = 'hatsune_miku');
