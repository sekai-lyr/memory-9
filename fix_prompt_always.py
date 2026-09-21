import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Fix: Always use the getDefaultSystemPrompt() and append DB prompt as additional context
old_check = '''            String sysPrompt = config.getSystemPrompt();
            if (sysPrompt == null || sysPrompt.length() < 50 || isGarbled(sysPrompt)) {
                sysPrompt = getDefaultSystemPrompt();
            }'''

new_check = '''            // Always use the comprehensive Haru system prompt
            String sysPrompt = getDefaultSystemPrompt();
            // Append any custom DB prompt as additional context
            String dbPrompt = config.getSystemPrompt();
            if (dbPrompt != null && dbPrompt.length() > 20 && !isGarbled(dbPrompt) && !dbPrompt.equals(sysPrompt)) {
                sysPrompt = sysPrompt + \"\\n\\n[Additional context: \" + dbPrompt + \"]\";
            }'''

if old_check in content:
    content = content.replace(old_check, new_check)
    path.write_text(content, 'utf-8')
    print('FIXED: Always use full system prompt from Java code')
else:
    print('Pattern not found, searching...')
    idx = content.find('sysPrompt = config.getSystemPrompt()')
    if idx >= 0:
        print(content[idx-10:idx+300])
