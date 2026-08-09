INSERT INTO sekai_form_user(user_name, password)
SELECT 'sekai', 'NbCIA6n9SCV5t6P8t42QBOCoLaCCaAFcLSgNIJvnR6s='
WHERE NOT EXISTS (SELECT 1 FROM sekai_form_user WHERE user_name = 'sekai');


-- ===== Live2D Models =====
INSERT INTO live2d_model(model_name, model_path, display_name)
SELECT 'aidang_2', 'models\2d\aidang_2', '??? (Cubism 4)'
WHERE NOT EXISTS (SELECT 1 FROM live2d_model WHERE model_name = 'aidang_2');

INSERT INTO live2d_model(model_name, model_path, display_name)
SELECT 'biaoqiang_3', 'models\2d\biaoqiang_3', '?? (Cubism 4)'
WHERE NOT EXISTS (SELECT 1 FROM live2d_model WHERE model_name = 'biaoqiang_3');

INSERT INTO live2d_model(model_name, model_path, display_name)
SELECT 'haru_ja', 'models\2d\haru_ja', 'Haru (Cubism 2)'
WHERE NOT EXISTS (SELECT 1 FROM live2d_model WHERE model_name = 'haru_ja');

INSERT INTO live2d_model(model_name, model_path, display_name)
SELECT 'hiyori_en', 'models\2d\hiyori_en', 'Hiyori (Cubism 2/4)'
WHERE NOT EXISTS (SELECT 1 FROM live2d_model WHERE model_name = 'hiyori_en');
