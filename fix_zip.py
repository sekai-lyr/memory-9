import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\control\SekaiFormModelControl.java')
content = path.read_text('utf-8')

# Add error return for non-zip files
old_endzip = 'deleteDir(tempDir.toFile());\n            }'
new_endzip = 'deleteDir(tempDir.toFile());\n            } else {\n                return Result.fail(\"仅支持zip格式上传\");\n            }'
content = content.replace(old_endzip, new_endzip)

path.write_text(content, 'utf-8')
print('Fixed: non-zip returns error')
