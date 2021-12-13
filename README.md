# Java Runtime Decompiler
*Java Runtime Decompiler*, or *JRD* for short, allows you to extract bytecode from the running JVM and decompile it with an external decompiler.
## Relase
* Should be done in -Plegacy to enable injecting to jdk8 vms.
## Installation
* JRD 4 and lower: Note that Git, Maven & [JDK 8](https://adoptopenjdk.net/) with its tools.jar or [JDK11 and higher](https://adoptopenjdk.net/) are required to run or help develop this app.*
* JRD 5 and up: Note that Git, Maven & [JDK11 and higher](https://adoptopenjdk.net/) are required to run or help develop this app.*
### From GIT
#### Initial setup
```
$ git clone https://github.com/pmikova/java-runtime-decompiler.git
$ cd java-runtime-decompiler
$ mvn clean install  # builds the runtime decpompiler
$ mvn clean install -PdownloadPlugins # builds the decompiler and downloads the decompiler plugins for future use
$ mvn clean install -Pimages # on Linux, bundles the plugins and JRD into a standalone portable image

# $PLUGINS and $VERIFY_CP variables may help to solve some weird image building issues.
```
Then, in images/target/runtime-decompiler... `./start.sh` in a *Linux terminal* or `start.bat` in a *Windows CMD* to start the application. The usage of top level start.sh/bat is only for development purposes.

#### Configuring decompiler agent
In order to start using Java-Runtime-Decompiler, you will need to select the Decompiler Agent's path in *Configure → Agent Path*.
The Decompiler Agent is a built-in project and can usually be found at `./decompiler_agent/target/decompiler-agent-*.jar`. The image should have agent preset.
#### Configuring external decompilers
Internal *javap* and *javap -v* decompiling tools are available by default. In image, we try to keep as many decompilers as possible bundled.

Additionally, external decompilers are supported and can be configured in *Configure → Plugins*:
* You can download them using the links below and set them up yourself using the *New* button.
* You can use `mvn clean install -PdownloadPlugins` from a terminal in the project's directory and import the necessary files using the *Import* button.

Currently supported decompilers are:
* [Fernflower](https://github.com/JetBrains/intellij-community/tree/master/plugins/java-decompiler/engine)
* [Procyon](https://bitbucket.org/mstrobel/procyon/downloads/)
* [CFR](https://github.com/leibnitz27/cfr/)

Assemblers/Disassemblers
* [jasm](https://github.com/openjdk/asmtools)
* [jcoder](https://github.com/openjdk/asmtools)
* [javap](https://github.com/openjdk/jdk) (disassemble only)
#### Known issues
* `mvn clean install` results in `BUILD FAILURE` with the error
`java.lang.RuntimeException: Unexpected error: java.security.InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty`
(spotted on Windows)

   **Temporary solution**: Use `mvn clean install -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true`
   to disregard SSL certificates when building.
* `mvn clean install` results in `TEST FAILURE`

   **Temporary solution**: Use `mvn clean install -DskipTests` to not run test classes when building.
### From Fedora repositories
The Java-Runtime-Decompiler is packed together with Fernflower and Procyon decompilers in the main Fedora repositories and in EPEL 7.
```
dnf install java-runtime-decompiler
```
The resulting installation is fully preconfigured.

## Usage
### Local Processes
The list on the top left shows all currently running processes on the local JVM with the name of their main class and their process ID.
Selecting any item will display the respective Loaded Classes and Decompiled Bytecode screens.
To return to the Welcome screen, simply unselect the item by holding Control while clicking it.
### Remote Processes
The list on the bottom left shows any remote processes.
Connections to remote processes can be established either by using the *+ button* to the right of the heading, or by going to *Connect → New Connection*.
### Local FS (file system)
Here you can turn JRD to casual, user-friendly jar/folder/file decompiler. You create a FS VM by setting up classpath as you are used for JVM.
### Loaded Classes
The middle list contains the classes present in the selected process.
This list can be reloaded with the *Refresh button* at the top and searched through with the *Search field*.
### Decompiled Bytecode
The text area on the right shows decompiled bytecode of the selected class of the selected process.
Different results may be achieved with different decompilers; you can select the decompiler from the dropdown menu at the top right.
### Overwriting classes
Using the *Overwrite button* at the top, you can replace the currently selected class' bytecode with your own compiled .class file via a dialog.

![](https://user-images.githubusercontent.com/47597303/63510098-01977e00-c4de-11e9-8a72-24cec35bbc79.png)

### CLI
Commandline interface is powerful and allows for bulk processing of VM's, jars and much more...\

Enter `./start.sh --help` in a *Linux terminal* or `start.bat --help` in a *Windows CMD* to get started.
Decompile:
```
$ ./start.sh -decompile classpathless-compiler-api-2.1.jar Cfr '.*' -saveas /tmp/cplc -savelike dir
Processing io.github.mkoncek.classpathless.api.ClassIdentifier
Saved: /tmp/cplc/io/github/mkoncek/classpathless/api/ClassIdentifier.java
...
Saved: /tmp/cplc/io/github/mkoncek/classpathless/api/MessagesListener.java

```

Compile:
```
$ ./start.sh -compile -cp classpathless-compiler-api-2.1.jar /tmp/cplc/io/github/mkoncek/classpathless/api/ClassIdentifier.java -savelike fqn -saveas .
...
Saved: ./io.github.mkoncek.classpathless.api.ClassIdentifier.class
```

Upload:
```
$ ./start.sh -overwrite classpathless-compiler-api-2.1.jar io.github.mkoncek.classpathless.api.ClassIdentifier io.github.mkoncek.classpathless.api.ClassIdentifier.class
WARNING: Class package do not match directories.
Overwrite of class 'io.github.mkoncek.classpathless.api.ClassIdentifier' successful.
```

Disassemble:
```
$ ./start.sh  -decompile classpathless-compiler-api-2.1.jar jasm '.*' -saveas /tmp/cplc -savelike dir
Saved: /tmp/cplc/io/github/mkoncek/classpathless/api/ClassIdentifier.java
...
Saved: /tmp/cplc/io/github/mkoncek/classpathless/api/IdentifiedSource.java
Saved: /tmp/cplc/io/github/mkoncek/classpathless/api/MessagesListener.java
```

Assemble:
```
$ ./start.sh -compile -p jasm /tmp/cplc -r -saveas /tmp/bin
...
jasm compiler caled with input of: 8
entering into /tmp/jrd-jasm14590159245433727064tmp.dir
writing tmp file into /tmp/jrd-jasm14590159245433727064tmp.dir/src/io.github.mkoncek.classpathless.api.IdentifiedBytecode.java
...
Saved: /tmp/bin/io/github/mkoncek/classpathless/api/ClassIdentifier.class
Saved: /tmp/bin/io/github/mkoncek/classpathless/api/MessagesListener.class
```

Don't forget that all operations are same over classpath, remote vm, or process of VM - *runtime* compiler/decompiler!
