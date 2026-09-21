import urllib.request, json, sys

# Test 1: Check 2D model API
req = urllib.request.Request('http://localhost:8089/api/live2d/models')
data = json.loads(urllib.request.urlopen(req, timeout=5).read().decode('utf-8'))
print('Models:', len(data['data']))
for m in data['data']:
    print(f'  {m[\"id\"]}: {m[\"modelName\"]} -> {m[\"modelPath\"]}')

# Test 2: Check model file access
req2 = urllib.request.Request('http://localhost:8089/api/live2d/models/1')
data2 = json.loads(urllib.request.urlopen(req2, timeout=5).read().decode('utf-8'))
print(f'Model 1: {data2}')
