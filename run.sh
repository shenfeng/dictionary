#! /bin/sh -x

rm classes -rf
lein javac
java -Xmx1500m -Xms1500m \
    -cp lib/*:src/:classes/ clojure.main \
    -e "(require 'ldoce.core) (ldoce.core/split-to-disk)"
