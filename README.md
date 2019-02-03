<a href="https://copr.fedorainfracloud.org/coprs/radekmanak/java-runtime-decompiler/package/java-runtime-decompiler/"><img src="https://copr.fedorainfracloud.org/coprs/radekmanak/java-runtime-decompiler/package/java-runtime-decompiler/status_image/last_build.png" /></a>
# Java-Runtime-Decompiler
This application allows you to extract bytecode from running JVM and decompile it with an external decompiler.
## Install
### From GIT
```
git clone https://github.com/pmikova/java-runtime-decompiler.git
cd java-runtime-decompiler
mvn clean install
./start.sh
```
### Fedora
java-runtime decompiler + fernflower and procyoin ar epacke din mian Fedora repositories and in EPEL 7 and up for Fedora built from master is available.
```
dnf install java-runtime-decompiler
```
The resulting installation is fully prconfigured

## Usage
When the application opens go to menubar -> configure and select Agent path.

Agent is built in decompiler-agent project.
You need to get a java decompiler (e.g. here):
https://bitbucket.org/mstrobel/procyon/downloads/

And place it accordingly, or change paths to its jars in configuration files.

![](https://i.imgur.com/3N8hFOp.png)
