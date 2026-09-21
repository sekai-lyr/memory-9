import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\resources\templates\live2d.html')
content = path.read_text('utf-8')

# Fix 1: Title - fix garbled text
content = content.replace('<title>Live2D \u9340\u5a98\u256c\u6d98', '<title>Live2D \u770b\u677f')

# Fix 2: Chat window - bigger, WeChat style
content = content.replace(
    '#chat-window{position:fixed;right:16px;top:50px;bottom:70px;width:460px;',
    '#chat-window{position:fixed;right:16px;top:60px;bottom:80px;width:520px;'
)
content = content.replace(
    '#chat-window.hidden{transform:translateX(480px);',
    '#chat-window.hidden{transform:translateX(540px);'
)
content = content.replace(
    '#chat-window-toggle.active{right:484px}',
    '#chat-window-toggle.active{right:544px}'
)

# Fix 3: User message bubble - WeChat green
content = content.replace(
    '.msg.user{align-self:flex-end;background:rgba(100,200,180,0.25);border:1px solid rgba(100,200,180,0.35);color:#e0f0e8;border-bottom-right-radius:4px;font-size:0.95em;max-width:80%}',
    '.msg.user{align-self:flex-end;background:#95ec69;border:none;color:#111;border-bottom-right-radius:4px;font-size:0.95em;max-width:80%}'
)

# Fix 4: AI message - cleaner dark bubble
content = content.replace(
    '.msg.ai{align-self:flex-start;background:rgba(25,20,45,0.85);border:1px solid rgba(255,255,255,0.12);color:#e0d8f0;border-bottom-left-radius:4px',
    '.msg.ai{align-self:flex-start;background:rgba(40,35,60,0.92);border:1px solid rgba(255,255,255,0.08);color:#e8e0f0;border-bottom-left-radius:4px'
)

# Fix 5: Danmaku - bigger, WeChat green for user
content = content.replace(
    '.danmaku-msg{max-width:min(420px,75vw);padding:12px 18px;',
    '.danmaku-msg{max-width:min(520px,80vw);padding:14px 20px;'
)
content = content.replace(
    '.danmaku-msg.user{align-self:flex-end;background:linear-gradient(135deg,#44ccaa,#3ab894);color:#fff;',
    '.danmaku-msg.user{align-self:flex-end;background:#95ec69;color:#111;'
)
content = content.replace(
    '.danmaku-msg.ai{align-self:flex-start;background:rgba(30,25,55,0.92);backdrop-filter:blur(12px);color:#e0d8f0;',
    '.danmaku-msg.ai{align-self:flex-start;background:rgba(35,30,55,0.94);backdrop-filter:blur(12px);color:#e8e2f2;'
)

# Fix 6: Chat area bottom bar - fix button order, move file btn to right place
content = content.replace(
    '#chat-input{flex:1;padding:10px 16px;border-radius:24px;',
    '#chat-input{flex:1;padding:12px 18px;border-radius:24px;'
)
content = content.replace(
    '#chat-send{padding:10px 20px;border-radius:24px;',
    '#chat-send{padding:12px 24px;border-radius:24px;'
)
content = content.replace(
    '#chat-img-btn{padding:10px 14px;border-radius:24px;border:1px solid rgba(100,200,180,0.3);background:rgba(100,200,180,0.12);color:#80d0b8;cursor:pointer;font-size:1.1em;line-height:1}',
    '#chat-img-btn{padding:10px 12px;border-radius:50%;border:1px solid rgba(100,200,180,0.3);background:rgba(100,200,180,0.12);color:#80d0b8;cursor:pointer;font-size:1.1em;line-height:1;flex-shrink:0;width:40px;height:40px;display:flex;align-items:center;justify-content:center}'
)

# Fix 7: Chat window input area - bigger
content = content.replace(
    '#chat-win-input input{flex:1;padding:10px 18px;',
    '#chat-win-input input{flex:1;padding:12px 18px;'
)

# Fix 8: Message font size bigger
content = content.replace(
    '.msg{max-width:85%;padding:12px 16px;border-radius:16px;font-size:0.95em;',
    '.msg{max-width:85%;padding:12px 16px;border-radius:16px;font-size:1em;'
)

# Fix 9: Danmaku font bigger
content = content.replace(
    'border-radius:18px;font-size:0.88em;line-height:1.5;word-break:break-word;animation:danmakuIn',
    'border-radius:18px;font-size:0.92em;line-height:1.6;word-break:break-word;animation:danmakuIn'
)

# Fix 10: File upload in chat window - make button round
content = content.replace(
    '#chat-win-input .img-upload{padding:8px 10px;border-radius:20px;',
    '#chat-win-input .img-upload{padding:6px 8px;border-radius:50%;width:36px;height:36px;display:flex;align-items:center;justify-content:center;'
)

# Fix 11: Fix garbled comment
content = content.replace('/* ===== ???????????? ===== */', '/* ===== Danmaku ===== */')
content = content.replace('/* ???bubble?????? */', '/* Thinking Bubble */')
content = content.replace('/* Chat window panel */', '/* Chat Window Panel */')
content = content.replace('/* Stats panel */', '/* Stats Panel */')

# Fix 12: Update chat messages gap for better spacing
content = content.replace(
    '#chat-messages{flex:1;overflow-y:auto;padding:16px 14px;display:flex;flex-direction:column;gap:12px}',
    '#chat-messages{flex:1;overflow-y:auto;padding:16px 14px;display:flex;flex-direction:column;gap:10px}'
)

# Fix 13: Add WeChat-style timestamp to messages
# Add a new CSS rule for message timestamps
chatmsg_css = '#chat-messages{flex:1;overflow-y:auto;padding:16px 14px;display:flex;flex-direction:column;gap:10px}'
new_chat_css = chatmsg_css + '\n.msg-time{text-align:center;font-size:0.72em;color:rgba(255,255,255,0.35);padding:6px 0;margin:2px 0}'

content = content.replace(chatmsg_css, new_chat_css)

path.write_text(content, 'utf-8')
print('SUCCESS - live2d.html CSS/HTML updated')
