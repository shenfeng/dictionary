
def get_file_as_string(filename)
  data = ''
  f = File.open(filename, "r")
  f.each_line do |line|
    data += line
  end
  return data
end

def minify_js(name, jss)
  jscompiler = "closure-compiler.jar"
  target = "client/#{name}-min.js"

  source_arg = ''
  jss.each do |js|
    source_arg += " --js #{js} "
  end
  # ADVANCED_OPTIMIZATIONS SIMPLE_OPTIMIZATIONS
  sh "java -jar bin/#{jscompiler} --warning_level QUIET " +
    "--compilation_level SIMPLE_OPTIMIZATIONS " +
    "--js_output_file '#{target}' #{source_arg}"
end

jscompiler = "closure-compiler.jar"

file "bin/#{jscompiler}" do
  mkdir_p 'bin'
  sh 'wget http://closure-compiler.googlecode.com/files/compiler-latest.zip' +
    ' -O /tmp/closure-compiler.zip'
  rm_rf '/tmp/compiler.jar'
  sh 'unzip /tmp/closure-compiler.zip compiler.jar -d /tmp'
  rm_rf '/tmp/closure-compiler.zip'
  mv '/tmp/compiler.jar', "bin/#{jscompiler}"
end

task :deps => ["bin/#{jscompiler}"]

dict_jss = FileList['client/jquery.js',
                    'client/allwords.js',
                    'client/mustache.js',
                    'client/tmpls.js',
                    'client/dict.js']

def gen_jstempls()
  print "Generating tmpls.js, please wait....\n"
  html_tmpls = FileList["tmpls/**/*.*"]
  data = "(function(){var tmpls = {};"
  html_tmpls.each do |f|
    text = get_file_as_string(f).gsub(/\s+/," ")
    name = File.basename(f, ".tpl")
    data += "tmpls." + name + " = '" + text + "';\n"
  end
  data += "window.D = {tmpls: tmpls};})();\n"
  File.open("client/tmpls.js", 'w') {|f| f.write(data)}
end

desc "generate js template"
task :jstemple do
  gen_jstempls()
end

desc "lein swank"
task :swank do
  sh "rm classes -rf && lein javac && lein swank"
end

desc "watch for change"
task :watch do
  sh 'while inotifywait -r -e modify tmpls/; do rake jstemple; done'
end

desc "deploy"
task :deploy do
  sh "scripts/deploy"
end

desc "minify js"
task :minify_js do
  sh 'rm -f client/dict-min.js*'
  minify_js("dict", dict_jss);
  sh 'cd client && gzip -9 dict-min.js'
end
