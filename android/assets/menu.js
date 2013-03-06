(function () {
  "use strict";
  var MENU_WIDTH = 180;

  function log () {
    return;
    var l = "";
    for(var i = 0; i < arguments.length; i++) {
      var a = arguments[i];
      if(typeof a === 'object') {
        l += JSON.stringify(a) + ', ';
      } else {
        l += a + ', ';
      }
    }
    console.log(l);
  }


  var $container = $('#container'),
      footer = $('#footer')[0],
      top_nav = $('#top-nav')[0],
      container = $container[0],
      $body = $('html body'),
      body = $body[0];

  var startX, startY, moveX, moveY, distanceX = 0,
      matrix, m41 = 0,
      shouldOpenMenu = false, shouldCloseMenu = false,
      menuOpen = false, menuOpening = false, menuClosing = false,
      startTime = 0;

  var transitionEnd = whichTransitionEvent();
  var in_progress = false;



  // remove animation time
  $container.bind(transitionEnd, function () { $body.removeClass('menu-opening'); });
  $body.delegate('.menu-button', 'click', function () {
    if(menuOpen) { closeMenu(); } else { openMenu(); }
  });

  $container.bind('touchstart', function (event) {
    startX = event.touches[0].pageX;
    startY = event.touches[0].pageY;
    startTime = new Date();
    in_progress = false;
    shouldOpenMenu = false;
    shouldCloseMenu = false;
    matrix = new WebKitCSSMatrix(container.style.webkitTransform);
    m41 = matrix['m41'];
    log("start", startX, startY);
  }).bind('touchmove', function (event) {
    moveX      = event.changedTouches[0].pageX;
    moveY      = event.changedTouches[0].pageY;
    distanceX  = moveX - startX;
    var distanceY  = moveY - startY,
        movedLeft  = distanceX < 0,
        movedRight = distanceX > 5;
    log('x=' + moveX, 'y=' + moveY);
    if(!in_progress) {
      if(Math.abs(distanceX) > 16 && // first move
         Math.abs(distanceY) * 1.4 < Math.abs(distanceX) ) {
        in_progress = true;
        log('move start');
      }
    }
    if(in_progress && Math.abs(distanceY) < 50) { // 50 is too many
      event.preventDefault();   // othewise, touchcancel
      in_progress = true;

      var dx = Math.max(0, Math.min(distanceX + m41, MENU_WIDTH));
      applyTransform(dx);
      log('dx=' + dx);
      // applyTransform('translate3d(' + dx + 'px, 0 , 0)');

      if(!menuOpen) {
        shouldOpenMenu = dx > MENU_WIDTH / 2;
      }
      if(menuOpen) {
        shouldCloseMenu = dx < MENU_WIDTH / 2;
      }
    } else {
      in_progress = false;
    }
  }).bind('touchend', function (event) {
    handle_cancel_end();
    log('end');
  }).bind('touchcancel', function (event) {
    handle_cancel_end();
    log('cancel');
  });

  function applyTransform (left) {
    // 'translate3d(200px, 0 , 0)'
    if(left) {
      var p = 'translate3d(' + left + 'px, 0 , 0)';
      top_nav.style.left = left + 'px';
      // $('#top-nav').css({left: left});
      // $container.css({overflow: 'hidden', height: window.innerHeight});
      // container.style.left = left + 'px';
      // $('#top-nav')[0].style.webkitTransform = p;
      container.style.webkitTransform = p;

      // $('#top-nav').css({top: body.scrollTop, display: 'absolute'});

    } else {
      container.style.removeProperty('-webkit-transform');
      top_nav.style.left = 0 + 'px';
      // $('#top-nav')[0].style.removeProperty('-webkit-transform');
      // container.style.removeProperty('left');
      // $('#top-nav').css({left: 0});

      // footer.style.removeProperty('-webkit-transform');
      // header.style.removeProperty('-webkit-transform');
    }
  }

  function handle_cancel_end () {
    var ts = new Date() - startTime;
    log("ts="+ ts, "x=" + distanceX, 'open=' + menuOpen);
    if(in_progress && ts < 145) { // enough distance in a short time
      if(distanceX < -15 && menuOpen) {
        shouldCloseMenu = true;
      }
      if(distanceX > 15 && !menuOpen) {
        shouldOpenMenu = true;
      }
    }

    if(menuOpen) {
      if(shouldCloseMenu) { closeMenu(); }
      else { openMenu(); }
    } else {
      if(shouldOpenMenu) { openMenu(); }
      else { closeMenu(); }
    }
  }

  function openMenu () {
    menuOpen = true;
    // $body.addClass('menu-opening');
    $body.addClass('menu-open');
    applyTransform(MENU_WIDTH);

    var html = "<ul>";
    for(var word in localStorage) {
      var times = JSON.parse(localStorage.getItem(word)).length;
      html += "<li>" + word + '(' + times + ")</li>";
    }
    html += '</ul>';

    $('#menu-panel').html(html);
  }

  function closeMenu () {
    menuOpen = false;
    // $('#top-nav').css({top: '0', display: 'fixed'});
    // $container.css({overflow: 'auto', height: 'auto'});

    // $body.addClass('menu-opening');
    $body.removeClass('menu-open');
    applyTransform();
  }

  function whichTransitionEvent() {
    var t, el = document.createElement('fakeelement');
    var transitions = {
      'transition':'transitionEnd',
      'OTransition':'oTransitionEnd',
      'MSTransition':'msTransitionEnd',
      'MozTransition':'transitionend',
      'WebkitTransition':'webkitTransitionEnd'
    };

    for (t in transitions) {
      if( el.style[t] !== undefined ) {
        return transitions[t];
      }
    }
  }
  window.close_menu = closeMenu;
})();
