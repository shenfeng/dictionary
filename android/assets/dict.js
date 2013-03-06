(function () {

  var tmpls = window.D.tmpls,
      LOOK = window.LOOK || {look: function () {
        // allow easy debug
        return '[{"l":[{"m":"concerning or relating to a particular subject","e":["a book about politics","She said something about leaving town.","He lied about his age.","About that car of yours. How much are you selling it for?","What\'s he on about (=talking about)?","It\'s about Tommy, doctor. He\'s been sick again.","Naturally, my mother wanted to know all about it (=all the details relating to it)."]},{"m":"used to show why someone is angry, happy, upset etc","e":["I\'m really worried about Jack.","She\'s upset about missing the party."]},{"m":"in many different directions within a particular place, or in different parts of a place","e":["We spent the whole afternoon walking about town.","Books were scattered about the room."]},{"m":"in the nature or character of a person or thing","e":["There\'s something really strange about Liza.","What I like about the job is that it\'s never boring."]},{"r":"spoken","m":"used to ask a question that directs attention to another person or thing"},{"m":"to do something to solve a problem or stop a bad situation","e":["If we don\'t do something about it, the problem is going to get worse.","What can be done about the rising levels of pollution?"]},{"m":"if an organization, a job, an activity etc is about something, that is its basic purpose","e":["Leadership is all about getting your team to co-operate."]},{"r":"spoken","m":"used to tell someone to do something while they are doing something else because it would be easier to do both things at the same time","e":["Go and see what\'s the matter, and while you\'re about it you can fetch me my sweater."]},{"r":"spoken","m":"used to ask the reason for something that has just happened, especially someone\'s angry behaviour"},{"r":"literary","m":"surrounding a person or thing","e":["Jo sensed fear and jealousy all about her."]}],"FW":true,"FS":true,"t":"prep.","p":"\u0259\'baut"},{"l":[{"m":"if someone is about to do something, or if something is about to happen, they will do it or it will happen very soon","e":["We were just about to leave when Jerry arrived.","Work was about to start on a new factory building."]},{"r":"informal","m":"used to emphasize that you have no intention of doing something","e":["I\'ve never smoked in my life and I\'m not about to start now."]}],"t":"adj."},{"l":[{"r":"spoken","m":"a little more or less than a particular number, amount, or size","e":["I live about 10 miles away.","a tiny computer about as big as a postcard","We left the restaurant at round about 10.30."]},{"m":"in many different directions within a place or in different parts of a place","e":["People were rushing about, trying to find the driver.","Cushions were scattered about on the chairs."]},{"m":"near to you or in the same place as you","e":["Is Derrick about? There\'s a phone call for him.","Quick! Let\'s go while there\'s no-one about."]},{"r":"spoken","m":"existing or available now","e":["I hope she hasn\'t caught flu. There\'s a lot of it about.","She might get temporary work, but there\'s not much about."]},{"r":"informal","m":"almost or probably","e":["I was about ready to leave when somebody rang the doorbell.","\'Have you finished?\' \' Just about.\'","It\'s just about the worst mistake anyone could make."]},{"r":"spoken","m":"used to tell someone that you have told them everything you know"},{"m":"so as to face in the opposite direction","e":["He quickly turned about and walked away."]}],"FW":true,"FS":true,"t":"adv."}]';
      }};

  var body = $('body')[0];

  function store_hist (word) {
    var data = localStorage.getItem(word);
    if(data) { data = JSON.parse(data); }
    else { data = []; }
    data.push(new Date().getTime());
    localStorage.setItem(word, JSON.stringify(data));
  }

  function get_latest () {
    var w = 'welcome',
        last_ts = 0;
    for(var word in localStorage) {
      var ds = JSON.parse(localStorage.getItem(word));
      if(last_ts < ds[ds.length - 1]) {
        w = word;
      }
    }
    return w;
  }

  function lookup (lookup_word, save) {
    var str = LOOK.look(lookup_word);
    if(str) {
      if(save) {
        store_hist(lookup_word);  // successfully lookup
      }
      var data = JSON.parse(str);
      if(!data.length) { data = [data]; }
      var html = Mustache.to_html(tmpls.explain, {
        items: [data[0]],
        pronounce: data[0].p,
        word: data[0].w || lookup_word
      });
      $('#container').html(html);
    }
  }

  $('#form').submit(function (e) {
    e.preventDefault();
    var $q = $('#q'),
        q = $.trim($q.val());
    lookup(q, true);
    body.scrollTop = 0;
    $q[0].blur();
    return false;
  });

  $('#menu-panel').delegate('li', 'click', function () {
    var text = $(this).text();
    text = text.substring(0, text.indexOf('('));
    lookup(text);
    window.close_menu();
  });

  lookup(get_latest());
  $('#container').css('min-height', window.innerHeight);
})();