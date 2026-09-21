import urllib.request, json

# Test models
req = urllib.request.Request('http://localhost:8089/api/live2d/models')
data = json.loads(urllib.request.urlopen(req, timeout=5).read().decode('utf-8'))
print(f"Models loaded: {len(data['data'])}")

# Test chat
body = json.dumps({'message': 'hello', 'history': []}).encode('utf-8')
req = urllib.request.Request('http://localhost:8089/api/live2d/chat/1', data=body, 
    headers={'Content-Type': 'application/json;charset=UTF-8'}, method='POST')
data = json.loads(urllib.request.urlopen(req, timeout=60).read().decode('utf-8'))
reply = data.get('data', {}).get('reply', 'N/A')
print(f"Chat OK - reply {len(reply)} chars")
print(reply[:150])
