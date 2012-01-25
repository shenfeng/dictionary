(function () {
  var eventSplitter = /^(\S+)\s*(.*)$/;

  var $q = $("#q"),
      $ac = $("#ac"),
      to_html = Mustache.to_html,
      max_candiates = 25,
      all_words,
      tmpls = window.D.tmpls,
      trigger_ac = true,
      old_q;

  function delegateEvents($ele, events) {
    for (var key in events) {
      var method = events[key],
          match = key.match(eventSplitter),
          eventName = match[1],
          selector = match[2];
      if (selector === '') {
        $ele.bind(eventName, method);
      } else {
        $ele.delegate(selector, eventName, method);
      }
    }
  }

  function show_nearby_words (word, force_refresh) {
    var result = [];
    var i = 0;
    for(; i < all_words.length; i++) {
      if(word === all_words[i]) {
        break;
      }
    }
    if(i > max_candiates / 2) { i -= Math.round(max_candiates / 2); }
    for(; i < all_words.length; i++) {
      if(result.push(all_words[i]) > max_candiates) {
        break;
      }
    }
    show_candidates(result, word, force_refresh);
  }

  delegateEvents($ac, {
    'click li': function (e) {
      $("li.selected").removeClass('selected');
      var $this = $(this);
      $this.addClass('selected');
      show_search_result($this.text().trim());
    }
  });

  function show_candidates (candidats, select, force_refresh) {
    if(select && !force_refresh) {
      var already_show = false;
      $("#ac li").each(function (index, li) {
        if($(li).text().trim() === select) {
          already_show = true;
          $("li.selected").removeClass('selected');
          $(li).addClass('selected');
        }
      });
      if(already_show) { return; }
    }
    if(candidats.length) {
      var html = Mustache.to_html(tmpls.ac, {words: candidats});
      $ac.empty().append(html);
      if(!select) {
        $("#ac li:first").addClass('selected');
      } else {
        $("#ac li").each(function (index, li) {
          if($(li).text().trim() === select) {
            $(li).addClass('selected');
          }
        });
      }
    }
  }

  function show_empty_query_candidates () {
    var result = [];
    for(var i = 0; i < all_words.length; i++) {
      var word = all_words[i],
          c = word.charAt(0);
      if(c !== '-' && c !== '\'' && word.length > 3 && (!+c)) {
        if(result.push(word) > max_candiates) {
          break;
        }
      }
    }
    show_candidates(result);
  }

  $.get('/allwords', function (data) {
    all_words = data.split("\n");
    show_empty_query_candidates();
  });

  $q.focus();

  function show_search_result (q, force_refresh) {
    $.getJSON("/d/" + q, function (data) {
      show_nearby_words(q, force_refresh);
      $q.val(q);
      if(!_.isArray(data)) { data = [data]; }
      var html = to_html(tmpls.explain, {
        items: data,
        word: q
      });
      $("#explain").empty().append(html);
    });
  }

  $(document).keydown(function (e) {
    var which = e.which;
    $selected = $("li.selected");
    switch(which) {
    case 27:                    // ESC
      // $ac.empty();
      break;
    case 13:                    // ENTER
      var q = $selected.text().trim() || $q.val().trim();
      // $ac.empty();
      show_search_result(q, true);
      break;
    case 40:                    // DOWN
      var $next = $selected.next();
      if($selected.length && $next.length) {
        $selected.removeClass('selected');
        $next.addClass('selected');
        show_search_result($next.text().trim());
      }
      break;
    case 38:                    // UP
      var $prev = $selected.prev();
      if($selected.length && $prev) {
        $selected.removeClass('selected');
        $prev.addClass('selected');
        show_search_result($prev.text().trim());
      }
      break;
    }
  });

  function auto_complete () {
    var q = $q.val().trim().toLowerCase();
    if(q && q !== old_q) {
      old_q = q;
      var result = [];
      for(var i = 0; i < all_words.length; i++) {
        var word = all_words[i];
        if(word.indexOf(q) === 0) {
          if(result.push(word) > max_candiates) {
            break;
          }
        }
      }
      show_candidates(result);
    }
  }

  $q.keydown(function (e) {
    if(e.which !== 27 && e.which !== 40 && e.which !== 38 && e.which !== 13) {
      setTimeout(auto_complete, 1);
    }
  });

})();
