<h1>{{word}}</h1>
<ul class="items">
  {{#items}}
    <li class="item">
      <span class="type">{{t}}</span>
      <ul>
        {{#l}}
          <li>
            <p class="meaning">{{m}}</p>
            <ol class="egs">
              {{#e}}
                <li>
                  {{.}}
                </li>
              {{/e}}
            </ol>
          </li>
        {{/l}}
      </ul>
    </li>
  {{/items}}
</ul>
