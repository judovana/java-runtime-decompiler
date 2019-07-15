<a href="https://copr.fedorainfracloud.org/coprs/radekmanak/java-runtime-decompiler/package/java-runtime-decompiler/"><img src="https://copr.fedorainfracloud.org/coprs/radekmanak/java-runtime-decompiler/package/java-runtime-decompiler/status_image/last_build.png" /></a>
# Java-Runtime-Decompiler
This application allows you to extract bytecode from the running JVM and decompile it with an external decompiler.
## Installation
### From GIT
```
git clone https://github.com/pmikova/java-runtime-decompiler.git
cd java-runtime-decompiler
mvn clean install
./start.sh
```
### From Fedora repositories
The Java-Runtime-Decompiler is packed together with Fernflower and Procyon decompilers in the main Fedora repositories and in EPEL 7 and up for Fedora built from master is available.
```
dnf install java-runtime-decompiler
```
The resulting installation is fully prconfigured.

There is also `dnf copr enable radekmanak/java-runtime-decompiler`  for a nightly build, but the specfile may be outdated.
## Usage
When the application starts go to Config -> Configure and select the Agent's path. The Decompiler Agent is a built-in project.

You need to get a Java decompiler, e.g. here:
https://bitbucket.org/mstrobel/procyon/downloads/

And place it accordingly, or change paths to its jars in configuration files.

![](https://i.imgur.com/3N8hFOp.png)
