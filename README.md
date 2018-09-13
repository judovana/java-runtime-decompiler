# Java-Runtime-Decompiler
This application allows you to extract bytecode from running JVM and decompile it with an external decompiler.
## Install
```
git clone https://github.com/pmikova/Java-Runtime-Decompiler.git
cd Java-Runtime-Decompiler
mvn clean install
```
run with:
```
start.sh script
```
When the application opens go to menubar -> configure and select Agent path.

You can get the agent here:  
https://github.com/pmikova/thermostat-decompiler-agent  
And a java decompiler here:  
https://bitbucket.org/mstrobel/procyon/downloads/  

NOTE: Java Runtime Decompiler is currently in alpha and can be unstable. Use it at your own risk!

![](https://i.imgur.com/3N8hFOp.png)
