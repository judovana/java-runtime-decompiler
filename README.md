<a href="https://copr.fedorainfracloud.org/coprs/radekmanak/java-runtime-decompiler/package/java-runtime-decompiler/"><img src="https://copr.fedorainfracloud.org/coprs/radekmanak/java-runtime-decompiler/package/java-runtime-decompiler/status_image/last_build.png" /></a>
# Java-Runtime-Decompiler
This application allows you to extract bytecode from the running JVM and decompile it with an external decompiler.
## Installation
*Note that JDK 8 is required for this app to run.*
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
When the application starts go to *Config → Configure* and select the Agent's path.
The Decompiler Agent is a built-in project and can usually be found at `./decompiler_agent/target/decompiler-agent-*.jar`.

By default you can use the internal *javap* and *javap -v* decompiling tools.
You can also download an external decompiler, either yourself or with `mvn clean install -PdownloadPlugins`, and set it up in *Config → Plugin configuration*.

Currently supported decompilers are:
* [Fernflower](https://github.com/JetBrains/intellij-community/tree/master/plugins/java-decompiler/engine)
* [Procyon](https://bitbucket.org/mstrobel/procyon/downloads/)

___
![](https://i.imgur.com/3N8hFOp.png)
