# Java-Runtime-Decompiler
This application allows you to extract bytecode from running JVM and decompile it with an external decompiler.
## Install
```
git clone https://github.com/pmikova/Java-Runtime-Decompiler.git
cd Java-Runtime-Decompiler
mvn install
```
run with:
```
java -cp /usr/lib/jvm/default/lib/tools.jar:$HOME/.m2/repository/com/fifesoft/rsyntaxtextarea/2.6.1/rsyntaxtextarea-2.6.1.jar:$HOME/.m2/repository/io/github/soc/directories/10/directories-10.jar:$HOME/.m2/repository/java/java-runtime-decompiler/1.0.0-SNAPSHOT/java-runtime-decompiler-1.0.0-SNAPSHOT.jar com.redhat.thermostat.vm.decompiler.data.Main
```
When the application opens go to menubar -> configure and select Agent and Decompiler paths.

You can get the agent here:  
https://github.com/pmikova/thermostat-decompiler-agent  
And a java decompiler here:  
https://bitbucket.org/mstrobel/procyon/downloads/  

NOTE: Java Runtime Decompiler is currently in alpha and can be unstable. Use it at your own risk!

![](https://i.imgur.com/3N8hFOp.png)
