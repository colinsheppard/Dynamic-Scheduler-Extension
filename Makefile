ifeq ($(origin JAVA_HOME), undefined)
  JAVA_HOME=/usr
endif

ifeq ($(origin NETLOGO), undefined)
  NETLOGO=../..
endif

ifneq (,$(findstring CYGWIN,$(shell uname -s)))
  COLON=\;
  JAVA_HOME := `cygpath -up "$(JAVA_HOME)"`
else
  COLON=:
endif

SRCS=$(wildcard src/*.java)

dynamic-scheduler.jar dynamic-scheduler.jar.pack.gz: $(SRCS) manifest.txt
	mkdir -p classes
	$(JAVA_HOME)/bin/javac -g -encoding us-ascii -source 1.5 -target 1.5 -classpath $(NETLOGO)/NetLogoLite.jar -d classes $(SRCS)
	jar cmf manifest.txt dynamic-scheduler.jar -C classes .
	pack200 --modification-time=latest --effort=9 --strip-debug --no-keep-file-order --unknown-attribute=strip dynamic-scheduler.jar.pack.gz dynamic-scheduler.jar

dynamic-scheduler.zip: dynamic-scheduler.jar
	rm -rf dynamic-scheduler
	mkdir dynamic-scheduler
	cp -rp dynamic-scheduler.jar dynamic-scheduler.jar.pack.gz README.md Makefile src manifest.txt dynamic-scheduler
	zip -rv dynamic-scheduler dynamic-scheduler
	rm -rf dynamic-scheduler
