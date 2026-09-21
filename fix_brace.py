import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Fix: line 298 has '     } }' but should just have '    }'
# The issue is a duplicated closing brace after the system prompt method
old_bad = ';\n     } }\n\n    private ObjectNode chatMessage'
new_good = ';\n    }\n\n    private ObjectNode chatMessage'

if old_bad in content:
    content = content.replace(old_bad, new_good)
    print('Fixed extra closing brace')
else:
    print('Pattern not found, showing context...')
    idx = content.find('chatMessage')
    if idx >= 0:
        print(repr(content[idx-30:idx+50]))

path.write_text(content, 'utf-8')
print('Done')
