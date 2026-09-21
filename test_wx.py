import urllib.request, json
body = json.dumps({'message': '\u676d\u5dde\u5929\u6c14\u600e\u4e48\u6837', 'history': []}).encode('utf-8')
req = urllib.request.Request('http://localhost:8089/api/live2d/chat/1', data=body, 
    headers={'Content-Type': 'application/json;charset=UTF-8'}, method='POST')
data = json.loads(urllib.request.urlopen(req, timeout=60).read().decode('utf-8'))
reply = data.get('data', {}).get('reply', 'N/A')
import sys
sys.stdout.buffer.write(('Reply (' + str(len(reply)) + ' chars):\n' + reply).encode('utf-8'))
