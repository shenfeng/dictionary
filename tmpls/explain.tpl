<h1>{{word}}</h1>
<ul class="items">
  {{#items}}
    <li class="item">
      <span class="type">{{t}}</span>
      <ul class="imags">
        {{#i}}<img onerror="switch_img(this)"
          src="/bimgs/{{.}}.jpg" data-img="{{.}}"/>{{/i}}
      </ul>
      <ul>
        {{#l}}
          <li>
            <p class="explain">
              {{#g}}
                <span class="group">[{{g}}]</span>
              {{/g}}
              {{#h}}
                <span class="help">[{{h}}]</span>
              {{/h}}
              <span class="meaning">{{m}}</span>
            </p>
            <ol class="egs">
              {{#e}}
                <li><p class="example">{{.}}</p></li>
              {{/e}}
            </ol>
            <ul class="extra">
              {{#x}}
                <li>
                  <p class="phrase">{{p}}</p>
                  <ol class="egs">
                    {{#e}}
                      <li><p class="example">{{.}}</p></li>
                    {{/e}}
                  </ol>
                </li>
              {{/x}}
            </ul>
          </li>
        {{/l}}
      </ul>
    </li>
  {{/items}}
</ul>
