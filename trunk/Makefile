make:
	javac -cp ../zookeeper-3.3.2.jar:../lib/log4j-1.2.15.jar:../lib/jline-0.9.94.jar:../conf/:. Election.java

run:
	java -cp ../zookeeper-3.3.2.jar:../lib/log4j-1.2.15.jar:../lib/jline-0.9.94.jar:../conf/:. Election cluster1.lab.ic.unicamp.br:33999

slides:
	latex mc715.tex
	dvips mc715.dvi
	ps2pdf mc715.ps

