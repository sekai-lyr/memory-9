import urllib.request, json, sys
body = json.dumps({'message': '\u6211\u4eca\u5929\u597d\u7d2f\u554a', 'history': []}).encode('utf-8')
req = urllib.request.Request('http://localhost:8089/api/live2d/chat/1', data=body, 
    headers={'Content-Type': 'application/json;charset=UTF-8'}, method='POST')
data = json.loads(urllib.request.urlopen(req, timeout=60).read().decode('utf-8'))
reply = data.get('data', {}).get('reply', 'N/A')
sys.stdout.buffer.write(('LEN=' + str(len(reply)) + '\n' + reply + '\n').encode('utf-8'))
