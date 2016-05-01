#!/bin/sh
java -Xms32m -Xmx256m -cp lib/rel.jar:lib/derby.jar:lib/pg.jar:lib/owlapi3.jar:lib/JSAP-2.1.jar:lib/TrOWLCore.jar:lib/aterm-java-1.6.jar:lib/bzip2.jar:lib/commons-configuration-1.6.jar:lib/h2.jar:lib/servlet-api.jar:lib/jetty-all.jar trowl.QueryServer "$@"

