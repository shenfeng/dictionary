(function () {
  var eventSplitter = /^(\S+)\s*(.*)$/;

  var alphabet = "abcdefghijklmnopqrstuvwxyz";

  var $q = $("#q"),
      $ac = $("#ac"),
      to_html = Mustache.to_html,
      ajax_queue = [],
      max_candiates = 27,
      all_words = window._WORDS_,
      word_map = window._MAPS_,
      tmpls = window.D.tmpls,
      lookup_map = {},            // for faster lookup
      trigger_ac = true,
      old_q;

  (function () {                        // build word map
    var last, c;
    for(var i = 0; i < all_words.length; i++) {
      c = all_words[i].charAt(0);
      if(c !== last) {
        lookup_map[c] = i;
        last = c;
      }
    }
  })();

  function next_charactor (c) {
    var i = 0;
    for(;i < alphabet.length; ++i) {
      if(c === alphabet.charAt(i)) {
        ++i;
        break;
      }
    }
    return i === alphabet.length ? 'a' : alphabet.charAt(i);
  }

  function binary_search_word (w) {
    var low = 0, high = all_words.length - 1;
    while(low <= high) {
      var mid = Math.round((low + high) / 2),
          word = all_words[mid];
      if(w === word) {
        return mid;
      } else if (w > word) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }
    return 0;                   // easier
  }

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
    var i = binary_search_word(word);
    if(i > max_candiates / 2) { i -= Math.round(max_candiates / 2); }
    for(; i < all_words.length; i++) {
      if(result.push(all_words[i]) > max_candiates) {
        break;
      }
    }
    show_candidates(result, word, force_refresh);
  }

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

  function remove_from_array (array, e) {
    var result = [];
    for(var i = 0; i < array.length; i++) {
      if(array[i] !== e) {
        result.push(array[i]);
      }
    }
    return result;
  }

  function cancel_all_ajax () {
    for(var i = 0; i < ajax_queue.length; i++) {
      ajax_queue[i].abort();
    }
    ajax_queue = [];
  }

  function show_search_result (q, force_refresh, update_value) {
    cancel_all_ajax();
    var lookup_word = word_map[q] || q;
    var xhr = $.getJSON("/d/" + lookup_word, function (data) {
      ajax_queue = remove_from_array(ajax_queue, xhr);
      show_nearby_words(q, force_refresh);
      if(!update_value) {
        $q.val(q);
      }
      if(!Array.isArray(data)) { data = [data]; }
      var html = to_html(tmpls.explain, {
        items: data,
        word: data[0].w || lookup_word
      });
      $("#explain").empty().append(html);
      location.hash = q;
    });
    ajax_queue.push(xhr);
  }


  function auto_complete () {
    var q = $q.val().trim().toLowerCase();
    if(q && q !== old_q) {
      old_q = q;
      var result = [],  c = q.charAt(0),
          n = next_charactor(c),
          start = lookup_map[c] || 0,
          end = (n === 'a' ? all_words.length : lookup_map[n]);
      for(var i = start; i < end; i++) {
        var word = all_words[i];
        if(word.indexOf(q) === 0) {
          if(result.push(word) > max_candiates) {
            break;
          }
        }
      }
      show_candidates(result);
      if(result.length) {
        show_search_result(result[0], false, true);
      }
    }
  }

  $(document).keydown(function (e) {
    var which = e.which;
    $selected = $("li.selected");
    switch(which) {
    case 27:                    // ESC
      break;
    case 191:
      setTimeout(function () { $q.focus(); }, 1);
      break;
    case 13:                    // ENTER
      var q = $selected.text().trim() || $q.val().trim();
      show_search_result(q, true);
      break;
    case 74:                    // J
    case 40:                    // DOWN
      var $next = $selected.next();
      if($selected.length && $next.length) {
        $selected.removeClass('selected');
        $next.addClass('selected');
        show_search_result($next.text().trim());
      }
      break;
    case 75:                    // K
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

  $q.keydown(function (e) {
    if(!e.ctrlKey
       && e.which != 91         // win
       && e.which !== 27 && e.which !== 40
       && e.which !== 38 && e.which !== 13
       && e.which != 18) {      // Alt key
      setTimeout(auto_complete, 1);
      e.stopPropagation();
    }
  });

  delegateEvents($ac, {
    'click a': function (e) {
      $("li.selected").removeClass('selected');
      var $this = $(this);
      $this.addClass('selected');
      show_search_result($this.text().trim());
    }
  });

  if(location.hash) {
    var hash = location.hash.substring(1);
    show_search_result(hash, true);
  } else {
    show_empty_query_candidates();
  }

  $q.focus();
  window.switch_img = function (img) {
    var $img = $(img);
    img.src = "/imgs/" + $img.attr('data-img') + ".jpg";
  };
})();

