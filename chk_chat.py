import pathlib
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')
# Find the chat method and function calling loop
idx = content.find('public Result<ChatResponse> chat')
snippet = content[idx:idx+3000]
# Write to file for analysis
pathlib.Path(r'D:\Sekai_two\memory-9\chat_method.txt').write_text(snippet, 'utf-8')
print(f'Chat method: {len(snippet)} chars')
print(snippet[:500])
