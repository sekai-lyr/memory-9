// Live2D Kanban - Interactive System
var app, currentModel, bubbleTimer, mikuSpeechTimer, idleTimer, lastInteraction = Date.now();
var models2D = [], currentModelName = "", charId = 1;
var isDragging = false, dragStart = { x:0, y:0 }, modelStart = { x:0, y:0 }, tapCooldown = false;
var modelScaleDefaults = { "haru_ja": 0.15, "hiyori_en": 0.15, "hatsune_miku": 0.42 };
var modelScaleFactor = 1, isResizing = false;
var resizeStart = { x:0, y:0, centerX:0, centerY:0, distance:1, factor:1 };
var MODEL_SCALE_MIN = 0.5, MODEL_SCALE_MAX = 2.5;
var affection = 0;
var mousePos = { x: window.innerWidth / 2, y: window.innerHeight / 2 };
var pointerFrame = null;
var followActive = true, lastExpression = "";
var isAiThinking = false;
var prefersReducedMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

function playUiAnimation(el, keyframes, options) {
    if (!el || prefersReducedMotion || typeof el.animate !== "function") return null;
    var config = Object.assign({ fill: "both", easing: "cubic-bezier(0.16, 1, 0.3, 1)" }, options || {});
    return el.animate(keyframes, config);
}

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
var expFillAnimation = null;
function onStatsChange(fn) { _statsListeners.push(fn); }
function emitStatsChange() { _statsListeners.forEach(function(f) { try { f(charStats); } catch(e) {} }); }

function animateStatValue(id, value) {
    var el = document.getElementById(id);
    if (!el) return;
    var next = String(value);
    if (el.textContent === next) return;
    el.textContent = next;
    playUiAnimation(el, [
        { opacity: 0.45, transform: "translateY(4px) scale(0.92)" },
        { opacity: 1, transform: "translateY(0) scale(1)" }
    ], { duration: 320 });
}

function updateStatsUI() {
    animateStatValue("s-level", charStats.level);
    animateStatValue("s-hp", charStats.hp);
    animateStatValue("s-atk", charStats.atk);
    animateStatValue("s-def", charStats.def);
    animateStatValue("free-pts", charStats.freePoints);
    animateStatValue("aff-val", affection);
    var pct = charStats.expToNext > 0 ? (charStats.exp / charStats.expToNext * 100) : 100;
    var fill = document.getElementById("exp-bar-fill");
    var nextProgress = Math.max(0, Math.min(1, pct / 100));
    var previousProgress = parseFloat(fill.dataset.progress || "0");
    fill.dataset.progress = String(nextProgress);
    fill.style.transform = "scaleX(" + nextProgress + ")";
    if (expFillAnimation) expFillAnimation.cancel();
    if (Math.abs(previousProgress - nextProgress) > 0.001) {
        expFillAnimation = playUiAnimation(fill, [
            { transform: "scaleX(" + previousProgress + ")" },
            { transform: "scaleX(" + nextProgress + ")" }
        ], { duration: 520 });
    }
    document.getElementById("exp-text").textContent = "经验 " + charStats.exp + " / " + charStats.expToNext;
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

function setChatBusy(busy) {
    var buttons = [
        document.getElementById("chat-send"),
        document.querySelector("#chat-win-input button")
    ];
    buttons.forEach(function(button) {
        if (!button) return;
        if (busy) {
            if (!button.dataset.readyLabel) button.dataset.readyLabel = button.textContent;
            button.disabled = true;
            button.classList.add("is-busy");
            button.setAttribute("aria-busy", "true");
            button.textContent = "思考中";
        } else {
            button.disabled = false;
            button.classList.remove("is-busy");
            button.removeAttribute("aria-busy");
            button.textContent = button.dataset.readyLabel || "发送";
        }
    });
    var chatArea = document.getElementById("chat-area");
    if (chatArea) chatArea.classList.toggle("is-busy", busy);
}

function log(msg) {
    var status = document.getElementById("status-text");
    if (!status) return;
    status.textContent = msg;
    status.classList.remove("status-flip");
    void status.offsetWidth;
    status.classList.add("status-flip");
    playUiAnimation(status, [
        { opacity: 0.35, transform: "translateX(-50%) translateY(4px)" },
        { opacity: 1, transform: "translateX(-50%) translateY(0)" }
    ], { duration: 220 });
}

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
    d.innerHTML = '<div class="dlg-item" onclick="doTrain()">训练角色</div>';
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
    showBubble("训练辛苦了，经验加 10，羁绊加 2");
    showFloatText("经验 +10 | 羁绊 +2", "var(--ds-accent-strong)");
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
            toast(stat + " 属性 +1");
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
    var collapsed = p.classList.contains("collapsed");
    b.textContent = collapsed ? "OPEN" : "CLOSE";
    b.title = collapsed ? "展开角色状态" : "收起角色状态";
    b.setAttribute("aria-expanded", String(!collapsed));
    var content = p.querySelector(".stats-content");
    if (content) {
        playUiAnimation(content, collapsed ? [
            { opacity: 1, transform: "translateY(0)" },
            { opacity: 0, transform: "translateY(-8px)" }
        ] : [
            { opacity: 0, transform: "translateY(-8px)" },
            { opacity: 1, transform: "translateY(0)" }
        ], { duration: 280 });
    }
}

function flashLevelUp(times) {
    var el = document.getElementById("level-up-flash");
    el.textContent = times > 1 ? "等级提升 x" + times : "等级提升";
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
            var ratios = { "haru_ja": 0.35, "hiyori_en": 0.35, "hatsune_miku": 0.3 };
            headY = currentModel.y - mh * (ratios[currentModelName] || 0.3);
        }
    }

    // Strategy 3: Estimate height from canvas info and scale, then use ratio
    if (headY === null) {
        var knownH = currentModel._knownCanvasH;
        if (knownH && knownH > 0) {
            var estH = knownH * (currentModel.scale ? currentModel.scale.y : (currentModel.scale || 0.15));
            var ratios = { "haru_ja": 0.35, "hiyori_en": 0.35, "hatsune_miku": 0.3 };
            if (estH > 0) headY = currentModel.y - estH * (ratios[currentModelName] || 0.3);
        }
    }

    // Strategy 4: Hardcoded pixel offsets
    if (headY === null) {
        var off = { "haru_ja": 160, "hiyori_en": 160, "hatsune_miku": 170 };
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
    if (mikuSpeechTimer) clearTimeout(mikuSpeechTimer);
    if (currentModel && currentModel.setMouthOpen) {
        var speechDuration = Math.min(2800, Math.max(520, String(t || "").length * 42));
        currentModel.setMouthOpen(0.72, speechDuration);
        mikuSpeechTimer = setTimeout(function() {
            if (currentModel && currentModel.setMouthOpen) currentModel.setMouthOpen(0, 0);
        }, speechDuration);
    }
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
        if (!currentModel || isDragging || isResizing || isAiThinking) return;
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

// --- Model size controls ---
function clampModelScaleFactor(value) {
    return Math.max(MODEL_SCALE_MIN, Math.min(MODEL_SCALE_MAX, value));
}

function getModelBaseScale(name) {
    return modelScaleDefaults[name] || 0.15;
}

function getModelScaleStorageKey(name) {
    return "divastage:model-scale:" + name;
}

function readModelScaleFactor(name) {
    try {
        var saved = parseFloat(localStorage.getItem(getModelScaleStorageKey(name)));
        if (Number.isFinite(saved)) return clampModelScaleFactor(saved);
    } catch(_) {}
    return 1;
}

function saveModelScaleFactor() {
    try { localStorage.setItem(getModelScaleStorageKey(currentModelName), String(modelScaleFactor)); } catch(_) {}
}

function updateModelScaleControls() {
    var slider = document.getElementById("model-size-slider");
    var value = document.getElementById("model-size-value");
    var percent = Math.round(modelScaleFactor * 100);
    if (slider) slider.value = String(percent);
    if (value) value.textContent = percent + "%";
}

function setModelScaleFactor(factor, shouldPersist) {
    if (!currentModel || !Number.isFinite(Number(factor))) return;
    modelScaleFactor = clampModelScaleFactor(Number(factor));
    currentModel.scale.set(getModelBaseScale(currentModelName) * modelScaleFactor);
    updateModelScaleControls();
    if (shouldPersist !== false) saveModelScaleFactor();
    updateBubblePosition();
    updateModelResizeUi();
}

function setModelScaleFromControl(value) {
    setModelScaleFactor(parseFloat(value) / 100);
}

function resetModelSize() {
    setModelScaleFactor(1);
    showToast("模型大小已重置");
}

function updateModelResizeUi() {
    var ui = document.getElementById("model-resize-ui");
    var outline = document.getElementById("model-resize-outline");
    var handle = document.getElementById("model-resize-handle");
    if (!ui || !outline || !handle) return;
    if (!currentModel || !document.body.classList.contains("model-ready")) {
        ui.hidden = true;
        return;
    }

    var bounds;
    try { bounds = currentModel.getBounds(); } catch(_) { return; }
    if (!bounds || !Number.isFinite(bounds.x) || !Number.isFinite(bounds.y) ||
        !Number.isFinite(bounds.width) || !Number.isFinite(bounds.height) ||
        bounds.width <= 0 || bounds.height <= 0) return;

    ui.hidden = false;
    outline.style.left = bounds.x + "px";
    outline.style.top = bounds.y + "px";
    outline.style.width = bounds.width + "px";
    outline.style.height = bounds.height + "px";
    handle.style.left = (bounds.x + bounds.width) + "px";
    handle.style.top = (bounds.y + bounds.height) + "px";
}

function startModelResize(e) {
    if (!currentModel || isAiThinking) return;
    e.preventDefault();
    e.stopPropagation();
    var bounds;
    try { bounds = currentModel.getBounds(); } catch(_) { return; }
    var centerX = bounds.x + bounds.width / 2;
    var centerY = bounds.y + bounds.height / 2;
    var distance = Math.hypot(e.clientX - centerX, e.clientY - centerY);
    resizeStart = {
        x: e.clientX,
        y: e.clientY,
        centerX: centerX,
        centerY: centerY,
        distance: Math.max(1, distance),
        factor: modelScaleFactor
    };
    isResizing = true;
    currentModel.alpha = 0.8;
    var ui = document.getElementById("model-resize-ui");
    if (ui) ui.classList.add("is-resizing");
    try { e.currentTarget.setPointerCapture(e.pointerId); } catch(_) {}
}

function moveModelResize(e) {
    if (!isResizing) return;
    e.preventDefault();
    var distance = Math.hypot(e.clientX - resizeStart.centerX, e.clientY - resizeStart.centerY);
    setModelScaleFactor(resizeStart.factor * distance / resizeStart.distance, false);
}

function endModelResize() {
    if (!isResizing) return;
    isResizing = false;
    if (currentModel) currentModel.alpha = 1;
    saveModelScaleFactor();
    var ui = document.getElementById("model-resize-ui");
    if (ui) ui.classList.remove("is-resizing");
    updateModelResizeUi();
}

function adjustModelScaleWithKeyboard(e) {
    var delta = 0;
    if (e.key === "ArrowRight" || e.key === "ArrowUp") delta = e.shiftKey ? 0.1 : 0.05;
    if (e.key === "ArrowLeft" || e.key === "ArrowDown") delta = e.shiftKey ? -0.1 : -0.05;
    if (e.key === "Home") delta = 1 - modelScaleFactor;
    if (e.key === "End") delta = MODEL_SCALE_MAX - modelScaleFactor;
    if (!delta) return;
    e.preventDefault();
    setModelScaleFactor(modelScaleFactor + delta);
}

function setupModelResizeControls() {
    var handle = document.getElementById("model-resize-handle");
    if (!handle || handle.dataset.bound === "true") return;
    handle.dataset.bound = "true";
    handle.addEventListener("pointerdown", startModelResize);
    handle.addEventListener("keydown", adjustModelScaleWithKeyboard);
    window.addEventListener("pointermove", moveModelResize);
    window.addEventListener("pointerup", endModelResize);
    window.addEventListener("pointercancel", endModelResize);
}

// --- Init ---
async function init() {
    log("正在启动舞台...");
    var canvas = document.getElementById("live2d-canvas");

    app = new PIXI.Application({
        view: canvas, resizeTo: window, backgroundAlpha: 0,
        antialias: true, resolution: window.devicePixelRatio || 1
    });
    setupModelResizeControls();
    app.ticker.add(updateModelResizeUi);

    log("正在启动 Cubism4...");
    try {
        PIXI.live2d.startUpCubism4();
        await PIXI.live2d.cubism4Ready;
        log("Cubism4 已就绪");
    } catch(e) { log("Cubism4 启动失败: " + e.message); return; }
    await loadCharStats();
    await loadModelList();

    window.addEventListener("resize", function() {
        if (currentModel) {
            currentModel.x = window.innerWidth / 2;
            currentModel.y = (window.innerHeight - 80) / 2;
            updateBubblePosition();
            updateModelResizeUi();
        }
    });

    document.addEventListener("mousemove", function(e) {
        mousePos.x = e.clientX;
        mousePos.y = e.clientY;
        if (pointerFrame === null && !prefersReducedMotion) {
            pointerFrame = requestAnimationFrame(function() {
                document.documentElement.style.setProperty("--pointer-x", mousePos.x + "px");
                document.documentElement.style.setProperty("--pointer-y", mousePos.y + "px");
                pointerFrame = null;
            });
        }
        if (currentModel && followActive && !isDragging && !isResizing) {
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
async function createHatsuneMikuModel() {
    var baseUrl = "/live2d-models/hatsune_miku/";
    var bodyTexture;
    try {
        bodyTexture = await PIXI.Assets.load(baseUrl + "hatsune_miku.png");
    } catch(_) {
        bodyTexture = await PIXI.Assets.load(baseUrl + "hatsune_miku.svg");
    }
    var modelWidth = bodyTexture.width || 881;
    var modelHeight = bodyTexture.height || 1589;
    var centerX = modelWidth / 2;
    var centerY = modelHeight / 2;
    var model = new PIXI.Container();
    var visual = new PIXI.Container();
    var body = PIXI.SimplePlane
        ? new PIXI.SimplePlane(bodyTexture, 9, 17)
        : new PIXI.Sprite(bodyTexture);
    if (PIXI.SimplePlane) {
        body.position.set(-centerX, -centerY);
    } else {
        body.anchor.set(0.5);
    }
    var bodyPositionBuffer = body.geometry && typeof body.geometry.getBuffer === "function"
        ? body.geometry.getBuffer("aVertexPosition") : null;
    var bodyBaseVertices = bodyPositionBuffer ? new Float32Array(bodyPositionBuffer.data) : null;
    visual.addChild(body);
    model.addChild(visual);

    function toLocalPoints(points) {
        var local = [];
        points.forEach(function(point) {
            local.push(point[0] - centerX, point[1] - centerY);
        });
        return local;
    }

    function createMaskedLayer(points) {
        var layer = new PIXI.Container();
        var sprite = new PIXI.Sprite(bodyTexture);
        var mask = new PIXI.Graphics();
        sprite.anchor.set(0.5);
        mask.beginFill(0xffffff);
        mask.drawPolygon(toLocalPoints(points));
        mask.endFill();
        mask.renderable = false;
        sprite.mask = mask;
        layer.addChild(sprite);
        layer.addChild(mask);
        visual.addChild(layer);
        return layer;
    }

    // Broad masks retain the original illustration while making the hair
    // pieces independently movable, like a lightweight cutout rig.
    var leftTwinTail = createMaskedLayer([
        [0, 0], [302, 0], [323, 220], [300, 520], [302, 820],
        [276, 1100], [230, 1325], [125, 1535], [0, 1589],
        [22, 1310], [60, 1060], [74, 820], [92, 550], [58, 260]
    ]);
    var rightTwinTail = createMaskedLayer([
        [579, 0], [881, 0], [823, 260], [789, 550], [807, 820],
        [821, 1060], [859, 1310], [881, 1589], [756, 1535],
        [651, 1325], [605, 1100], [579, 820], [581, 520], [558, 220]
    ]);
    var frontHair = createMaskedLayer([
        [255, 40], [626, 40], [670, 120], [641, 190], [606, 286],
        [566, 237], [520, 306], [473, 245], [440, 320], [399, 246],
        [347, 298], [314, 239], [268, 286], [226, 170]
    ]);
    var torso = createMaskedLayer([
        [286, 286], [595, 286], [642, 480], [595, 690], [440, 748],
        [285, 690], [238, 480]
    ]);
    var leftSleeve = createMaskedLayer([
        [170, 386], [350, 382], [351, 680], [287, 820], [156, 850],
        [113, 690], [125, 500]
    ]);
    var rightSleeve = createMaskedLayer([
        [530, 382], [711, 386], [756, 500], [768, 690], [725, 850],
        [594, 820], [530, 680]
    ]);
    var skirt = createMaskedLayer([
        [222, 578], [658, 578], [738, 815], [680, 860], [200, 860], [143, 815]
    ]);
    var tie = createMaskedLayer([
        [388, 298], [493, 298], [523, 630], [440, 735], [357, 630]
    ]);

    function setLayerPivot(layer, x, y) {
        var px = x - centerX;
        var py = y - centerY;
        layer.pivot.set(px, py);
        layer.position.set(px, py);
        return { x: px, y: py };
    }
    var torsoPivot = setLayerPivot(torso, 440, 470);
    var leftSleevePivot = setLayerPivot(leftSleeve, 274, 490);
    var rightSleevePivot = setLayerPivot(rightSleeve, 606, 490);
    var skirtPivot = setLayerPivot(skirt, 440, 720);
    var tiePivot = setLayerPivot(tie, 440, 430);

    var faceFx = new PIXI.Container();
    var faceEyeY = modelHeight * 0.145 - centerY;
    var faceMouthY = modelHeight * 0.174 - centerY;
    var eyeOffsetX = modelWidth * 0.058;
    var skinColor = 0xf3d9d3;
    var mouthColor = 0x5d4c59;
    var eyeLidLeft = new PIXI.Graphics();
    var eyeLidRight = new PIXI.Graphics();
    var mouthPatch = new PIXI.Graphics();
    var mouth = new PIXI.Graphics();
    var leftCheek = new PIXI.Graphics();
    var rightCheek = new PIXI.Graphics();
    var leftBrow = new PIXI.Graphics();
    var rightBrow = new PIXI.Graphics();

    function makeEyeLid(graphics) {
        graphics.beginFill(skinColor);
        graphics.drawEllipse(0, 0, modelWidth * 0.033, modelHeight * 0.012);
        graphics.endFill();
        graphics.alpha = 0;
        faceFx.addChild(graphics);
    }

    makeEyeLid(eyeLidLeft);
    makeEyeLid(eyeLidRight);
    mouthPatch.beginFill(skinColor);
    mouthPatch.drawEllipse(0, 0, modelWidth * 0.033, modelHeight * 0.011);
    mouthPatch.endFill();
    mouthPatch.alpha = 0;
    faceFx.addChild(mouthPatch);

    leftCheek.beginFill(0xf28ea4, 0.22);
    leftCheek.drawEllipse(0, 0, modelWidth * 0.026, modelHeight * 0.009);
    leftCheek.endFill();
    rightCheek.beginFill(0xf28ea4, 0.22);
    rightCheek.drawEllipse(0, 0, modelWidth * 0.026, modelHeight * 0.009);
    rightCheek.endFill();
    faceFx.addChild(leftCheek);
    faceFx.addChild(rightCheek);

    leftBrow.lineStyle(3, mouthColor, 0.75);
    leftBrow.moveTo(-modelWidth * 0.032, 0);
    leftBrow.lineTo(modelWidth * 0.005, -modelHeight * 0.006);
    rightBrow.lineStyle(3, mouthColor, 0.75);
    rightBrow.moveTo(-modelWidth * 0.005, -modelHeight * 0.006);
    rightBrow.lineTo(modelWidth * 0.032, 0);
    leftBrow.alpha = 0;
    rightBrow.alpha = 0;
    faceFx.addChild(leftBrow);
    faceFx.addChild(rightBrow);
    visual.addChild(faceFx);

    eyeLidLeft.position.set(-eyeOffsetX, faceEyeY);
    eyeLidRight.position.set(eyeOffsetX, faceEyeY);
    mouthPatch.position.set(0, faceMouthY);
    mouth.position.set(0, faceMouthY);
    leftCheek.position.set(-modelWidth * 0.09, faceMouthY + modelHeight * 0.019);
    rightCheek.position.set(modelWidth * 0.09, faceMouthY + modelHeight * 0.019);
    leftBrow.position.set(-eyeOffsetX, faceEyeY - modelHeight * 0.025);
    rightBrow.position.set(eyeOffsetX, faceEyeY - modelHeight * 0.025);

    var expressionNames = ["Normal", "Smile", "Sad", "Angry", "Surprised"];
    var expression = "Normal";
    var mouthOpen = 0;
    var mouthTarget = 0;

    function redrawFaceFx() {
        var isOpen = mouthOpen > 0.045 || expression !== "Normal";
        mouthPatch.alpha = isOpen ? 0.96 : 0;
        mouth.clear();
        if (isOpen) {
            var open = Math.max(0.03, mouthOpen);
            mouth.lineStyle(2, mouthColor, 0.92);
            if (expression === "Surprised") {
                mouth.beginFill(mouthColor, 0.82);
                mouth.drawEllipse(0, 0, modelWidth * 0.012 + open * 8, modelHeight * 0.009 + open * 8);
                mouth.endFill();
            } else {
                mouth.moveTo(-modelWidth * 0.022, expression === "Sad" ? modelHeight * 0.003 : 0);
                mouth.quadraticCurveTo(0,
                    (expression === "Sad" ? -1 : 1) * modelHeight * 0.006 + open * 9,
                    modelWidth * 0.022, expression === "Sad" ? modelHeight * 0.003 : 0);
            }
        }
        var cheekAlpha = expression === "Smile" ? 0.42 : expression === "Sad" ? 0.12 : 0.06;
        leftCheek.alpha = cheekAlpha;
        rightCheek.alpha = cheekAlpha;
        leftBrow.alpha = expression === "Angry" || expression === "Sad" ? 0.78 : 0;
        rightBrow.alpha = leftBrow.alpha;
        if (expression === "Angry") {
            leftBrow.rotation = -0.16;
            rightBrow.rotation = 0.16;
        } else if (expression === "Sad") {
            leftBrow.rotation = 0.15;
            rightBrow.rotation = -0.15;
        } else {
            leftBrow.rotation = 0;
            rightBrow.rotation = 0;
        }
        setParameterAliases(["EyeLSmile", "ParamEyeLSmile", "PARAM_EYE_L_SMILE"], expression === "Smile" ? 1 : 0);
        setParameterAliases(["EyeRSmile", "ParamEyeRSmile", "PARAM_EYE_R_SMILE"], expression === "Smile" ? 1 : 0);
        setParameterAliases(["MouthForm", "ParamMouthForm", "PARAM_MOUTH_FORM"], expression === "Smile" ? 1 : expression === "Sad" ? -1 : 0);
        setParameterAliases(["Cheek", "ParamCheek", "PARAM_TERE"], cheekAlpha);
        setParameterAliases(["BrowLForm", "ParamBrowLForm", "PARAM_BROW_L_FORM"], expression === "Angry" ? -1 : expression === "Sad" ? 1 : 0);
        setParameterAliases(["BrowRForm", "ParamBrowRForm", "PARAM_BROW_R_FORM"], expression === "Angry" ? -1 : expression === "Sad" ? 1 : 0);
    }

    function updateBodyMesh(focusX, focusY, phase, motionPulse) {
        if (!bodyPositionBuffer || !bodyBaseVertices) return;
        var vertices = bodyPositionBuffer.data;
        var vertexCount = bodyBaseVertices.length / 2;
        var breath = prefersReducedMotion ? 0 : Math.sin(phase * 1.8);
        for (var i = 0; i < vertexCount; i++) {
            var index = i * 2;
            var baseX = bodyBaseVertices[index];
            var baseY = bodyBaseVertices[index + 1];
            var ny = modelHeight > 0 ? baseY / modelHeight : 0;
            var torsoWeight = Math.max(0, 1 - Math.abs(ny - 0.42) / 0.25);
            var skirtWeight = Math.max(0, 1 - Math.abs(ny - 0.49) / 0.2);
            var perspective = 1 + focusX * 0.055 * (0.5 - ny);
            var x = centerX + (baseX - centerX) * perspective + focusX * (1 - ny) * 15;
            var y = baseY - focusY * (1 - ny) * 5;
            x += breath * torsoWeight * 1.8;
            y -= breath * torsoWeight * 1.1;
            x += Math.sin(phase * 1.5 + ny * 4.2) * (skirtWeight * 2 + motionPulse * 2);
            y += motionPulse * skirtWeight * 3;
            vertices[index] = x;
            vertices[index + 1] = y;
        }
        bodyPositionBuffer.update();
    }

    model.hitArea = new PIXI.Rectangle(-modelWidth / 2, -modelHeight / 2, modelWidth, modelHeight);
    model._mikuVisual = visual;
    model._mikuFocusX = 0;
    model._mikuFocusY = 0;
    model._mikuFocusTargetX = 0;
    model._mikuFocusTargetY = 0;
    model._mikuMotion = "Idle";
    model._mikuMotionBase = "Idle";
    model._mikuMotionStarted = 0;
    model._mikuMotionUntil = 0;
    model._mikuExpression = expression;
    model._mikuMouthOpen = 0;
    model._mikuMouthHoldUntil = 0;
    model._mikuParameters = {
        AngleX: 0,
        AngleY: 0,
        AngleZ: 0,
        EyeLOpen: 1,
        EyeLSmile: 0,
        EyeROpen: 1,
        EyeRSmile: 0,
        EyeBallX: 0,
        EyeBallY: 0,
        BrowLForm: 0,
        BrowRForm: 0,
        MouthForm: 0,
        MouthOpenY: 0,
        Cheek: 0,
        BodyAngleZ: 0,
        BodyAngleX: 0,
        BodyAngleY: 0,
        Breath: 0,
        ArmLA: 0,
        ArmRA: 0,
        BustY: 0,
        HairAhoge: 0,
        HairFront: 0,
        HairSide: 0,
        HairBack: 0,
        HairSideUp: 0,
        Ribbon: 0,
        Skirt: 0,
        SideUpRibbon: 0,
        EyeBlink: 1,
        MouthOpen: 0
    };
    function setParameterAliases(ids, value) {
        ids.forEach(function(id) { model._mikuParameters[id] = value; });
    }
    model.getParameterValueById = function(id) {
        return Number(model._mikuParameters[id] || 0);
    };
    model.setParameterValueById = function(id, value) {
        model._mikuParameters[id] = Number(value) || 0;
    };
    model.focus = {
        set: function(nx, ny) {
            model._mikuFocusTargetX = Math.max(-1, Math.min(1, (Number(nx) - 0.5) * 2));
            model._mikuFocusTargetY = Math.max(-1, Math.min(1, (Number(ny) - 0.5) * 2));
        }
    };
    model.motion = function(name) {
        var nextMotion = name || "Idle";
        var now = performance.now();
        var motionBase = nextMotion.split("@")[0];
        model._mikuMotion = nextMotion;
        model._mikuMotionBase = motionBase;
        model._mikuMotionStarted = now;
        model._mikuMotionUntil = now + (motionBase === "Idle" ? 900 : 720);
        if (motionBase !== "Idle") model._mikuMouthHoldUntil = now + 420;
    };
    model.expression = function(name) {
        expression = expressionNames.indexOf(name) >= 0 ? name : "Normal";
        model._mikuExpression = expression;
        redrawFaceFx();
    };
    model.setMouthOpen = function(value, duration) {
        mouthTarget = Math.max(0, Math.min(1, Number(value) || 0));
        model._mikuMouthHoldUntil = performance.now() + (Number(duration) || 0);
    };
    var nextBlinkAt = performance.now() + 1800 + Math.random() * 2600;
    var blinkStarted = 0;
    var lastTickAt = performance.now();
    model._mikuTick = function() {
        if (model.destroyed) return;
        var now = performance.now();
        var phase = now / 1000;
        var delta = Math.min(0.05, Math.max(0.001, (now - lastTickAt) / 1000));
        lastTickAt = now;
        var motionActive = now < model._mikuMotionUntil;
        var motion = model._mikuMotion;
        var motionBase = model._mikuMotionBase || motion;
        var motionProgress = motionActive && model._mikuMotionUntil > model._mikuMotionStarted
            ? Math.max(0, Math.min(1, (now - model._mikuMotionStarted) /
                (model._mikuMotionUntil - model._mikuMotionStarted))) : 0;
        var motionPulse = motionActive ? Math.sin(motionProgress * Math.PI) : 0;
        model._mikuFocusX += (model._mikuFocusTargetX - model._mikuFocusX) * Math.min(1, delta * 8);
        model._mikuFocusY += (model._mikuFocusTargetY - model._mikuFocusY) * Math.min(1, delta * 8);
        var focusX = model._mikuFocusX;
        var focusY = model._mikuFocusY;
        var bob = prefersReducedMotion ? 0 : Math.sin(phase * 1.8) * 3;
        var shake = motionBase === "Shake" && !prefersReducedMotion ? Math.sin(phase * 24) * 0.035 * motionPulse : 0;
        var focusLean = focusX * 0.04;
        var actionLean = motionBase === "Flick" || motionBase === "FlickDown" ? -0.045 * motionPulse
            : motionBase === "FlickLeft" ? 0.055 * motionPulse
                : motionBase === "FlickRight" ? -0.055 * motionPulse
                    : motionBase === "Tap" ? 0.02 * motionPulse : 0;
        visual.x = focusX * 10;
        visual.y = bob - motionPulse * 7 + focusY * 5;
        visual.rotation = focusLean + actionLean + shake - motionPulse * 0.02;
        var breathe = prefersReducedMotion ? 1 : 1 + Math.sin(phase * 1.8) * 0.006;
        visual.scale.set(breathe + motionPulse * 0.004, breathe - motionPulse * 0.002);
        updateBodyMesh(focusX, focusY, phase, motionPulse);
        var bodyBreath = prefersReducedMotion ? 0 : Math.sin(phase * 1.8);
        torso.position.set(torsoPivot.x + focusX * 3, torsoPivot.y + bodyBreath * 1.5 - motionPulse * 2);
        torso.rotation = focusX * 0.012 + bodyBreath * 0.004;
        torso.scale.set(1 + motionPulse * 0.004, 1 + bodyBreath * 0.004);
        leftSleeve.position.set(leftSleevePivot.x + focusX * 2, leftSleevePivot.y + motionPulse * 2);
        rightSleeve.position.set(rightSleevePivot.x + focusX * 2, rightSleevePivot.y + motionPulse * 2);
        leftSleeve.rotation = Math.sin(phase * 1.55 + 0.5) * 0.018 + focusX * 0.012 + shake;
        rightSleeve.rotation = -Math.sin(phase * 1.55 + 0.8) * 0.018 + focusX * 0.012 - shake;
        skirt.position.set(skirtPivot.x + focusX * 2, skirtPivot.y + bodyBreath * 1.2 - motionPulse * 2);
        skirt.rotation = Math.sin(phase * 1.35) * 0.012 + focusX * 0.01;
        tie.position.set(tiePivot.x + focusX * 2, tiePivot.y + bodyBreath * 2);
        tie.rotation = Math.sin(phase * 1.65 + 0.4) * 0.025 + focusX * 0.014;
        leftTwinTail.x = focusX * 3 + Math.sin(phase * 1.45) * 2;
        leftTwinTail.y = Math.sin(phase * 1.7) * 2 - motionPulse * 4;
        leftTwinTail.rotation = Math.sin(phase * 1.45 + 0.4) * 0.035 + focusX * 0.018 + shake;
        rightTwinTail.x = focusX * 3 - Math.sin(phase * 1.45) * 2;
        rightTwinTail.y = Math.sin(phase * 1.7 + 0.8) * 2 - motionPulse * 4;
        rightTwinTail.rotation = -Math.sin(phase * 1.45 + 0.8) * 0.035 + focusX * 0.018 - shake;
        frontHair.x = focusX * 2;
        frontHair.y = focusY * 2 + Math.sin(phase * 2.4) * 1.2;
        frontHair.rotation = focusX * 0.015 + Math.sin(phase * 2.1) * 0.008;
        faceFx.x = focusX * 1.2;
        faceFx.y = focusY * 1.4;

        if (!prefersReducedMotion && now >= nextBlinkAt && !blinkStarted) blinkStarted = now;
        var blink = 0;
        if (blinkStarted) {
            var blinkAge = now - blinkStarted;
            blink = blinkAge < 170 ? Math.sin((blinkAge / 170) * Math.PI) : 0;
            if (blinkAge >= 170) {
                blinkStarted = 0;
                nextBlinkAt = now + 2500 + Math.random() * 4200;
            }
        }
        eyeLidLeft.alpha = blink;
        eyeLidRight.alpha = blink;
        model._mikuParameters.EyeBlink = 1 - blink;

        if (now >= model._mikuMouthHoldUntil) mouthTarget = 0;
        var motionMouth = motionActive && motionBase !== "Idle" ? 0.16 + motionPulse * 0.2 : 0;
        var expressionMouth = expression === "Surprised" ? 0.42 : expression === "Smile" ? 0.16 : 0;
        var nextMouth = Math.max(mouthTarget, motionMouth, expressionMouth);
        mouthOpen += (nextMouth - mouthOpen) * Math.min(1, delta * 13);
        model._mikuMouthOpen = mouthOpen;
        model._mikuParameters.MouthOpen = mouthOpen;
        setParameterAliases(["AngleX", "ParamAngleX", "PARAM_ANGLE_X"], focusX);
        setParameterAliases(["AngleY", "ParamAngleY", "PARAM_ANGLE_Y"], -focusY);
        setParameterAliases(["AngleZ", "ParamAngleZ", "PARAM_ANGLE_Z"], visual.rotation * 10);
        setParameterAliases(["EyeLOpen", "ParamEyeLOpen", "PARAM_EYE_L_OPEN"], 1 - blink);
        setParameterAliases(["EyeROpen", "ParamEyeROpen", "PARAM_EYE_R_OPEN"], 1 - blink);
        setParameterAliases(["EyeBallX", "ParamEyeBallX", "PARAM_EYE_BALL_X"], focusX * 0.7);
        setParameterAliases(["EyeBallY", "ParamEyeBallY", "PARAM_EYE_BALL_Y"], -focusY * 0.7);
        setParameterAliases(["MouthOpenY", "ParamMouthOpenY", "PARAM_MOUTH_OPEN_Y"], mouthOpen);
        setParameterAliases(["BodyAngleX", "ParamBodyAngleX", "PARAM_BODY_ANGLE_X"], focusX * 0.45);
        setParameterAliases(["BodyAngleY", "ParamBodyAngleY", "PARAM_BODY_ANGLE_Y"], -focusY * 0.35);
        setParameterAliases(["BodyAngleZ", "ParamBodyAngleZ", "PARAM_BODY_ANGLE_Z"], visual.rotation * 10);
        setParameterAliases(["Breath", "ParamBreath", "PARAM_BREATH"], Math.sin(phase * 1.8));
        setParameterAliases(["BustY", "ParamBustY", "PARAM_BUST_Y"], bodyBreath * 0.5 + motionPulse * 0.2);
        setParameterAliases(["HairFront", "ParamHairFront", "PARAM_HAIR_FRONT"], leftTwinTail.rotation * 10);
        setParameterAliases(["HairSide", "ParamHairSide", "PARAM_HAIR_SIDE"], rightTwinTail.rotation * 10);
        setParameterAliases(["HairBack", "ParamHairBack", "PARAM_HAIR_BACK"], (leftTwinTail.rotation - rightTwinTail.rotation) * 6);
        setParameterAliases(["Ribbon", "ParamRibbon"], tie.rotation * 12);
        setParameterAliases(["Skirt", "ParamSkirt"], skirt.rotation * 12 + motionPulse * 0.2);
        redrawFaceFx();
        if (!motionActive && motion !== "Idle") model._mikuMotion = "Idle";
    };
    model._mikuTick();
    app.ticker.add(model._mikuTick);
    return model;
}

async function loadModelList() {
    try {
        var r = await fetch("/api/model/2d/list");
        var j = await r.json();
        var sel = document.getElementById("model-select");
        sel.innerHTML = "";
        if (j.success && j.data && j.data.length) {
            models2D = j.data;
            appendModelOptions(sel);
            sel.value = models2D[0];
            await switchModel(models2D[0]);
        } else { sel.innerHTML = "<option value=\"\">暂无可用角色</option>"; log("暂无可用角色"); }
    } catch(e) { log("角色列表加载失败: "+e.message); }
}

function appendModelOptions(select) {
    models2D.forEach(function(name) {
        var option = document.createElement("option");
        option.value = name;
        option.textContent = getModelDisplayName(name);
        select.appendChild(option);
    });
}

async function reloadModels(selectName) {
    try {
        var r = await fetch("/api/model/2d/list");
        var j = await r.json();
        var sel = document.getElementById("model-select");
        sel.innerHTML = "";
        if (j.success && j.data && j.data.length) {
            models2D = j.data;
            appendModelOptions(sel);
            var target = selectName && models2D.indexOf(selectName) >= 0 ? selectName : models2D[0];
            sel.value = target;
            await switchModel(target);
        } else { sel.innerHTML = "<option value=\"\">暂无可用角色</option>"; log("暂无可用角色"); }
    } catch(e) { log("角色列表加载失败: "+e.message); }
}

/*
 * The built-in Miku is a project-authored cutout rig. It uses the same stage
 * contract as Cubism models, while keeping the existing Cubism path unchanged
 * for uploaded and bundled Live2D models.
 */
async function switchModel(name) {
    if (!name) return;
    log("正在加载角色...");
    isResizing = false;
    var resizeUi = document.getElementById("model-resize-ui");
    if (resizeUi) resizeUi.classList.remove("is-resizing");
    document.body.classList.add("model-loading");
    var stageLegend = document.getElementById("stage-legend");
    if (stageLegend) stageLegend.classList.add("is-loading");
    if (currentModel) {
        try {
            app.stage.removeChild(currentModel);
            if (currentModel._mikuTick) {
                app.ticker.remove(currentModel._mikuTick);
                currentModel.destroy({ children: true });
            } else {
                currentModel.destroy();
            }
        } catch(e) {}
        currentModel = null;
    }
    updateModelResizeUi();
    currentModelName = name;
    updateStageModelLabel();

    var url = "/live2d-models/" + name + "/" + name + ".model3.json";

    try {
        currentModel = name === "hatsune_miku"
            ? await createHatsuneMikuModel()
            : await PIXI.live2d.Live2DModel.from(url, { autoInteract: true });
        modelScaleFactor = readModelScaleFactor(name);
        currentModel.scale.set(getModelBaseScale(name) * modelScaleFactor);
        updateModelScaleControls();
        if (currentModel.anchor) currentModel.anchor.set(0.5, 0.5);
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
        document.body.classList.add("model-ready");
        document.body.classList.remove("model-loading");
        if (stageLegend) stageLegend.classList.remove("is-loading");
        updateStageModelLabel();
        log("角色已就绪");
        playUiAnimation(document.getElementById("live2d-canvas"), [
            { opacity: 0.18 },
            { opacity: 1 }
        ], { duration: 620 });
        playUiAnimation(document.getElementById("stage-legend"), [
            { opacity: 0.35, transform: "translateY(-4px)" },
            { opacity: 1, transform: "translateY(0)" }
        ], { duration: 360 });
        playUiAnimation(document.getElementById("char-glow"), [
            { opacity: 0.26, transform: "translate(-50%, -50%) scale(0.88)" },
            { opacity: 0.84, transform: "translate(-50%, -50%) scale(1)" }
        ], { duration: 720 });

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
    } catch(e) {
        document.body.classList.remove("model-loading");
        if (stageLegend) stageLegend.classList.remove("is-loading");
        log("角色加载失败: " + e.message);
    }
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
    updateModelResizeUi();
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
    log("正在上传角色包...");
    toast("正在上传 " + f.name + "（" + (f.size / 1024 / 1024).toFixed(1) + " MB）");
    uploadWithRetry("/api/model/upload", fd, 2).then(function(j) {
        if (j.success) {
            var name = j.data.fileName;
            toast("角色上传成功: " + name);
            log("角色上传完成: " + name);
            return reloadModels(name);
        } else {
            var msg = j.message || "Unknown error";
            log("角色上传失败: " + msg);
            toast("角色上传失败: " + msg);
        }
    }).catch(function(e) {
        if (e.name === "AbortError") {
            log("上传超时，请检查文件大小");
            toast("上传超时，请检查文件大小");
        } else {
            log("上传失败: " + (e.message || "网络不可用"));
            toast("上传失败: " + (e.message || "网络不可用"));
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
        "hatsune_miku": "初音未来"
    };
    return nameMap[currentModelName] || currentModelName || "看板娘";
}

function getModelDisplayName(name) {
    var nameMap = {
        "haru_ja": "Haru",
        "hiyori_en": "Hiyori",
        "hatsune_miku": "初音未来"
    };
    return nameMap[name] || name;
}

function updateStageModelLabel() {
    var label = document.getElementById("stage-model-name");
    if (!label) return;
    label.textContent = getModelName();
    playUiAnimation(label, [
        { opacity: 0.25, transform: "translateY(5px)" },
        { opacity: 1, transform: "translateY(0)" }
    ], { duration: 240 });
}

// --- Chat Window ---
var chatWindowOpen = false;
var chatHistory = [];

function toggleChatWindow() {
    var win = document.getElementById("chat-window");
    var btn = document.getElementById("chat-window-toggle");
    chatWindowOpen = !chatWindowOpen;
    playUiAnimation(btn, [
        { transform: "scale(0.92)" },
        { transform: "scale(1)" }
    ], { duration: 260 });
    if (chatWindowOpen) {
        win.classList.remove("hidden");
        btn.classList.add("active");
        btn.textContent = "CLOSE";
        btn.title = "关闭对话";
        btn.setAttribute("aria-label", "关闭对话");
        scrollChatToBottom();
        setTimeout(function() {
            var input = document.getElementById("chat-win-input-text");
            if (input) input.focus();
        }, 240);
    } else {
        win.classList.add("hidden");
        btn.classList.remove("active");
        btn.textContent = "CHAT";
        btn.title = "打开对话";
        btn.setAttribute("aria-label", "打开对话");
    }
}

function scrollChatToBottom() {
    var el = document.getElementById("chat-messages");
    setTimeout(function() { el.scrollTop = el.scrollHeight; }, 50);
}

function addChatMessage(role, text) {
    var el = document.createElement("div");
    el.className = "msg " + role;
    if (text) el.textContent = text;
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

// --- Chat Window Send ---
async function sendFromChatWindow() {
    if (isAiThinking) return;
    var input = document.getElementById("chat-win-input-text");
    var m = input.value.trim();
    if (!m) return;
    input.value = "";

    isAiThinking = true;
    setChatBusy(true);
    showBubble("思考中...");
    try {
        addDanmaku("user", m);
        addChatMessage("user", m);

        var reply = await doAiChat(m);
        if (reply) {
            addDanmaku("ai", reply);
            addChatMessage("ai", reply);
        }
    } finally {
        isAiThinking = false;
        setChatBusy(false);
        lastInteraction = Date.now();
        startIdle();
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
        if (json.success && json.data && json.data.reply) {
            reply = json.data.reply;
            if (json.data.motion && currentModel) try { currentModel.motion(json.data.motion); } catch(e) {}
        } else {
            reply = json.message || "Agent 服务未连接，请检查模型 API 配置。";
        }
    } catch(e) {
        reply = "Agent 服务连接失败，请检查模型 API 配置。";
    }

    removeElement(thinkEl);

    if (!reply) reply = "Agent 服务未返回有效内容，请稍后重试。";

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
    if (isAiThinking) return;
    var m = document.getElementById("chat-input").value.trim(); if (!m) return;
    document.getElementById("chat-input").value = "";
    isAiThinking = true;
    setChatBusy(true);
    if (idleTimer) { clearTimeout(idleTimer); idleTimer = null; }
    showBubble("思考中...");
    if (currentModel) try { currentModel.motion("Tap"); } catch(e) {}

    // 在聊天窗口中也显示用户消息
    addChatMessage("user", m);

    var reply = null;
    try {
        var resp = await fetch("/api/live2d/chat/1", {
            method: "POST", headers: {"Content-Type":"application/json"},
            body: JSON.stringify({message: m, history: chatHistory.slice(-20)})
        });
        var json = await resp.json();
        if (json.success && json.data && json.data.reply) {
            reply = json.data.reply;
            if (json.data.motion) try { currentModel.motion(json.data.motion); } catch(e) {}
        } else {
            reply = json.message || "Agent 服务未连接，请检查模型 API 配置。";
        }
    } catch(e) {
        reply = "Agent 服务连接失败，请检查模型 API 配置。";
    }

    isAiThinking = false;
    if (!reply) reply = "Agent 服务未返回有效内容，请稍后重试。";
    addDanmaku("ai", reply);
    addChatMessage("ai", reply);

    chatHistory.push({role: "user", content: m});
    chatHistory.push({role: "assistant", content: reply});

    if (currentModel) try { currentModel.motion("Tap"); } catch(e) {}
    lastInteraction = Date.now();

    // Chat increases affection and EXP
    var affectionGain = Math.max(1, Math.floor(m.length / 5));
    affection += affectionGain;
    showFloatText("羁绊 +" + affectionGain, "var(--ds-accent-strong)");
    saveAffection();

    var expGain = 3 + Math.floor(m.length / 10);
    gainExp(expGain);

    startIdle();
    setChatBusy(false);
}

document.addEventListener("DOMContentLoaded", init);
