// SekaiForm 3D Character Viewer - Pose Mirror
var scene, camera, renderer, controls, model, mixer, clock = new THREE.Clock();
var modelList = [], currentModelName = '';
var blends = {};
var totalTime = 0;
var blinkTimer = 0, blinkInterval = 3, isBlinking = false, blinkProgress = 0;

// Pose mirroring
var mirrorEnabled = false;
var poseLandmarks = null;
var poseReady = false;
var boneMap = {};
var restRotations = {};
var videoElement = null;
var camPreviewCtx = null;
var smoothingFactor = 0.55;
var targetRotations = {};

function setStatus(txt, sub) {
    document.getElementById("status").style.display = "";
    document.querySelector('#status .txt').textContent = txt;
    document.getElementById('subStatus').textContent = sub || '';
}
function toast(msg, type) {
    var t = document.createElement('div');
    t.className = 'tst tst-' + (type === 'err' ? 'err' : type === 'ok' ? 'ok' : 'info');
    t.textContent = msg;
    document.getElementById('toasts').appendChild(t);
    setTimeout(function() { t.remove(); }, 2200);
}

// ---------- Bone Utilities ----------

function findBones(model) {
    boneMap = {};
    restRotations = {};
    targetRotations = {};
    model.traverse(function(obj) {
        if (obj.isBone) {
            boneMap[obj.name] = obj;
            restRotations[obj.name] = {
                x: obj.rotation.x,
                y: obj.rotation.y,
                z: obj.rotation.z
            };
            targetRotations[obj.name] = {
                x: obj.rotation.x,
                y: obj.rotation.y,
                z: obj.rotation.z
            };
        }
    });
}

// ---------- MediaPipe Pose ----------

function initPoseTracking() {
    videoElement = document.getElementById('camVideo');
    var preview = document.getElementById('camPreview');
    camPreviewCtx = preview.getContext('2d');

    var PoseLib = window.Pose;
    if (!PoseLib) {
        console.error('MediaPipe Pose not loaded');
        toast('Pose load failed', 'err');
        return;
    }
    var pose = new PoseLib({
        locateFile: function(file) {
            return 'https://cdn.jsdelivr.net/npm/@mediapipe/pose@0.5.1675469404/' + file;
        }
    });
    pose.setOptions({
        modelComplexity: 1,
        smoothLandmarks: true,
        enableSegmentation: false,
        minDetectionConfidence: 0.5,
        minTrackingConfidence: 0.5
    });
    pose.onResults(function(results) {
        if (results.poseLandmarks) {
            poseLandmarks = results.poseLandmarks;
            poseReady = true;
            drawPosePreview(results);
        }
    });

    navigator.mediaDevices.getUserMedia({
        video: { width: 640, height: 480, facingMode: 'user' },
        audio: false
    }).then(function(stream) {
        videoElement.srcObject = stream;
        videoElement.play();
        processFrame(pose, videoElement);
    }).catch(function(err) {
        console.error('Webcam error:', err);
        toast('Camera failed: ' + err.message, 'err');
    });
}

function processFrame(pose, video) {
    if (!mirrorEnabled) {
        setTimeout(function() { processFrame(pose, video); }, 500);
        return;
    }
    if (video.readyState >= 2) {
        pose.send({image: video}).then(function() {
            setTimeout(function() { processFrame(pose, video); }, 1);
        }).catch(function() {
            setTimeout(function() { processFrame(pose, video); }, 100);
        });
    } else {
        setTimeout(function() { processFrame(pose, video); }, 100);
    }
}

function drawPosePreview(results) {
    var preview = document.getElementById('camPreview');
    if (preview.style.display === 'none') return;
    var w = preview.width, h = preview.height;
    if (!camPreviewCtx) return;
    camPreviewCtx.clearRect(0, 0, w, h);
    camPreviewCtx.fillStyle = 'rgba(0,0,0,0.3)';
    camPreviewCtx.fillRect(0, 0, w, h);
    if (!results.poseLandmarks) return;
    var keyIndices = [0, 7, 8, 11, 12]; // Head + shoulders only
    camPreviewCtx.fillStyle = '#44ff88';
    for (var i = 0; i < keyIndices.length; i++) {
        var lm = results.poseLandmarks[keyIndices[i]];
        var x = (1 - lm.x) * w;
        var y = lm.y * h;
        camPreviewCtx.beginPath();
        camPreviewCtx.arc(x, y, 4, 0, Math.PI * 2);
        camPreviewCtx.fill();
    }
    camPreviewCtx.strokeStyle = '#44ff88';
    camPreviewCtx.lineWidth = 2;
    var connections = [[11,12],[0,7],[0,8]]; // Shoulder line + head
    for (var j = 0; j < connections.length; j++) {
        var a = results.poseLandmarks[connections[j][0]];
        var b = results.poseLandmarks[connections[j][1]];
        camPreviewCtx.beginPath();
        camPreviewCtx.moveTo((1 - a.x) * w, a.y * h);
        camPreviewCtx.lineTo((1 - b.x) * w, b.y * h);
        camPreviewCtx.stroke();
    }
}

// ---------- Pose to Bone Mapping ----------

function updateBonesFromPose() {
    if (!poseReady || !poseLandmarks || Object.keys(boneMap).length === 0) return;
    var lm = poseLandmarks;
    var shoulderCX = (lm[11].x + lm[12].x) / 2;
    var shoulderCY = (lm[11].y + lm[12].y) / 2;
    // Upper body only: head + body lean, arms stay at rest
    if (boneMap['J_Bip_Head']) calcAndApplyHead(lm);
    if (boneMap['J_Bip_Spine']) calcAndApplySpine(lm, shoulderCX, shoulderCY);
    if (boneMap['J_Bip_Neck']) calcAndApplyNeck(lm);
}

function toWorldDir(a, b) {
    var dx = b.x - a.x;
    var dy = a.y - b.y;
    var dz = b.z - a.z;
    var len = Math.sqrt(dx*dx + dy*dy + dz*dz);
    if (len < 0.001) return null;
    return {x: dx/len, y: dy/len, z: dz/len};
}

function calcAndApplyArm(side, shoulder, elbow, wrist) {
    var upperBone = boneMap['J_Bip_' + side + '_UpperArm'];
    if (!upperBone) return;
    var upperDir = toWorldDir(shoulder, elbow);
    if (!upperDir) return;
    var swingForward = Math.asin(Math.max(-1, Math.min(1, upperDir.z))) * (180/Math.PI);
    var swingSide = Math.asin(Math.max(-1, Math.min(1, upperDir.x))) * (180/Math.PI);
    if (Math.abs(swingForward) < 8) swingForward = 0;
    if (Math.abs(swingSide) < 8) swingSide = 0;
    swingForward = Math.max(-90, Math.min(90, swingForward));
    swingSide = Math.max(-90, Math.min(90, swingSide));
    var rx = THREE.MathUtils.degToRad(swingForward);
    var rz = THREE.MathUtils.degToRad(side === 'L' ? swingSide : -swingSide);
    var rest = restRotations['J_Bip_' + side + '_UpperArm'];
    var tr = targetRotations['J_Bip_' + side + '_UpperArm'];
    tr.x = rest.x + rx;
    tr.z = rest.z + rz;
    tr.y = rest.y;
    var lowerBone = boneMap['J_Bip_' + side + '_LowerArm'];
    if (lowerBone) {
        var forearmDir = toWorldDir(elbow, wrist);
        if (forearmDir) {
            var dot = upperDir.x*forearmDir.x + upperDir.y*forearmDir.y + upperDir.z*forearmDir.z;
            dot = Math.max(-1, Math.min(1, dot));
            var bendAngle = Math.acos(dot) * (180/Math.PI);
            if (bendAngle < 10) bendAngle = 0;
            bendAngle = Math.max(0, Math.min(160, bendAngle));
            var restF = restRotations['J_Bip_' + side + '_LowerArm'];
            var trF = targetRotations['J_Bip_' + side + '_LowerArm'];
            trF.x = restF.x + THREE.MathUtils.degToRad(bendAngle);
            trF.y = restF.y;
            trF.z = restF.z;
        }
    }
}

function calcAndApplyHead(lm) {
    var nose = lm[0], leftEar = lm[7], rightEar = lm[8];
    if (!leftEar || !rightEar) return;
    var earDX = rightEar.x - leftEar.x;
    var earDY = rightEar.y - leftEar.y;
    var roll = Math.atan2(earDY, earDX) * (180/Math.PI);
    var earCX = (leftEar.x + rightEar.x) / 2;
    var yaw = (nose.x - earCX) * 180;
    var earCY = (leftEar.y + rightEar.y) / 2;
    var pitch = (earCY - nose.y) * 120;
    var rest = restRotations['J_Bip_Head'];
    var tr = targetRotations['J_Bip_Head'];
    tr.x = rest.x + THREE.MathUtils.degToRad(Math.max(-30, Math.min(30, pitch)));
    tr.y = rest.y + THREE.MathUtils.degToRad(Math.max(-45, Math.min(45, yaw)));
    tr.z = rest.z + THREE.MathUtils.degToRad(Math.max(-20, Math.min(20, roll)));
}

function calcAndApplySpine(lm, shoulderCX, shoulderCY) {
    var hipCX = (lm[23].x + lm[24].x) / 2;
    var hipCY = (lm[23].y + lm[24].y) / 2;
    var spineRX = (shoulderCY - hipCY) * -60;
    var spineRZ = (shoulderCX - hipCX) * 40;
    var rest = restRotations['J_Bip_Spine'];
    var tr = targetRotations['J_Bip_Spine'];
    tr.x = rest.x + THREE.MathUtils.degToRad(Math.max(-25, Math.min(25, spineRX)));
    tr.z = rest.z + THREE.MathUtils.degToRad(Math.max(-15, Math.min(15, spineRZ)));
    tr.y = rest.y;
}

function calcAndApplyNeck(lm) {
    var nose = lm[0];
    var midShoulder = {
        x: (lm[11].x + lm[12].x) / 2,
        y: (lm[11].y + lm[12].y) / 2
    };
    // Neck forward lean based on head position relative to shoulders
    var leanX = (nose.x - midShoulder.x) * 60;
    var leanY = (midShoulder.y - nose.y) * 40;
    var rest = restRotations['J_Bip_Neck'];
    var tr = targetRotations['J_Bip_Neck'];
    tr.x = rest.x + THREE.MathUtils.degToRad(Math.max(-20, Math.min(20, leanY)));
    tr.z = rest.z + THREE.MathUtils.degToRad(Math.max(-15, Math.min(15, leanX)));
    tr.y = rest.y;
}
function smoothBones(dt) {
    var f = Math.min(1, dt * (1 / Math.max(0.001, 1 - smoothingFactor)));
    for (var name in targetRotations) {
        if (!boneMap[name]) continue;
        var bone = boneMap[name];
        var t = targetRotations[name];
        bone.rotation.x += (t.x - bone.rotation.x) * f;
        bone.rotation.y += (t.y - bone.rotation.y) * f;
        bone.rotation.z += (t.z - bone.rotation.z) * f;
    }
}

function resetBonesToRest() {
    // Reset arms to natural hanging pose
    if (boneMap['J_Bip_L_UpperArm']) { boneMap['J_Bip_L_UpperArm'].rotation.z = 0.55; }
    if (boneMap['J_Bip_R_UpperArm']) { boneMap['J_Bip_R_UpperArm'].rotation.z = -0.55; }
    if (boneMap['J_Bip_L_LowerArm']) { boneMap['J_Bip_L_LowerArm'].rotation.set(0,0,0); }
    if (boneMap['J_Bip_R_LowerArm']) { boneMap['J_Bip_R_LowerArm'].rotation.set(0,0,0); }
    for (var name in restRotations) {
        if (!boneMap[name]) continue;
        var r = restRotations[name];
        boneMap[name].rotation.set(r.x, r.y, r.z);
        if (targetRotations[name]) {
            targetRotations[name].x = r.x;
            targetRotations[name].y = r.y;
            targetRotations[name].z = r.z;
        }
    }
}

// ---------- Mirror Toggle ----------

function toggleMirror() {
    mirrorEnabled = !mirrorEnabled;
    var btn = document.getElementById('mirrorToggle');
    var preview = document.getElementById('camPreview');
    if (mirrorEnabled) {
        btn.classList.add('active');
        btn.innerHTML = '<span class="dot"></span> Mirror ON';
        preview.style.display = 'block';
        if (!poseReady && Object.keys(boneMap).length > 0) {
            initPoseTracking();
        }
        poseLandmarks = null;
        poseReady = false;
        toast('Pose mirror: ON', 'ok');
    } else {
        btn.classList.remove('active');
        btn.innerHTML = '<span class="dot"></span> Mirror';
        preview.style.display = 'none';
        resetBonesToRest();
        toast('Pose mirror: OFF', 'info');
    }
}

// ---------- Existing Scene / Model Code ----------

function initScene() {
    scene = new THREE.Scene();
    var bgCanvas = document.createElement('canvas');
    bgCanvas.width = 2; bgCanvas.height = 512;
    var ctx = bgCanvas.getContext('2d');
    var grad = ctx.createLinearGradient(0, 0, 0, 512);
    grad.addColorStop(0, '#f2f4f7');
    grad.addColorStop(0.3, '#eaecf0');
    grad.addColorStop(0.6, '#dfe2e8');
    grad.addColorStop(1, '#d0d4db');
    ctx.fillStyle = grad; ctx.fillRect(0, 0, 2, 512);
    var bgTex = new THREE.CanvasTexture(bgCanvas);
    bgTex.minFilter = THREE.LinearFilter;
    scene.background = bgTex;
    scene.fog = new THREE.Fog(0xd4d9e0, 7, 28);

    camera = new THREE.PerspectiveCamera(35, innerWidth / innerHeight, 0.1, 100); // Tight upper body
    camera.position.set(0, 1.5, 1.8); // Chest-up tight frame

    renderer = new THREE.WebGLRenderer({antialias: true, alpha: false, powerPreference: 'high-performance'});
    renderer.setSize(innerWidth, innerHeight);
    renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.15;
    try { renderer.outputColorSpace = THREE.SRGBColorSpace; } catch(e) {}
    document.body.insertBefore(renderer.domElement, document.body.firstChild);

    controls = new THREE.OrbitControls(camera, renderer.domElement);
    controls.target.set(0, 1.42, 0); // Neck level
    controls.enableDamping = true; controls.dampingFactor = 0.08;
    controls.minDistance = 0.5; controls.maxDistance = 8;
    controls.maxPolarAngle = Math.PI * 0.72;
    controls.autoRotate = false;
    controls.update();

    var shGeo = new THREE.PlaneGeometry(4.5, 4.5);
    var shPlane = new THREE.Mesh(shGeo, new THREE.ShadowMaterial({opacity: 0.06}));
    shPlane.rotation.x = -Math.PI / 2; shPlane.position.y = 0.002;
    shPlane.receiveShadow = true; shPlane.renderOrder = 1;
    scene.add(shPlane);

    var key = new THREE.DirectionalLight(0xfffaf5, 6.5);
    key.position.set(2.5, 4, 3);
    key.castShadow = true;
    key.shadow.mapSize.set(2048, 2048);
    key.shadow.camera.near = 0.1; key.shadow.camera.far = 20;
    key.shadow.camera.left = -4; key.shadow.camera.right = 4;
    key.shadow.camera.top = 5; key.shadow.camera.bottom = -1;
    key.shadow.bias = -0.0002; key.shadow.normalBias = 0.02;
    scene.add(key);

    var fill = new THREE.DirectionalLight(0xe8f0ff, 2.8);
    fill.position.set(-2.5, 1.8, -1.5); scene.add(fill);
    var rim = new THREE.DirectionalLight(0xffffff, 3.5);
    rim.position.set(0, 1.8, -4); scene.add(rim);
    var bounce = new THREE.DirectionalLight(0xbcc8d8, 1.8);
    bounce.position.set(0, -0.5, 2.2); scene.add(bounce);
    scene.add(new THREE.AmbientLight(0x8899aa, 3.0));
}

function stripVRM(buffer) {
    try {
        var view = new DataView(buffer);
        if (view.getUint32(0, true) !== 0x46546C67) return buffer;
        var jsonLen = view.getUint32(12, true);
        var jsonStart = 20;
        if (jsonStart + jsonLen > buffer.byteLength) return buffer;
        var jsonBytes = new Uint8Array(buffer, jsonStart, jsonLen);
        var json = JSON.parse(new TextDecoder().decode(jsonBytes));
        var changed = false;
        if (json.extensionsUsed) {
            var filtered = json.extensionsUsed.filter(function(e) { return e !== 'VRM' && e !== 'VRMC_vrm'; });
            if (filtered.length !== json.extensionsUsed.length) { changed = true; json.extensionsUsed = filtered; }
            if (!json.extensionsUsed.length) delete json.extensionsUsed;
        }
        if (json.extensionsRequired) {
            var filtered = json.extensionsRequired.filter(function(e) { return e !== 'VRM' && e !== 'VRMC_vrm'; });
            if (filtered.length !== json.extensionsRequired.length) { changed = true; json.extensionsRequired = filtered; }
            if (!json.extensionsRequired.length) delete json.extensionsRequired;
        }
        if (json.extensions) {
            if (json.extensions.VRM) { delete json.extensions.VRM; changed = true; }
            if (json.extensions.VRMC_vrm) { delete json.extensions.VRMC_vrm; changed = true; }
            if (!Object.keys(json.extensions).length) delete json.extensions;
        }
        if (!changed) return buffer;
        var cleaned = JSON.stringify(json);
        if (cleaned.length < jsonLen) { while (cleaned.length < jsonLen) cleaned += ' '; }
        else { cleaned = cleaned.substring(0, jsonLen); }
        var out = new Uint8Array(buffer.byteLength);
        out.set(new Uint8Array(buffer, 0, jsonStart));
        out.set(new TextEncoder().encode(cleaned), jsonStart);
        var restStart = jsonStart + jsonLen;
        out.set(new Uint8Array(buffer, restStart, buffer.byteLength - restStart), restStart);
        return out.buffer;
    } catch(e) { return buffer; }
}

function scanBlends(root) {
    blends = {};
    root.traverse(function(obj) {
        if (obj.isMesh && obj.morphTargetDictionary) {
            var dict = obj.morphTargetDictionary;
            var infl = obj.morphTargetInfluences;
            if (!infl) return;
            for (var k in dict) {
                if (!blends[k]) blends[k] = [];
                blends[k].push({mesh: obj, index: dict[k], influences: infl});
            }
        }
    });
}

function setBlendWeight(name, value) {
    var entries = blends[name];
    if (!entries) return;
    for (var i = 0; i < entries.length; i++) {
        if (entries[i].influences) entries[i].influences[entries[i].index] = value;
    }
}

function applyNaturalPose(model) {
    model.traverse(function(obj) {
        if (!obj.isBone) return;
        var n = obj.name;
        if (n === 'J_Bip_L_UpperArm') { obj.rotation.z = 0.55; }
        if (n === 'J_Bip_R_UpperArm') { obj.rotation.z = -0.55; }
    });
}

function updateBlink(dt) {
    blinkTimer += dt;
    if (!isBlinking && blinkTimer > blinkInterval) {
        isBlinking = true; blinkProgress = 0; blinkTimer = 0;
        blinkInterval = 2.5 + Math.random() * 4.5;
    }
    if (isBlinking) {
        blinkProgress += dt * 10;
        if (blinkProgress > 1.5) {
            isBlinking = false;
            setBlendWeight('Blink', 0); setBlendWeight('Blink_L', 0); setBlendWeight('Blink_R', 0);
            return;
        }
        var w;
        if (blinkProgress < 0.15) w = blinkProgress / 0.15;
        else if (blinkProgress < 0.3) w = 1 - (blinkProgress - 0.15) / 0.15;
        else w = 1;
        w = Math.max(0, Math.min(1, w));
        if (blends['Blink_L']) setBlendWeight('Blink_L', w);
        else if (blends['Blink']) setBlendWeight('Blink', w);
        if (blends['Blink_R']) setBlendWeight('Blink_R', w);
    }
}

function loadModel(url, displayName) {
    setStatus('Loading...', displayName);
    currentModelName = displayName;

    fetch(url).then(function(r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        setStatus('Processing...', displayName);
        return r.arrayBuffer();
    }).then(function(buffer) {
        if (/\.vrm$/i.test(url)) {
            buffer = stripVRM(buffer);
        }
        if (model) {
            scene.remove(model);
            model.traverse(function(c) {
                if (c.geometry) c.geometry.dispose();
                if (c.material) {
                    if (Array.isArray(c.material)) c.material.forEach(function(m) { m.dispose(); });
                    else c.material.dispose();
                }
            });
            model = null;
        }
        if (mixer) { mixer.stopAllAction(); mixer = null; }
        boneMap = {};
        restRotations = {};
        targetRotations = {};

        var loader = new THREE.GLTFLoader();
        loader.parse(buffer, '', function(gltf) {
            model = gltf.scene;
            if (!model) { setStatus('Empty model', ''); return; }

            var box = new THREE.Box3().setFromObject(model);
            var size = box.getSize(new THREE.Vector3());
            var center = box.getCenter(new THREE.Vector3());
            var maxDim = Math.max(size.x, size.y, size.z);
            if (maxDim > 0.001) {
                var s = 2.2 / maxDim;
                model.scale.setScalar(s);
                model.position.set(-center.x * s, -center.y * s + 0.22, -center.z * s);
            }

            model.traverse(function(c) { if (c.isMesh) { c.castShadow = true; c.receiveShadow = true; } });
            scene.add(model);
            controls.target.set(0, 1.42, 0); // Neck level

            applyNaturalPose(model);
            scanBlends(model);
            findBones(model);

            if (gltf.animations && gltf.animations.length) {
                mixer = new THREE.AnimationMixer(model);
                mixer.clipAction(gltf.animations[0]).play();
            }

            // If mirror was already enabled, restart tracking with new bones
            if (mirrorEnabled && Object.keys(boneMap).length > 0) {
                poseLandmarks = null;
                poseReady = false;
                initPoseTracking();
            }

            blinkTimer = 0; blinkInterval = 3; isBlinking = false; blinkProgress = 0;
            document.getElementById('status').style.display = 'none';
            toast('Model: ' + displayName, 'ok');
        }, function(err) {
            setStatus('Parse failed', (err.message || 'Error').substring(0, 60));
            document.querySelector('#status .ring').style.display = 'none';
            toast('Parse failed', 'err');
        });
    }).catch(function(err) {
        setStatus('Load failed', err.message);
        document.querySelector('#status .ring').style.display = 'none';
        toast('Load failed', 'err');
    });
}

function animate() {
    requestAnimationFrame(animate);
    var dt = Math.min(clock.getDelta(), 0.1);
    totalTime += dt;

    if (mixer) mixer.update(dt);

    // Pose mirroring: update bones from webcam
    if (mirrorEnabled && poseReady && Object.keys(boneMap).length > 0) {
        updateBonesFromPose();
        smoothBones(dt);
    }

    controls.update();
    if (model) {
        model.position.y += (0.22 + Math.sin(totalTime * 1.4) * 0.006 - model.position.y) * 0.1;
        updateBlink(dt);
    }
    renderer.render(scene, camera);
}

window.addEventListener('resize', function() {
    camera.aspect = innerWidth / innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(innerWidth, innerHeight);
});

async function loadModelList(skipAutoLoad) {
    try {
        var r = await fetch('/api/model/list');
        var j = await r.json();
        if (j.success && j.data && j.data['3D'] && j.data['3D'].length > 0) {
            modelList = j.data['3D'];
            var sel = document.getElementById('modelSelect');
            sel.innerHTML = '<option value="">Select model...</option>';
            modelList.forEach(function(name) { sel.innerHTML += '<option value="' + name + '">' + name + '</option>'; });
            if (!skipAutoLoad) { sel.value = modelList[0]; loadModel('/models/3d/' + modelList[0], modelList[0]); }
        } else {
            setStatus('No models', 'Upload a VRM file');
            document.querySelector('#status .ring').style.display = 'none';
        }
    } catch(e) {
        setStatus('Connection error', 'Refresh to retry');
        document.querySelector('#status .ring').style.display = 'none';
    }
}

function deleteCurrentModel() {
    var sel = document.getElementById('modelSelect');
    var name = sel.value;
    if (!name) { toast('No model selected', 'err'); return; }
    if (!confirm('Delete model: ' + name + '?')) return;

    fetch('/api/model/delete?fileName=' + encodeURIComponent(name) + '&type=3D', { method: 'DELETE' })
        .then(function(r) { return r.json(); })
        .then(function(j) {
            if (j.success) {
                toast('Deleted: ' + name, 'ok');
                loadModelList();
            } else {
                toast('Delete failed: ' + (j.message || 'error'), 'err');
            }
        }).catch(function(e) {
            toast('Delete error', 'err');
        });
}
function onSelectModel() { var n = document.getElementById('modelSelect').value; if (n) loadModel('/models/3d/' + n, n); }

// ---------- Upload helpers with timeout & retry ----------
var UPLOAD_TIMEOUT_MS = 600000; // 10 minutes
var UPLOAD_MAX_RETRIES = 2;

function uploadWithRetry(url, formData, retriesLeft) {
    var ctrl = new AbortController();
    var timer = setTimeout(function() { ctrl.abort(); }, UPLOAD_TIMEOUT_MS);
    return fetch(url, { method: "POST", body: formData, signal: ctrl.signal }).then(function(r) {
        clearTimeout(timer);
        if (!r.ok) throw new Error("Server returned " + r.status + " " + r.statusText);
        return r.json();
    }).catch(function(e) {
        clearTimeout(timer);
        if (retriesLeft > 0 && (e.name === "AbortError" || e.message.indexOf("fetch") !== -1 || e.message.indexOf("network") !== -1 || e.message.indexOf("NetworkError") !== -1)) {
            console.warn("Upload retry, attempts left: " + retriesLeft + " - " + e.message);
            return uploadWithRetry(url, formData, retriesLeft - 1);
        }
        throw e;
    });
}

function onUpload2D(input) {
    var file = input.files[0]; if (!file) return;
    var fd = new FormData(); fd.append("file", file); fd.append("type", "2D");
    setStatus("Uploading 2D...", file.name);
    toast("Uploading " + file.name + " (" + (file.size / 1024 / 1024).toFixed(1) + " MB)...", "info");
    uploadWithRetry("/api/model/upload", fd, UPLOAD_MAX_RETRIES).then(function(j) {
        if (j.success) {
            toast("2D model uploaded: " + j.data.fileName, "ok");
            setStatus("Ready", "");
            document.querySelector("#status .ring").style.display = "none";
        } else {
            toast("Upload failed: " + (j.message || ""), "err");
            setStatus("Upload failed", j.message || "");
            document.querySelector("#status .ring").style.display = "none";
        }
    }).catch(function(e) {
        toast("Upload error: " + (e.message || "Network failure. Check server and retry."), "err");
        setStatus("Upload error", "Retry or check server");
        document.querySelector("#status .ring").style.display = "none";
    });
    input.value = "";
}
function onUploadFile(input) {
    var file = input.files[0]; if (!file) return;
    var fd = new FormData(); fd.append("file", file); fd.append("type", "3D");
    setStatus("Uploading 3D...", file.name + " (" + (file.size / 1024 / 1024).toFixed(1) + " MB)");
    uploadWithRetry("/api/model/upload", fd, UPLOAD_MAX_RETRIES).then(function(j) {
        if (j.success) {
            loadModelList(true).then(function() {
                var sel = document.getElementById("modelSelect");
                sel.value = j.data.fileName;
                loadModel("/models/3d/" + j.data.fileName, j.data.fileName);
                toast("Uploaded: " + j.data.fileName, "ok");
            });
        } else {
            setStatus("Upload failed", j.message || "");
            document.querySelector("#status .ring").style.display = "none";
        }
    }).catch(function(e) {
        setStatus("Upload error", "Network failure. Check server and retry.");
        document.querySelector("#status .ring").style.display = "none";
    });
    input.value = "";
}
document.addEventListener('DOMContentLoaded', function() {
    var preview = document.getElementById('camPreview');
    preview.width = 160;
    preview.height = 120;
});

initScene();
animate();
loadModelList();

