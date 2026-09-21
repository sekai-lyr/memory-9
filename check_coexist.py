import urllib.request
req = urllib.request.Request('http://localhost:8089/live2d')
html = urllib.request.urlopen(req, timeout=10).read().decode('utf-8')
# Check for key components
checks = {
    'live2d-canvas': '2D model canvas',
    'danmaku-container': 'Danmaku bubble area',
    'chat-window': 'Chat window panel',
    'chat-area': 'Bottom chat input bar',
    'model-select': 'Model selector',
}
for key, desc in checks.items():
    print(f'{desc}: {\"YES\" if key in html else \"MISSING\"}'  )
print(f'Page size: {len(html)} chars')
