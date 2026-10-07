/* NinjaVu TV + keyboard spatial navigation.
   Injected into every page. Arrows / D-pad move a focus ring.
   Enter / OK activates. Media keys drive the player. */
(function () {
  if (window.NinjaVuNav && window.NinjaVuNav.ready) return;

  var RING = "nv-focus-ring";
  var HINT = "nv-remote-hint";
  var current = null;
  var lastRect = null;
  var playerMode = false;

  function css(el, text) {
    var s = document.createElement("style");
    s.id = el;
    s.textContent = text;
    (document.head || document.documentElement).appendChild(s);
  }

  css("nv-nav-style",
    "#" + RING + "{position:fixed;z-index:2147483646;pointer-events:none;border:3px solid #0F9F6E;" +
    "border-radius:16px;box-shadow:0 0 0 5px rgba(15,159,110,.28),0 10px 28px rgba(16,34,26,.28);" +
    "transition:left .12s ease,top .12s ease,width .12s ease,height .12s ease,opacity .12s ease;opacity:0}" +
    "#" + HINT + "{position:fixed;left:50%;bottom:28px;transform:translateX(-50%);z-index:2147483645;" +
    "background:#10221A;color:#F7F4EC;font:600 13px/1.2 sans-serif;padding:10px 16px;border-radius:999px;" +
    "box-shadow:0 8px 24px rgba(16,34,26,.28);pointer-events:none;opacity:0;transition:opacity .3s}" +
    "video::-webkit-media-controls{z-index:2}"
  );

  function ring() {
    var r = document.getElementById(RING);
    if (!r) {
      r = document.createElement("div");
      r.id = RING;
      document.documentElement.appendChild(r);
    }
    return r;
  }

  function hint(text) {
    var h = document.getElementById(HINT);
    if (!h) {
      h = document.createElement("div");
      h.id = HINT;
      document.documentElement.appendChild(h);
    }
    h.textContent = text;
    h.style.opacity = "1";
    clearTimeout(hint._t);
    hint._t = setTimeout(function () { h.style.opacity = "0"; }, 2200);
  }

  function visible(el) {
    if (!el || el.nodeType !== 1) return false;
    if (el.id === RING || el.id === HINT) return false;
    var st = window.getComputedStyle(el);
    if (st.display === "none" || st.visibility === "hidden" || parseFloat(st.opacity) === 0) return false;
    var b = el.getBoundingClientRect();
    if (b.width < 16 || b.height < 16) return false;
    if (b.bottom < 4 || b.top > window.innerHeight - 4) return false;
    if (b.right < 4 || b.left > window.innerWidth - 4) return false;
    return true;
  }

  function clickable(el) {
    if (!el || el.nodeType !== 1) return false;
    var tag = el.tagName;
    if (tag === "A" || tag === "BUTTON" || tag === "INPUT" || tag === "SELECT" || tag === "TEXTAREA" || tag === "VIDEO" || tag === "SUMMARY") return true;
    var role = (el.getAttribute("role") || "").toLowerCase();
    if (role === "button" || role === "link" || role === "tab" || role === "menuitem" || role === "option" || role === "checkbox" || role === "switch") return true;
    if (el.hasAttribute("onclick") || el.getAttribute("tabindex") === "0") return true;
    var st = window.getComputedStyle(el);
    if (st.cursor === "pointer" && (el.querySelector("img, picture, video") || el.innerText.trim().length > 0)) return true;
    return false;
  }

  function candidates() {
    var nodes = document.querySelectorAll("a,button,input,select,textarea,summary,video,[role='button'],[role='link'],[role='tab'],[role='menuitem'],[role='option'],[onclick],[tabindex='0']");
    var out = [];
    var seen = [];
    for (var i = 0; i < nodes.length; i++) {
      var el = nodes[i];
      if (!clickable(el) || !visible(el)) continue;
      var b = el.getBoundingClientRect();
      var nested = false;
      for (var j = 0; j < seen.length; j++) {
        if (seen[j].contains(el) && Math.abs(seen[j].getBoundingClientRect().width - b.width) < 8) {
          nested = true;
          break;
        }
      }
      if (nested) continue;
      seen.push(el);
      out.push(el);
    }
    if (out.length < 4) {
      var extras = document.querySelectorAll("div,li,article,section");
      for (var k = 0; k < extras.length && out.length < 80; k++) {
        var ex = extras[k];
        if (!visible(ex)) continue;
        var r = ex.getBoundingClientRect();
        if (r.width < 90 || r.height < 70 || r.width > window.innerWidth * 0.92) continue;
        if (!ex.querySelector("img, picture") && (ex.innerText || "").trim().length < 2) continue;
        if (window.getComputedStyle(ex).cursor !== "pointer" && !ex.querySelector("img")) continue;
        out.push(ex);
      }
    }
    return out;
  }

  function center(el) {
    var b = el.getBoundingClientRect();
    return { x: b.left + b.width / 2, y: b.top + b.height / 2, b: b };
  }

  function paint(el) {
    current = el;
    var r = ring();
    if (!el) {
      r.style.opacity = "0";
      return;
    }
    var b = el.getBoundingClientRect();
    lastRect = b;
    var pad = 6;
    r.style.left = (b.left - pad) + "px";
    r.style.top = (b.top - pad) + "px";
    r.style.width = (b.width + pad * 2) + "px";
    r.style.height = (b.height + pad * 2) + "px";
    r.style.opacity = "1";
    try { el.scrollIntoView({ block: "nearest", inline: "nearest", behavior: "smooth" }); } catch (e) {
      el.scrollIntoView(false);
    }
    var typing = isTyping(el);
    if (window.NinjaVuKeys && NinjaVuKeys.setTyping) NinjaVuKeys.setTyping(typing);
  }

  function isTyping(el) {
    if (!el) return false;
    var tag = el.tagName;
    if (tag === "TEXTAREA" || tag === "SELECT") return true;
    if (tag === "INPUT") {
      var t = (el.type || "text").toLowerCase();
      return t !== "button" && t !== "submit" && t !== "checkbox" && t !== "radio" && t !== "range" && t !== "file";
    }
    return el.isContentEditable;
  }

  function focusEdge(which) {
    var list = candidates();
    if (!list.length) return "empty";
    var best = list[0];
    var bestScore = which === "bottom" ? -1 : 1e9;
    for (var i = 0; i < list.length; i++) {
      var y = center(list[i]).y;
      if (which === "bottom") {
        if (y > bestScore) { bestScore = y; best = list[i]; }
      } else if (y < bestScore) { bestScore = y; best = list[i]; }
    }
    paint(best);
    hint("Arrows move  \u00b7  OK selects  \u00b7  Back returns");
    return "ok";
  }

  function ensure() {
    if (current && visible(current)) {
      paint(current);
      return current;
    }
    return null;
  }

  function move(dir) {
    var list = candidates();
    if (!list.length) return "empty";
    var fromEl = ensure();
    if (!fromEl) return focusEdge(dir === "up" ? "bottom" : "top");
    var from = center(fromEl);
    var best = null;
    var bestScore = 1e12;
    for (var i = 0; i < list.length; i++) {
      var el = list[i];
      if (el === fromEl) continue;
      var c = center(el);
      var dx = c.x - from.x;
      var dy = c.y - from.y;
      var primary, secondary, ok = false;
      if (dir === "left") { ok = dx < -12; primary = -dx; secondary = Math.abs(dy); }
      else if (dir === "right") { ok = dx > 12; primary = dx; secondary = Math.abs(dy); }
      else if (dir === "up") { ok = dy < -12; primary = -dy; secondary = Math.abs(dx); }
      else if (dir === "down") { ok = dy > 12; primary = dy; secondary = Math.abs(dx); }
      if (!ok) continue;
      if (secondary > primary * 1.8 + 70) continue;
      var score = primary + secondary * 2.4;
      if (score < bestScore) { bestScore = score; best = el; }
    }
    if (!best) {
      var scroller = scrollParent(fromEl);
      var before = scroller ? scroller.scrollTop : window.scrollY;
      var delta = (dir === "down" || dir === "right") ? Math.round(window.innerHeight * 0.45) : -Math.round(window.innerHeight * 0.45);
      if (dir === "left" || dir === "right") delta = (dir === "right" ? 1 : -1) * Math.round(window.innerWidth * 0.4);
      if (scroller && (dir === "up" || dir === "down")) scroller.scrollBy(0, delta);
      else window.scrollBy(dir === "left" || dir === "right" ? delta : 0, dir === "up" || dir === "down" ? delta : 0);
      var after = scroller ? scroller.scrollTop : window.scrollY;
      if (after !== before) {
        setTimeout(function () { move(dir); }, 180);
        return "scrolled";
      }
      return "edge";
    }
    paint(best);
    return "moved";
  }

  function scrollParent(el) {
    var n = el.parentElement;
    while (n && n !== document.body) {
      var st = window.getComputedStyle(n);
      if (/(auto|scroll)/.test(st.overflowY) && n.scrollHeight > n.clientHeight + 20) return n;
      n = n.parentElement;
    }
    return null;
  }

  function activate() {
    var el = ensure() || candidates()[0];
    if (!el) return "empty";
    paint(el);
    if (isTyping(el)) {
      el.focus();
      if (window.NinjaVuKeys && NinjaVuKeys.setTyping) NinjaVuKeys.setTyping(true);
      hint("Type to search  \u00b7  Back leaves the box");
      return "typing";
    }
    if (el.tagName === "VIDEO") {
      if (el.paused) el.play(); else el.pause();
      hint(el.paused ? "Paused" : "Playing");
      return "video";
    }
    try { el.focus({ preventScroll: true }); } catch (e) { try { el.focus(); } catch (e2) {} }
    el.click();
    hint("Selected");
    return "activated";
  }

  function videoEl() {
    var vids = document.querySelectorAll("video");
    var best = null;
    var area = 0;
    for (var i = 0; i < vids.length; i++) {
      var b = vids[i].getBoundingClientRect();
      var a = b.width * b.height;
      if (a > area) { area = a; best = vids[i]; }
    }
    return best;
  }

  function player(action) {
    var v = videoEl();
    if (!v) return "none";
    if (action === "toggle") {
      if (v.paused) v.play(); else v.pause();
    } else if (action === "play") v.play();
    else if (action === "pause") v.pause();
    else if (action === "seek+") v.currentTime = Math.min((v.duration || v.currentTime + 10), v.currentTime + 10);
    else if (action === "seek-") v.currentTime = Math.max(0, v.currentTime - 10);
    else if (action === "seek++") v.currentTime = Math.min((v.duration || v.currentTime + 30), v.currentTime + 30);
    else if (action === "seek--") v.currentTime = Math.max(0, v.currentTime - 30);
    var sec = Math.floor(v.currentTime % 60); hint(v.paused ? "Paused" : "Playing  " + Math.floor(v.currentTime / 60) + ":" + (sec < 10 ? "0" : "") + sec);
    return "ok";
  }

  function closeOverlay() {
    var closer = document.querySelector("[aria-label='Close'], [aria-label='close'], button.close, .modal-close, .popup-close");
    if (closer && visible(closer)) { closer.click(); return true; }
    if (document.fullscreenElement && document.exitFullscreen) {
      document.exitFullscreen();
      return true;
    }
    var v = videoEl();
    if (v && v.webkitDisplayingFullscreen && v.webkitExitFullscreen) {
      v.webkitExitFullscreen();
      return true;
    }
    return false;
  }

  function handle(action) {
    if (action === "left" || action === "right" || action === "up" || action === "down") {
      if (isTyping(document.activeElement) && document.activeElement !== current) return "typing";
      var result = move(action);
      if (result === "edge" && window.NinjaVuKeys && NinjaVuKeys.onEdge) NinjaVuKeys.onEdge(action);
      return result;
    }
    if (action === "ok") return activate();
    if (action === "back") return closeOverlay() ? "closed" : "native";
    if (action.indexOf("media:") === 0) return player(action.slice(6));
    return "noop";
  }

  window.NinjaVuNav = {
    ready: true,
    handle: handle,
    move: move,
    activate: activate,
    focusEdge: focusEdge,
    player: player,
    closeOverlay: closeOverlay,
    repaint: function () { if (current) paint(current); }
  };

  window.addEventListener("scroll", function () { if (current) paint(current); }, true);
  window.addEventListener("resize", function () { if (current) paint(current); });
  document.addEventListener("focusin", function (e) {
    if (isTyping(e.target) && window.NinjaVuKeys && NinjaVuKeys.setTyping) NinjaVuKeys.setTyping(true);
  });
  document.addEventListener("focusout", function () {
    setTimeout(function () {
      if (!isTyping(document.activeElement) && window.NinjaVuKeys && NinjaVuKeys.setTyping) NinjaVuKeys.setTyping(false);
    }, 60);
  });

  setTimeout(function () { focusEdge("top"); }, 350);
})();
