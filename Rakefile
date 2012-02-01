
def get_file_as_string(filename)
  data = ''
  f = File.open(filename, "r")
  f.each_line do |line|
    data += line
  end
  return data
end

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
  sh "./deploy"
end
