import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Fix the error swallowing - replace catch with logging
old_catch = '} catch (Exception ignored) {}'
new_catch = '} catch (Exception e) { System.err.println(\"[Chat] Tool pre-execution error: \" + e.getMessage()); e.printStackTrace(); }'

content = content.replace(old_catch, new_catch)

# Also fix the other catch blocks in the intent section
if content.count(old_catch) > 0:
    print(f'Found {content.count(old_catch)} more catch blocks to fix')
    content = content.replace(old_catch, new_catch)

path.write_text(content, 'utf-8')
print('Added error logging')
