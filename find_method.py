import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Find ALL occurrences of getDefaultSystemPrompt
pos = 0
count = 0
while True:
    idx = content.find('getDefaultSystemPrompt', pos)
    if idx == -1:
        break
    count += 1
    print(f'--- Occurrence {count} at pos {idx} ---')
    start = max(0, idx - 30)
    end = min(len(content), idx + 600)
    print(content[start:end])
    print()
    pos = idx + 10
