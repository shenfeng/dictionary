<div class="word">
  <h2>{{word}}</h2>
  {{#pronounce}}
    <span class="pronounce">[{{pronounce}}]</span>
  {{/pronounce}}
</div>
<ul class="items">
  {{#items}}
    <li>
      <div class="type">{{t}}</div>
      <div class="detail-all">
        <div class="imgs clearfix">
          {{#i}}
            <img onerror="switch_img(this)"
            src="/bimgs/{{.}}.jpg" data-img="{{.}}"/>
          {{/i}}
        </div>
        <ol class="detail">
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
              <ul class="egs">
                {{#e}}
                  <li><p class="example">{{.}}</p></li>
                {{/e}}
              </ul>
              <ul class="extra">
                {{#x}}
                  <li>
                    <p class="phrase">{{p}}</p>
                    <ul class="egs">
                      {{#e}}
                        <li><p class="example">{{.}}</p></li>
                      {{/e}}
                    </ul>
                  </li>
                {{/x}}
              </ul>
            </li>
          {{/l}}
        </ol>
      </div>
    </li>
  {{/items}}
</ul>
