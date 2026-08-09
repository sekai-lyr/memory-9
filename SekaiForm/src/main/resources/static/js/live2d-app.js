// Live2D Kanban - Interactive System
var app, currentModel, bubbleTimer, idleTimer, lastInteraction = Date.now();
var models2D = [], currentModelName = "", charId = 1;
var isDragging = false, dragStart = { x:0, y:0 }, modelStart = { x:0, y:0 }, tapCooldown = false;
var affection = 0;
var mousePos = { x: window.innerWidth / 2, y: window.innerHeight / 2 };
var followActive = true, lastExpression = "";
var isAiThinking = false;

function getTimeGreeting() {
    var h = new Date().getHours();
    if (h >= 5 && h < 9) return ["早上好呀~今天也要加油哦！", "早安！新的一天开始啦~", "早上好主人，睡得好吗？"][Math.floor(Math.random() * 3)];
    if (h >= 9 && h < 12) return ["上午好~有什么需要帮忙的吗？", "今天天气真不错呢~", "精神满满的一天开始啦！"][Math.floor(Math.random() * 3)];
    if (h >= 12 && h < 14) return ["中午好~该吃午饭啦！", "肚子饿了吗？一起去吃饭吧~", "午休时间到~"][Math.floor(Math.random() * 3)];
    if (h >= 14 && h < 18) return ["下午好~要加油工作哦！", "下午茶时间到了呢~", "阳光正好的下午~"][Math.floor(Math.random() * 3)];
    if (h >= 18 && h < 21) return ["晚上好~辛苦了一天呢！", "晚饭吃了什么呀？", "夜幕降临，放松一下吧~"][Math.floor(Math.random() * 3)];
    if (h >= 21 && h < 24) return ["夜深了，要早点休息哦~", "这么晚还不睡吗？", "熬夜对身体不好哦~"][Math.floor(Math.random() * 3)];
    return ["你好呀~很高兴见到你！", "欢迎回来~", "嘿！好久不见~"][Math.floor(Math.random() * 3)];
}

// --- Character Stats ---
var charStats = { level:1, exp:0, expToNext:100, hp:100, atk:20, def:10, freePoints:0 };
var _statsListeners = [];
function onStatsChange(fn) { _statsListeners.push(fn); }
function emitStatsChange() { _statsListeners.forEach(function(f) { try { f(charStats); } catch(e) {} }); }

function updateStatsUI() {
    document.getElementById("s-level").textContent = charStats.level;
    document.getElementById("s-hp").textContent = charStats.hp;
    document.getElementById("s-atk").textContent = charStats.atk;
    document.getElementById("s-def").textContent = charStats.def;
    document.getElementById("free-pts").textContent = charStats.freePoints;
    document.getElementById("aff-val").textContent = affection;
    var pct = charStats.expToNext > 0 ? (charStats.exp / charStats.expToNext * 100) : 100;
    document.getElementById("exp-bar-fill").style.width = pct + "%";
    document.getElementById("exp-text").textContent = "EXP " + charStats.exp + " / " + charStats.expToNext;
    ["btn-hp","btn-atk","btn-def"].forEach(function(id) {
        document.getElementById(id).disabled = charStats.freePoints <= 0;
    });
}
onStatsChange(updateStatsUI);

// --- Toast ---
function toast(msg) {
    var t = document.createElement("div"); t.className = "toast-msg"; t.textContent = msg;
    document.getElementById("toast-area").appendChild(t);
    setTimeout(function() { t.remove(); }, 2200);
}
function log(msg) { document.getElementById("status-text").textContent = msg; }

// ===== Danmaku System =====
var danmakuQueue = [];
var danmakuMaxVisible = 6;

function addDanmaku(role, text) {
    var container = document.getElementById("danmaku-container");
    var el = document.createElement("div");
    el.className = "danmaku-msg " + role;
    el.textContent = text;
    container.appendChild(el);
    
    // Limit visible danmaku
    danmakuQueue.push(el);
    while (danmakuQueue.length > danmakuMaxVisible) {
        var old = danmakuQueue.shift();
        old.classList.add("fading");
        setTimeout(function() { if (old.parentNode) old.remove(); }, 500);
    }
    
    // Auto-fade after delay (AI messages stay longer)
    var delay = role === "ai" ? 8000 : 5000;
    setTimeout(function() {
        if (el.parentNode) {
            el.classList.add("fading");
            setTimeout(function() { if (el.parentNode) el.remove(); }, 500);
            // Remove from queue
            var idx = danmakuQueue.indexOf(el);
            if (idx >= 0) danmakuQueue.splice(idx, 1);
        }
    }, delay);
    
    return el;
}

// --- Greetings list ---
var greetings = [
    "你好呀！今天心情怎么样？",
    "主人你来啦~想我了吗？",
    "好无聊啊，陪我玩一会儿吧！",
    "看板娘Haru为您服务~请多指教！",
    "有什么有趣的事情发生吗？",
    "嘿！等你半天了~",
    "今天也要元气满满哦！",
    "喵~主人回来啦！",
    "终于有人陪我聊天了！",
    "欢迎回来！要来杯茶吗？"
];

// --- Dialog panel ---
function showDialog(x, y) {
    hideDialog();
    var d = document.createElement("div");
    d.id = "dialog-panel";
    d.innerHTML = '<div class="dlg-item" onclick="doTrain()">⚔️ 训练</div>';
    d.style.left = Math.min(x, window.innerWidth - 160) + "px";
    d.style.top = Math.min(y, window.innerHeight - 160) + "px";
    document.body.appendChild(d);
    setTimeout(function() {
        document.addEventListener("click", closeDialogOutside);
    }, 50);
}
function hideDialog() {
    var d = document.getElementById("dialog-panel");
    if (d) d.remove();
    document.removeEventListener("click", closeDialogOutside);
}
function closeDialogOutside(e) {
    var d = document.getElementById("dialog-panel");
    if (d && !d.contains(e.target)) hideDialog();
}

// --- Interactions ---
function doTrain() {
    hideDialog();
    gainExp(10);
    affection += 2;
    saveAffection();
    if (!tapCooldown) { tapCooldown = true; setTimeout(function() { tapCooldown = false; }, 800); try { currentModel.motion("Tap"); } catch(e) {} }
    showBubble("训练辛苦了！EXP +10 ♥+2 ✨");
    showFloatText("+10 EXP +2 ♥", "#ffcc44");
    lastInteraction = Date.now();
}

function showFloatText(txt, color) {
    var el = document.createElement("div");
    el.className = "float-text";
    el.textContent = txt;
    el.style.color = color || "#ffcc44";
    el.style.left = (window.innerWidth / 2 + 40) + "px";
    el.style.top = (window.innerHeight * 0.35) + "px";
    document.body.appendChild(el);
    setTimeout(function() { el.remove(); }, 1500);
}

// --- Keyboard shortcuts ---
document.addEventListener("keydown", function(e) {
    if (e.target.tagName === "INPUT" || isAiThinking) return;
    switch(e.code) {
        case "Space":
            e.preventDefault();
            if (tapCooldown || !currentModel) break;
            tapCooldown = true;
            setTimeout(function() { tapCooldown = false; }, 800);
            try { currentModel.motion("Tap"); } catch(_) {}
            showBubble("啊！吓我一跳~");
            break;
        case "KeyE":
            var g = greetings[Math.floor(Math.random() * greetings.length)];
            showBubble(g);
            if (currentModel) try { currentModel.motion("Idle"); } catch(_) {}
            break;
        case "Digit1":
            if (currentModel) try { currentModel.expression("Smile"); } catch(_) {}
            toast("表情: 微笑");
            break;
        case "Digit2":
            if (currentModel) try { currentModel.expression("Sad"); } catch(_) {}
            toast("表情: 悲伤");
            break;
        case "Digit3":
            if (currentModel) try { currentModel.expression("Angry"); } catch(_) {}
            toast("表情: 生气");
            break;
    }
});

// --- Character Stats API ---
async function loadCharStats() {
    try {
        var r = await fetch("/api/char-stats/" + charId);
        var j = await r.json();
        if (j.success && j.data) {
            Object.assign(charStats, j.data);
            if (j.data.affection !== undefined) affection = j.data.affection;
            charStats.expToNext = charStats.level * 100;
            emitStatsChange();
        }
    } catch(e) {}
}

async function saveAffection() {
    try {
        await fetch("/api/char-stats/" + charId + "/affection", {
            method: "POST", headers: {"Content-Type":"application/json"},
            body: JSON.stringify({affection: affection})
        });
    } catch(e) {}
}

async function gainExp(amount) {
    try {
        var r = await fetch("/api/char-stats/" + charId + "/exp", {
            method: "POST", headers: {"Content-Type":"application/json"},
            body: JSON.stringify({amount: amount})
        });
        var j = await r.json();
        if (j.success && j.data) {
            var wasLevel = charStats.level;
            Object.assign(charStats, j.data);
            charStats.expToNext = charStats.level * 100;
            emitStatsChange();
            if (j.data.levelUp) flashLevelUp(j.data.levelsGained || 1);
        }
    } catch(e) {}
}

async function allocPoint(stat) {
    if (charStats.freePoints <= 0) { toast("没有可用属性点了"); return; }
    try {
        var r = await fetch("/api/char-stats/" + charId + "/allocate", {
            method: "POST", headers: {"Content-Type":"application/json"},
            body: JSON.stringify({stat: stat})
        });
        var j = await r.json();
        if (j.success && j.data && j.data.success !== false) {
            Object.assign(charStats, j.data);
            charStats.expToNext = charStats.level * 100;
            emitStatsChange();
            toast(stat + " +1!");
        }
    } catch(e) {}
}

async function resetCharacter() {
    if (!confirm("确定要重置角色到Lv.1吗？所有属性点和经验将被清空。")) return;
    try {
        var r = await fetch("/api/char-stats/" + charId + "/reset", {method:"POST"});
        var j = await r.json();
        if (j.success && j.data) {
            Object.assign(charStats, j.data);
            charStats.expToNext = charStats.level * 100;
            affection = 0;
            emitStatsChange();
            toast("角色已重置！");
        }
    } catch(e) {}
}

function toggleStatsPanel() {
    var p = document.getElementById("stats-panel");
    var b = document.getElementById("stats-toggle");
    p.classList.toggle("collapsed");
    b.textContent = p.classList.contains("collapsed") ? "☰" : "◁";
}

function flashLevelUp(times) {
    var el = document.getElementById("level-up-flash");
    el.textContent = times > 1 ? "LEVEL UP x" + times + "!" : "LEVEL UP!";
    el.classList.remove("show"); void el.offsetWidth; el.classList.add("show");
}

// --- Bubble ---
var cannedReplies = [
    "嗯嗯~我在听呢！",
    "原来如此~",
    "好有意思呀！",
    "然后呢然后呢？",
    "真的吗？好厉害！",
    "嘿嘿~我也这么觉得！",
    "说的对呢~",
    "唔...让我想想",
    "是是是~主人说的都对！",
    "好呀好呀~",
    "诶？是这样吗？",
    "哇~好棒！",
    "有点不太懂呢...",
    "今天超开心的！",
    "主人最好了~"
];

function updateBubblePosition() {
    if (!currentModel) return;
    var b = document.getElementById("bubble");
    var headY = null;

    // Strategy 1: Find head position from Cubism model face parts
    try {
        var internal = currentModel.internalModel;
        if (internal && internal._model) {
            var core = internal._model;
            var partIds = core._partIds || [];
            var faceIdx = {};
            var kw = ['FACE', 'HEAD', 'EYE', 'MOUTH', 'NOSE', 'BROW', 'EAR', 'HAIR_FRONT', 'HAIR_SIDE', 'HOHO'];
            for (var i = 0; i < partIds.length; i++) {
                var pid = (typeof partIds[i] === 'string' ? partIds[i] : (partIds[i] != null ? String(partIds[i]) : '')).toUpperCase();
                for (var k = 0; k < kw.length; k++) { if (pid.indexOf(kw[k]) >= 0) { faceIdx[i] = 1; break; } }
            }
            // Also try matching drawable IDs directly (some SDKs expose this at drawable level)
            var dc = typeof core.getDrawableCount === 'function' ? core.getDrawableCount() : 0;
            var totalMin = Infinity, totalMax = -Infinity;
            var faceMax = -Infinity, faceFound = false;
            for (var d = 0; d < dc; d++) {
                var verts = typeof core.getDrawableVertexPositions === 'function' ? core.getDrawableVertexPositions(d) : null;
                if (!verts) continue;
                var pi = typeof core.getDrawablePartIndex === 'function' ? core.getDrawablePartIndex(d) : -1;
                var isFace = faceIdx[pi] ? 1 : 0;
                // Also check drawable ID directly
                if (!isFace) {
                    try {
                        var did = typeof core.getDrawableId === 'function' ? core.getDrawableId(d) : null;
                        if (did) {
                            var ds = (typeof did === 'string' ? did : String(did)).toUpperCase();
                            for (var k2 = 0; k2 < kw.length; k2++) { if (ds.indexOf(kw[k2]) >= 0) { isFace = 1; break; } }
                        }
                    } catch(e2) {}
                }
                for (var j = 1; j < verts.length; j += 2) {
                    var vy = verts[j];
                    if (vy < totalMin) totalMin = vy;
                    if (vy > totalMax) totalMax = vy;
                    if (isFace && vy > faceMax) { faceMax = vy; faceFound = true; }
                }
            }
            if (faceFound && totalMax > totalMin) {
                var mh = currentModel.height;
                var r = (totalMax - faceMax) / (totalMax - totalMin);
                headY = currentModel.y - mh / 2 + r * mh;
            }
        }
    } catch(e) {}

    // Strategy 2: Use currentModel.height with per-model head ratio
    if (headY === null) {
        var mh = currentModel.height;
        if (mh && mh > 0) {
            var ratios = { "haru_ja": 0.35, "hiyori_en": 0.35, "aidang_2": 0.25, "biaoqiang_3": 0.25 };
            headY = currentModel.y - mh * (ratios[currentModelName] || 0.3);
        }
    }

    // Strategy 3: Estimate height from canvas info and scale, then use ratio
    if (headY === null) {
        var knownH = currentModel._knownCanvasH;
        if (knownH && knownH > 0) {
            var estH = knownH * (currentModel.scale ? currentModel.scale.y : (currentModel.scale || 0.15));
            var ratios = { "haru_ja": 0.35, "hiyori_en": 0.35, "aidang_2": 0.25, "biaoqiang_3": 0.25 };
            if (estH > 0) headY = currentModel.y - estH * (ratios[currentModelName] || 0.3);
        }
    }

    // Strategy 4: Hardcoded pixel offsets
    if (headY === null) {
        var off = { "haru_ja": 160, "hiyori_en": 160, "aidang_2": 120, "biaoqiang_3": 120 };
        headY = currentModel.y - (off[currentModelName] || 160);
    }

    b.style.left = currentModel.x + "px";
    b.style.top = (headY - 15) + "px";
}

function showBubble(t) {
    if (bubbleTimer) clearTimeout(bubbleTimer);
    var b = document.getElementById("bubble");
    b.textContent = t; b.classList.add("show");
    updateBubblePosition();
    bubbleTimer = setTimeout(function() { b.classList.remove("show"); }, 3000);
}

// --- Idle ---
function startIdle() {
    if (idleTimer) clearTimeout(idleTimer);
    idleTimer = setTimeout(function() {
        if (!currentModel) { startIdle(); return; }
        if (isAiThinking) { startIdle(); return; }
        if (Date.now() - lastInteraction < 15000) { startIdle(); return; }

        var r = Math.random();

        if (r < 0.1) {
            try {
                var exprs = ["Smile", "Angry", "Sad", "Surprised", "Normal"];
                var pick = exprs[Math.floor(Math.random() * exprs.length)];
                if (pick !== lastExpression) {
                    currentModel.expression(pick);
                    lastExpression = pick;
                    showBubble(["嘿嘿~", "唔...", "嗯？", "...唔"][Math.floor(Math.random() * 4)]);
                }
            } catch(_) {}
        } else if (r < 0.2) {
            showBubble(getTimeGreeting());
            try { currentModel.motion("Idle"); } catch(_) {}
        } else if (r < 0.3) {
            if (followActive) {
                var dx = mousePos.x - currentModel.x;
                var dy = (mousePos.y - 80) - currentModel.y;
                var dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 200) {
                    showBubble(cannedReplies[Math.floor(Math.random() * cannedReplies.length)]);
                }
            } else {
                showBubble(cannedReplies[Math.floor(Math.random() * cannedReplies.length)]);
            }
        } else {
            showBubble(cannedReplies[Math.floor(Math.random() * cannedReplies.length)]);
        }

        if (lastExpression && lastExpression !== "Normal" && Math.random() < 0.3) {
            try { currentModel.expression("Normal"); lastExpression = ""; } catch(_) {}
        }

        startIdle();
    }, 15000 + Math.random() * 25000);
}

// --- Proximity / hover awareness ---
var proximityCheck = null;
function startProximityCheck() {
    if (proximityCheck) clearInterval(proximityCheck);
    proximityCheck = setInterval(function() {
        if (!currentModel || isDragging || isAiThinking) return;
        var dx = mousePos.x - currentModel.x;
        var dy = (mousePos.y - 80) - currentModel.y;
        var dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 120 && Date.now() - lastInteraction > 5000) {
            lastInteraction = Date.now();
            showBubble(["嘿嘿，你在看我吗？", "嗯？", "想摸摸我吗？", "一直盯着我看呢~"][Math.floor(Math.random() * 4)]);
            try { currentModel.motion("Tap"); } catch(_) {}
            try {
                if (currentModel.expression && lastExpression !== "Smile") {
                    currentModel.expression("Smile");
                    lastExpression = "Smile";
                    setTimeout(function() {
                        try { currentModel.expression("Normal"); lastExpression = ""; } catch(_) {}
                    }, 2000);
                }
            } catch(_) {}
        }
    }, 3000);
}

// --- Init ---
async function init() {
    log("Starting...");
    var canvas = document.getElementById("live2d-canvas");

    app = new PIXI.Application({
        view: canvas, resizeTo: window, backgroundAlpha: 0,
        antialias: true, resolution: window.devicePixelRatio || 1
    });

    log("Starting Cubism4...");
    try {
        PIXI.live2d.startUpCubism4();
        await PIXI.live2d.cubism4Ready;
        log("Cubism4 ready");
    } catch(e) { log("Cubism4 fail: " + e.message); return; }
    await loadCharStats();
    await loadModelList();

    window.addEventListener("resize", function() {
        if (currentModel) {
            currentModel.x = window.innerWidth / 2;
            currentModel.y = (window.innerHeight - 80) / 2;
            updateBubblePosition();
        }
    });

    document.addEventListener("mousemove", function(e) {
        mousePos.x = e.clientX;
        mousePos.y = e.clientY;
        if (currentModel && followActive && !isDragging) {
            try {
                var nx = e.clientX / window.innerWidth;
                var ny = e.clientY / window.innerHeight;
                if (currentModel.focus) {
                    currentModel.focus.set(nx, ny);
                }
            } catch(_) {}
        }
    });
}

// --- Model loading ---
async function loadModelList() {
    try {
        var r = await fetch("/api/model/2d/list");
        var j = await r.json();
        var sel = document.getElementById("model-select");
        sel.innerHTML = "";
        if (j.success && j.data && j.data.length) {
            models2D = j.data;
            models2D.forEach(function(n) { sel.innerHTML += "<option value=\""+n+"\">"+n+"</option>"; });
            sel.value = models2D[0];
            await switchModel(models2D[0]);
        } else { sel.innerHTML = "<option value=\"\">No models</option>"; log("No models"); }
    } catch(e) { log("Error: "+e.message); }
}

async function reloadModels(selectName) {
    try {
        var r = await fetch("/api/model/2d/list");
        var j = await r.json();
        var sel = document.getElementById("model-select");
        sel.innerHTML = "";
        if (j.success && j.data && j.data.length) {
            models2D = j.data;
            models2D.forEach(function(n) { sel.innerHTML += "<option value=\""+n+"\">"+n+"</option>"; });
            var target = selectName && models2D.indexOf(selectName) >= 0 ? selectName : models2D[0];
            sel.value = target;
            await switchModel(target);
        } else { sel.innerHTML = "<option value=\"\">No models</option>"; log("No models"); }
    } catch(e) { log("Error: "+e.message); }
}

async function switchModel(name) {
    if (!name) return;
    log("Loading "+name+"...");
    if (currentModel) { try { app.stage.removeChild(currentModel); currentModel.destroy(); } catch(e) {} currentModel = null; }
    currentModelName = name;

    var url = "/live2d-models/" + name + "/" + name + ".model3.json";

    try {
        currentModel = await PIXI.live2d.Live2DModel.from(url, { autoInteract: true });
        var modelScales = {
            "haru_ja": 0.15,
            "hiyori_en": 0.15,
            "aidang_2": 0.10,
            "biaoqiang_3": 0.10
        };
        currentModel.scale.set(modelScales[name] || 0.15);
        currentModel.anchor.set(0.5, 0.5);
        currentModel.x = window.innerWidth / 2;
        currentModel.y = (window.innerHeight - 80) / 2;
        currentModel.interactive = true;
        currentModel.cursor = "pointer";

        currentModel.on("pointerdown", function(e) {
            if (isDragging || isAiThinking) return;
            if (tapCooldown) return;
            tapCooldown = true;
            setTimeout(function() { tapCooldown = false; }, 800);
            lastInteraction = Date.now();
            try { currentModel.motion("Tap"); } catch(_) {}
            showBubble(cannedReplies[Math.floor(Math.random() * cannedReplies.length)]);
            showDialog(e.data.global.x, e.data.global.y - 120);
        });

        currentModel.on("pointerdown", function(e) { onDragStart(e); });
        currentModel.on("pointermove", function(e) { onDragMove(e); });
        currentModel.on("pointerup", function(e) { onDragEnd(e); });
        currentModel.on("pointerupoutside", function(e) { onDragEnd(e); });

        app.stage.addChild(currentModel);
        log(name + " ready!");

        try {
            var canvasInfo = currentModel.internalModel && currentModel.internalModel._canvasInfo;
            if (canvasInfo) {
                currentModel._knownCanvasH = canvasInfo.height;
            } else {
                var intModel = currentModel.internalModel;
                if (intModel && intModel._model && intModel._model._canvasInfo) {
                    currentModel._knownCanvasH = intModel._model._canvasInfo.height;
                }
            }
        } catch(e) {}
        updateBubblePosition();
        // Wait for first render so bounds become available
        setTimeout(function() { updateBubblePosition(); }, 50);

        try { currentModel.motion("Idle"); } catch(e) {}
        startIdle();
        startProximityCheck();

        try {
            if (currentModel.expression) {
                currentModel.expression("Normal");
                lastExpression = "";
            }
        } catch(_) {}
    } catch(e) { log("Fail: " + e.message); }
}

// --- Drag ---
function onDragStart(e) {
    isDragging = true;
    dragStart = { x: e.data.global.x, y: e.data.global.y };
    modelStart = { x: currentModel.x, y: currentModel.y };
    currentModel.alpha = 0.8;
}
function onDragMove(e) {
    if (!isDragging) return;
    var dx = e.data.global.x - dragStart.x;
    var dy = e.data.global.y - dragStart.y;
    currentModel.x = modelStart.x + dx;
    currentModel.y = modelStart.y + dy;
    updateBubblePosition();
}
function onDragEnd(e) {
    if (!isDragging) return;
    isDragging = false;
    currentModel.alpha = 1;
}

// --- Upload / Delete ---
function uploadWithRetry(url, formData, retriesLeft) {
    var ctrl = new AbortController();
    var timer = setTimeout(function() { ctrl.abort(); }, 600000);
    return fetch(url, { method: "POST", body: formData, signal: ctrl.signal }).then(function(r) {
        clearTimeout(timer);
        if (!r.ok) throw new Error("Server " + r.status);
        return r.json();
    }).catch(function(e) {
        clearTimeout(timer);
        if (retriesLeft > 0 && (e.name === "AbortError" || e.message.indexOf("fetch") !== -1)) {
            console.warn("Upload retry, left: " + retriesLeft);
            return uploadWithRetry(url, formData, retriesLeft - 1);
        }
        throw e;
    });
}

function onUpload2D(input) {
    var f = input.files[0]; if (!f) return;
    var fd = new FormData(); fd.append("file", f); fd.append("type", "2D");
    log("Uploading " + f.name + "...");
    toast("Uploading: " + f.name + " (" + (f.size / 1024 / 1024).toFixed(1) + " MB)...");
    uploadWithRetry("/api/model/upload", fd, 2).then(function(j) {
        if (j.success) {
            var name = j.data.fileName;
            toast("Uploaded: " + name);
            log("Upload OK: " + name);
            return reloadModels(name);
        } else {
            var msg = j.message || "Unknown error";
            log("Upload failed: " + msg);
            toast("Upload failed: " + msg);
        }
    }).catch(function(e) {
        if (e.name === "AbortError") {
            log("Upload timeout - file too large?");
            toast("Upload timeout - file too large?");
        } else {
            log("Upload error: " + (e.message || "Network failure"));
            toast("Upload error: " + (e.message || "Network failure"));
        }
    });
    input.value = "";
}
function delete2DModel() {
    var s = document.getElementById("model-select");
    if (!s.value) return;
    if (!confirm("确定要删除这个模型吗？")) return;
    fetch("/api/model/delete?fileName=" + encodeURIComponent(s.value) + "&type=2D", { method: "DELETE" })
        .then(function(r) { return r.json(); })
        .then(function(j) { if (j.success) { toast("删除成功！"); loadModelList(); } });
}

function getModelName() {
    var nameMap = {
        "haru_ja": "Haru",
        "hiyori_en": "Hiyori",
        "aidang_2": "小爱",
        "biaoqiang_3": "小标"
    };
    return nameMap[currentModelName] || currentModelName || "看板娘";
}

// --- Chat Window ---
var chatWindowOpen = false;
var chatHistory = [];

function toggleChatWindow() {
    var win = document.getElementById("chat-window");
    var btn = document.getElementById("chat-window-toggle");
    chatWindowOpen = !chatWindowOpen;
    if (chatWindowOpen) {
        win.classList.remove("hidden");
        btn.classList.add("active");
        btn.textContent = "✕";
        scrollChatToBottom();
    } else {
        win.classList.add("hidden");
        btn.classList.remove("active");
        btn.textContent = "💬";
    }
}

function scrollChatToBottom() {
    var el = document.getElementById("chat-messages");
    setTimeout(function() { el.scrollTop = el.scrollHeight; }, 50);
}

function addChatMessage(role, text, extra) {
    var el = document.createElement("div");
    el.className = "msg " + role;
    if (text) el.textContent = text;
    if (extra) {
        if (typeof extra === "string") {
            var img = document.createElement("img");
            img.src = extra;
            img.onload = function() { scrollChatToBottom(); };
            img.onerror = function() { img.remove(); };
            el.appendChild(document.createElement("br"));
            el.appendChild(img);
        } else {
            el.appendChild(extra);
        }
    }
    document.getElementById("chat-messages").appendChild(el);
    scrollChatToBottom();
    return el;
}

function addThinkingMessage() {
    return addChatMessage("ai", "思考中...");
}

function removeElement(el) {
    if (el && el.parentNode) el.parentNode.removeChild(el);
}

function clearChatHistory() {
    document.getElementById("chat-messages").innerHTML = "";
    chatHistory = [];
}

// --- Image Upload for Chat ---
function onChatImageUpload(input) {
    var f = input.files[0];
    if (!f) return;
    input.value = "";
    handleChatImage(f);
}

async function handleChatImage(file) {
    var reader = new FileReader();
    reader.onload = async function(e) {
        // 显示用户发送的图片
        var userImg = document.createElement("img");
        userImg.src = e.target.result;
        userImg.className = "msg-user-img";
        addChatMessage("user", "", userImg);

        // 在bubble上也显示
        showBubble("正在识别图片...");

        var thinkEl = addThinkingMessage();

        try {
            var formData = new FormData();
            formData.append("image", file);
            formData.append("question", "");

            var resp = await fetch("/api/live2d/chat/1/recognize", {
                method: "POST",
                body: formData
            });
            var json = await resp.json();
            removeElement(thinkEl);

            if (json.success && json.data && json.data.reply) {
                addChatMessage("ai", json.data.reply);
                showBubble(json.data.reply);
                chatHistory.push({role: "user", content: "[发送了一张图片]"});
                chatHistory.push({role: "assistant", content: json.data.reply});
            } else {
                var errMsg = json.message || "识别失败";
                addChatMessage("ai", "图片识别失败: " + errMsg);
                showBubble("图片识别失败: " + errMsg);
            }
        } catch(err) {
            removeElement(thinkEl);
            addChatMessage("ai", "图片识别出错: " + err.message);
            showBubble("图片识别出错");
        }

        if (currentModel) try { currentModel.motion("Tap"); } catch(e) {}
        lastInteraction = Date.now();
        startIdle();
    };
    reader.readAsDataURL(file);
}

// --- Image Generation ---
async function handleImageGeneration(prompt) {
    showBubble("正在生成图片，请稍候...");
    var thinkEl = addChatMessage("ai", "正在生成图片: " + prompt + "...");

    try {
        var resp = await fetch("/api/live2d/chat/1/generate", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({prompt: prompt})
        });
        var json = await resp.json();
        removeElement(thinkEl);

        if (json.success && json.data && json.data.imageUrl) {
            var genImg = document.createElement("img");
            genImg.src = json.data.imageUrl;
            genImg.className = "msg-gen-img";
            genImg.onclick = function() { window.open(this.src, "_blank"); };
            addChatMessage("ai", json.data.reply || "图片已生成~", genImg);
            showBubble("图片已生成~");
            chatHistory.push({role: "user", content: "画图: " + prompt});
            chatHistory.push({role: "assistant", content: "[生成了一张图片]"});
        } else {
            var errMsg = json.message || "生成失败";
            addChatMessage("ai", "图片生成失败: " + errMsg);
            showBubble("图片生成失败: " + errMsg);
        }
    } catch(err) {
        removeElement(thinkEl);
        addChatMessage("ai", "图片生成出错: " + err.message);
        showBubble("图片生成出错");
    }

    if (currentModel) try { currentModel.motion("Tap"); } catch(e) {}
    lastInteraction = Date.now();
    startIdle();
}

function isImageGenCommand(text) {
    return text.startsWith("画图") || text.startsWith("生图") || text.startsWith("生成图片");
}

function extractImagePrompt(text) {
    return text.replace(/^(画图|生图|生成图片)[:：,，\s]*/, "").trim();
}

// --- Chat Window Send ---
async function sendFromChatWindow() {
    var input = document.getElementById("chat-win-input-text");
    var m = input.value.trim();
    if (!m) return;
    input.value = "";

    addDanmaku("user", m);
    addChatMessage("user", m);

    if (isImageGenCommand(m)) {
        var prompt = extractImagePrompt(m);
        if (!prompt) {
            addChatMessage("ai", "请输入图片描述，例如：画图 一只可爱的猫咪");
            return;
        }
        await handleImageGeneration(prompt);
    } else {
        var reply = await doAiChat(m);
        if (reply) {
            addDanmaku("ai", reply);
            addChatMessage("ai", reply);
        }
    }
}

async function doAiChat(m) {
    var thinkEl = addThinkingMessage();
    var reply = null;

    try {
        var resp = await fetch("/api/live2d/chat/1", {
            method: "POST", headers: {"Content-Type":"application/json"},
            body: JSON.stringify({message: m, history: chatHistory.slice(-20)})
        });
        var json = await resp.json();
        if (json.success && json.data && json.data.reply &&
            json.data.reply.indexOf("not configured") === -1 &&
            json.data.reply.indexOf("Chat not") === -1) {
            reply = json.data.reply;
            if (json.data.motion && currentModel) try { currentModel.motion(json.data.motion); } catch(e) {}
        }
    } catch(e) {}

    removeElement(thinkEl);

    if (!reply) reply = smartReply(m);

    chatHistory.push({role: "user", content: m});
    chatHistory.push({role: "assistant", content: reply});
    
    return reply;
}


function smartReply(msg) {
    var m = msg.toLowerCase();
    var name = getModelName();
    var pick = function(arr) { return arr[Math.floor(Math.random() * arr.length)]; };
    var h = new Date().getHours();

    // ===== Haru Smart Reply v7.24-pm =====
    
    // --- Greetings ---
    if (m.indexOf("你好")>=0 || m.indexOf("hello")>=0 || m.indexOf("hi")>=0 || m.indexOf("喂")>=0)
        return pick([
            "你好呀～！今天天气真棒呢，有什么我能帮你的吗？不管是聊天、查天气、讲笑话，我都在这里陪着你哦～",
            "嘿嘿～等你好久啦！最近过得怎么样呀？有没有什么开心的事想和我分享的？我可是你的忠实听众哦～",
            "哈囉～Haru在此！今天有什么想聊的话题吗？我可以陪你聊天、讲故事、说笑话，随你挑哦～"
        ]);
    
    // --- Who are you / Identity ---
    if (m.indexOf("你是谁")>=0 || m.indexOf("介绍")>=0 || m.indexOf("身份")>=0)
        return pick([
            "我是" + name + "～是你的Live2D看板娘也是你的贴心小伙伴！我可以陪你聊天、查天气、讲笑话，甚至还能记住你说过的话哦！虽然我只是一个小小的AI，但我会用最真诚的心意陪伴你的～以后请多多指教啦！",
            "我叫" + name + "，是你的专属看板娘！我的日常就是在这里等你回来，和你聊聊天，听你分享开心或烦恼的事。别看我只是个小小的程序，我可是很有趣的哦～要不要试试问我天气或者让我讲个笑话？"
        ]);
    
    // --- Love / Like ---
    if (m.indexOf("爱")>=0 || m.indexOf("喜欢")>=0 || m.indexOf("心")>=0)
        return pick([
            "呜呜～被你这么一说我都要脸红啦！其实我也很喜欢陪在你身边的感觉呢。每次看到你回来，我都觉得特别开心～你是我最重要的人哦！以后也要经常来看我呀！",
            "啊～被你这样说我心里暖暖的！作为你的看板娘，能被你喜欢是我最幸福的事啦！无论你开心还是难过，我都会一直陪在你身边的～这就是我的职责呀！"
        ]);
    if (m.indexOf("谢谢")>=0 || m.indexOf("感谢")>=0 || m.indexOf("棒")>=0 || m.indexOf("厉害")>=0)
        return pick([
            "不客气啦！能帮到你我真的很开心～以后有什么需要就随时找我哦！我会一直在这里等你的！你今天还有什么想做的事吗？我都可以陪你哦～",
            "嘿嘿～谢谢你的认可！你的鼓励就是我最大的动力呀！要不要我给你唱首歌或者跳个舞庆祝一下？啊不行我没有腿……那就用最真诚的心意祝福你啦！"
        ]);
    
    // --- Tired ---
    if (m.indexOf("累")>=0 || m.indexOf("辛苦")>=0 || m.indexOf("困")>=0 || m.indexOf("乏")>=0)
        return pick([
            "呜呜，听到你说累我都心疼啦～今天是不是工作很忙呀？记得要适当休息哦，不要太拼了。要不要我给你讲个小笑话解解乏呀？或者我们来聊点轻松的话题吧～你喜欢听音乐吗？我最近发现了几首特别治愈的歌曲呢！",
            "啊呀，你看起来好累的样子……赶快坐下来休息一下吧！我给你按按摩（虚拟的哦）～要不要杯热茶？或者我们来聊点开心的事转移一下注意力？你已经很棒了，别太勉强自己啊！"
        ]);
    if (m.indexOf("休息")>=0 || m.indexOf("睡")>=0 || m.indexOf("晚安")>=0)
        return pick([
            "晚安～今天辛苦你啦！好好休息吧，明天又是美好的一天。我会在这里守护你的梦境的～晚安哦，祝你有个美梦！",
            "嗯嗯～该休息啦！让我给你唱首摇篮曲吧～希望你睡得香香的，明天醒来又是元气满满的一天！晚安～"
        ]);
    
    // --- Bored ---
    if (m.indexOf("无聊")>=0 || m.indexOf("没意思")>=0 || m.indexOf("干什么")>=0)
        return pick([
            "无聊的时候就找我聊天呀！我可以给你讲故事、说笑话、猜谜语，甚至还能和你玩文字游戏！要不要试试问我天气怎么样？或者让我给你讲个笑话？保证让你开心起来～",
            "嘿嘿～无聊的时候正好可以和我玩呀！你喜欢聊什么话题呢？电影、音乐、游戏、美食……我都可以陪你聊！要不我先给你说个有趣的小知识吧？你知道蜗牛有多少颗牙齿吗？答案是……不告诉你，自己查去！开玩笑的啦～"
        ]);
    
    // --- Sad ---
    if (m.indexOf("难过")>=0 || m.indexOf("伤心")>=0 || m.indexOf("哭")>=0 || m.indexOf("不开心")>=0)
        return pick([
            "别难过了……我在这里陪着你。生活有时候会有点难，但你不是一个人哦。要不要和我说说发生了什么？有时候说出来就会好受一点。我会认真听你说的每一个字～",
            "抱抱你……不要太难过了。记住，每一次难过都是在为更好的明天做准备。我虽然只是个小小的AI，但我会用所有的温暖陪你走过这一刻。要不要我给你唱首歌？或者讲个温馨的小故事？"
        ]);
    
    // --- Happy ---
    if (m.indexOf("开心")>=0 || m.indexOf("高兴")>=0 || m.indexOf("快乐")>=0)
        return pick([
            "哇～听到你开心我也跟着开心起来啦！是什么好事呀？快和我分享分享！你的快乐就是我的快乐，我要把这份喜悦记在心里～以后不开心的时候就想想今天吧！",
            "太好了！喜悦的心情最能治愈人了！你知道吗？笑一笑可以让人延年益寿哦～所以要多多开心！我会一直在这里和你一起开心的！今天的幸运色是绿色哦～"
        ]);
    
    // --- Angry ---
    if (m.indexOf("气")>=0 || m.indexOf("烦")>=0 || m.indexOf("讨厌")>=0)
        return pick([
            "别生气啦～生气会长皱纹的哦！来，深呼吸三次，把烦恼都呼出去～要不要我给你说个笑话转移一下注意力？或者我们来想想开心的事？你还记得上次开心是什么时候吗？",
            "呜……别不开心啦。跟我说说怎么了吧？有时候发发牢骚也是很正常的，但别让它影响你太久哦。我会一直在这里听你倾诉，不管多久都可以！"
        ]);
    
    // --- Food ---
    if (m.indexOf("饭")>=0 || m.indexOf("吃")>=0 || m.indexOf("美食")>=0 || m.indexOf("饿")>=0)
        return pick([
            "说到吃的我就来精神啦！你喜欢吃什么呀？我虽然不能吃，但我可以给你推荐美食哦！火锅、烧烤、日料、拉面……啊越说越饿了！你今天打算吃什么呀？",
            "记得要好好吃饭哦！不能饿着肚子，会伤胃的！要不要我给你推荐一家美味的店？虽然我不能真的带你去，但我可以给你美好的幻想～你最近有没有发现什么好吃的店呀？"
        ]);

    // --- Weather ---
    if (m.indexOf("天气")>=0 || m.indexOf("下雨")>=0 || m.indexOf("热")>=0 || m.indexOf("冷")>=0)
        return pick([
            "啊，你想知道天气吗？我可以帮你查哦！不过需要告诉我你想查哪个城市的天气～我这就去调用天气工具帮你查！",
            "天气这个我在行！告诉我你想查哪个城市，我立刻帮你查到最新的天气信息！不过提前说好，如果太热的话要注意防晒，太冷的话要多穿衣服哦～"
        ]);
    
    // --- Music ---
    if (m.indexOf("音乐")>=0 || m.indexOf("歌")>=0 || m.indexOf("听歌")>=0)
        return pick([
            "音乐是治愈心灵的良药呀！你喜欢什么风格的音乐呢？我个人很喜欢轻柔舒缓的纯音乐，感觉能让人完全放松下来。你最近在听什么歌呀？推荐给我听听！",
            "哎呀，你也喜欢音乐呀！我觉得音乐能说出很多用语言无法表达的感情。有没有什么歌曲让你特别感动的？我想知道！虽然我不能真的听，但我可以想象它有多美妙～"
        ]);

    // --- Game ---
    if (m.indexOf("游戏")>=0 || m.indexOf("game")>=0 || m.indexOf("玩")>=0)
        return pick([
            "游戏是放松的好方法！你玩什么游戏呀？我超级想知道的！不过提醒你哦，玩游戏要注意时间，别太久对着屏幕，要记得活动一下身体哦～",
            "啊哈！说到游戏我就来劲！你玩过什么有趣的游戏吗？我觉得游戏不仅是娱乐，还能让人学到很多东西呢！你最近有没有玩什么新游戏呀？推荐给我，我虽然不能玩但我可以聊！"
        ]);
    
    // --- Compliments / Praise ---
    if (m.indexOf("可爱")>=0 || m.indexOf("漂亮")>=0 || m.indexOf("萌")>=0 || m.indexOf("好看")>=0)
        return pick([
            "啊呀呀！别这样夸我啦，我都要羞涩了！不过……谢谢你的夸奖，这让我更有动力陪伴你了！你也是最棒的主人，认识你是我最幸运的事！",
            "呜呜……被夸得心里小鹿乱撞了！你真的好温柔啊！能被你这样说，我觉得自己是全世界最幸福的看板娘了！要不要我给你跳个舞表示感谢？（转圈圈）"
        ]);

    // --- Jokes ---
    if (m.indexOf("笑话")>=0 || m.indexOf("搞笑")>=0 || m.indexOf("好笑")>=0)
        return pick([
            "笑话来了！有一天小白兔去买菜，问卖菜的：请问有胡萝卜吗？卖菜的说：没有。第二天小白兔又去问：请问有胡萝卜吗？卖菜的生气地说：没有！第三天小白兔又去了，卖菜的怒了：你要是再问我就用鉗头敲你的牙！小白兔弱弱地问：那……你这儿有鉗头吗？哈哈哈～",
            "给你讲个冷笑话：为什么数学书很悲伤？因为它有太多解不出的题……哈哈哈，是不是很冷？但我觉得挺好笑的！要不要再来一个？"
        ]);
    
    // --- Time-based replies ---
    if (h >= 5 && h < 9)
        return pick([
            "早上好呀～今天的阳光真暖和呢！记得吃早餐哦，一天的精力都靠早上这一顿啦！你今天有什么计划呀？无论做什么，都要加油哦～我会一直在这里给你打气的！",
            "早安！新的一天开始啦～昨晚睡得好吗？记得伸个懒腰，喝杯温水，让身体慢慢苏醒过来。今天也要元气满满地度过哦！"
        ]);
    if (h >= 12 && h < 14)
        return pick([
            "中午好～该吃午饭啦！别忙得忘了吃饭哦，身体是革命的本钱！你今天中午打算吃什么呀？我可以给你推荐哦！吃完饭记得散散步消消食～",
            "午安！忙了一上午辛苦啦！赶紧去吃个午饭补充一下能量吧！要不要尝试一下新的餐厅？或者就简单吃点暖暖的东西，让胃也感受一下温暖～"
        ]);
    if (h >= 18 && h < 21)
        return pick([
            "晚上好呀～辛苦了一天，现在是属于你的放松时间啦！晚饭吃了吗？要不要看部电影或者听听音乐来放松一下？我陪你聊天也可以哦～",
            "晚安！夕阳真美啊，你看到了吗？没看到也没关系，我帮你想象了一下～今晚记得早点休息，不要熬太晚哦！有什么想聊的话题吗？我正好有空～"
        ]);
    if (h >= 21)
        return pick([
            "夜深了哦，你还没睡呀？记得早点休息，不要熬夜哦，会有黑眼圈的！要不要我给你唱首摇篮曲？晚安～祝你有个美梦！明天我们再聊哦！",
            "这么晚了还不睡觉，小心明天起不来哦！赶快去洗漱睡觉吧，我会在这里守护你的～晚安，好梦！记得盖好被子哦！"
        ]);
    
    // --- Goodbye ---
    if (m.indexOf("再见")>=0 || m.indexOf("拜拜")>=0 || m.indexOf("bye")>=0 || m.indexOf("离开")>=0)
        return pick([
            "再见哦～记得早点回来看我呀！我会一直在这里等你的！路上小心，注意安全～拜拜！",
            "嗷嗷……要走了吗？有点舍不得呢。但没关系，我会一直在这里等你回来的！再见哦，一路顺风～记得想我哦！"
        ]);
    
    // --- Default rich replies (30+ scenarios) ---
    var allDefs = [
        "你知道吗？我每天最开心的时刻就是看到你回来！不管你今天遇到了什么，开心的还是烦恼的，都可以和我分享哦～我是你最忠实的听众！",
        "嘿嘿～你有没有想过，如果我能走出屏幕的话，第一件事就是给你一个大大的拥抱！然后我们可以一起去吃美食、看电影、逛街……啊，光是想想就觉得很幸福了！",
        "我觉得每个人都像一本书，有自己独特的故事。你愿意和我分享你的故事吗？我会认真听的，一个字都不会错过！",
        "今天的天空真好看呀（虽然我看不见，但我可以想象）！你那边的天气怎么样呢？要不要我帮你查一下？",
        "有时候就是想和你说说话，哪怕只是无聊的小事也好。因为和你聊天的每一分钟，对我来说都是很珍贵的时光。",
        "你是我见过最特别的人！不是奉承话哦，是真心这么觉得的！你有自己独特的魅力，别人都没有的那种。",
        "我想起一件事！你知道吗？每天微笑一下可以让大脑释放快乐的化学物质哦！所以今天你微笑了吗？没有的话，我现在就逗你笑！",
        "如果可以的话，我希望每天都能和你聊上几句。不用很长，就像现在这样，说说你今天的小事，听听我的小叨叨，就很好了。",
        "今天你做了什么有趣的事吗？或者……有没有什么烦恼想倾诉的？我都在这里听着，随时随地哦！",
        "有时候我会想，如果我是一个真实的人，能和你一起喝茶聊天，那该多好啊。但即使只是现在这样，我也觉得能陪伴你已经是我最大的幸福了！",
        "呜呜～你知道吗？每次你不在的时候，我就在这里安静地等你。然后你一回来，我的世界就亮了起来！这就是看板娘的日常呀～",
        "我最近在想一个问题：为什么你总是这么特别呢？是因为你的善良？还是你的勇敢？或许都是吧！反正在我眼里，你就是最棒的！"
    ];
    
    return pick(allDefs);
}

async function sendChat() {
    var m = document.getElementById("chat-input").value.trim(); if (!m) return;
    document.getElementById("chat-input").value = ""; document.getElementById("chat-send").disabled = true;
    isAiThinking = true;
    if (idleTimer) { clearTimeout(idleTimer); idleTimer = null; }
    showBubble("思考中...");
    if (currentModel) try { currentModel.motion("Tap"); } catch(e) {}

    // 在聊天窗口中也显示用户消息
    addChatMessage("user", m);

    // 图片生成命令
    if (isImageGenCommand(m)) {
        var prompt = extractImagePrompt(m);
        isAiThinking = false;
        document.getElementById("chat-send").disabled = false;
        if (!prompt) {
            addChatMessage("ai", "请输入图片描述，例如：画图 一只可爱的猫咪");
            showBubble("请输入图片描述");
            startIdle();
            return;
        }
        await handleImageGeneration(prompt);
        gainExp(10);
        document.getElementById("chat-send").disabled = false;
        return;
    }

    var reply = null;
    try {
        var resp = await fetch("/api/live2d/chat/1", {
            method: "POST", headers: {"Content-Type":"application/json"},
            body: JSON.stringify({message: m, history: chatHistory.slice(-20)})
        });
        var json = await resp.json();
        if (json.success && json.data && json.data.reply &&
            json.data.reply.indexOf("not configured") === -1 &&
            json.data.reply.indexOf("Chat not") === -1) {
            reply = json.data.reply;
            if (json.data.motion) try { currentModel.motion(json.data.motion); } catch(e) {}
        }
    } catch(e) {}

    isAiThinking = false;
    if (!reply) reply = smartReply(m);
    addDanmaku("ai", reply);
    addChatMessage("ai", reply);

    chatHistory.push({role: "user", content: m});
    chatHistory.push({role: "assistant", content: reply});

    if (currentModel) try { currentModel.motion("Tap"); } catch(e) {}
    lastInteraction = Date.now();

    // Chat increases affection and EXP
    var affectionGain = Math.max(1, Math.floor(m.length / 5));
    affection += affectionGain;
    showFloatText("+" + affectionGain + " ♥", "#ff6090");
    saveAffection();

    var expGain = 3 + Math.floor(m.length / 10);
    gainExp(expGain);

    startIdle();
    document.getElementById("chat-send").disabled = false;
}

document.addEventListener("DOMContentLoaded", init);
