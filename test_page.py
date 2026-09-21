import urllib.request
req = urllib.request.Request('http://localhost:8089/live2d')
resp = urllib.request.urlopen(req, timeout=10)
html = resp.read().decode('utf-8')
print('Status:', resp.status)
print('Content length:', len(html))
checks = ['danmaku-container', 'chat-window', 'chat-area', 'chat-img-btn', 'smartReply']
for c in checks:
    print(f'  {c}: {\"YES\" if c in html else \"MISSING\"}')
