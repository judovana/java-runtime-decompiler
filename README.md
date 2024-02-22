# java-runtime-(de)compiler - talk for Brno CZ java user group

see [jrd.markdown](https://github.com/judovana/java-runtime-decompiler/blob/brnoCzJug022024/jrd.markdown) for non ammended slides

presentation runs in https://github.com/tisnik/vim-weakpoint vim plugin

jrd-WeakPoint are generated slkides by vim-weakpoint:
```
lua ~/.vim/bundle/vim-weakpoint/WeekPointSplitter.lua  ./jrd.markdown  -deduct -height 30 -vim
```

# java-runtime-(de)compiler

https://github.com/judovana/java-runtime-decompiler

	* why? Because we can!
		* binary blob reproducers
			* obfuscated
			* debuginfo
		* swap lines
		* oversee instrumetnations
	* what!
	* how? This talk!
		* standard APIS only

Jiri Vanek
Red Hat

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# Demo I

	* install
	* overview
	* list and read
--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# How - Agent attach I

https://docs.oracle.com/javase/8/docs/jdk/api/attach/spec/com/sun/tools/attach/VirtualMachine.html

```
VirtualMachine vm = VirtualMachine.attach(Integer.toString(pid));
vm.loadAgent(agentJar,”param1:value1,param:value2,…valueN”);
vm.detach();
```
	* Agent is just bunch of classes which does nothing. No logic, no communication
		* unless  you write that
--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# How - Agent attach II
```
public static void premain(String agentArgs, Instrumentation inst); 
```
	* Is  the launched start point after agent is loaded, and instrumentation is what matters
	* For example you can open ServerSocket to receive commands
	* Agents can not be unloaded. Can be just turned off in best effort
	* And you can register transformer!
--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# Demo II
.
	* self modifications
		* classes
		* classloaders I
		* desompilers/disassemblers/hex
			* –-patch-module
			* (keyword based) code completion
			* inner classes
			* additional sources/binaries/diffs
		* back compile/assemble
		* real changes
		* bytecode level
	* java.lang.Override
--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# How - Transformers

https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html
https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/ClassFileTransformer.html

```
Transformer transformer = new Transformer();
inst.addTransformer(transformer, true);
//repeat(work-earn-spend)until die
instrumentation.removeTransformer(transformer);
```
	* Classes can NEVER EVER be unloaded
		* Depends on JVM?
--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# Demo III
.
	* self modifications
	* Class Redefinition - hotswap - limits
		* No new methods, no new fields, no renaming, no signatures…
			* even non used BYTECODE fieLds limits
			* bytecode level!
		* Generally you can only change content of methods bodies (including new variables)
			* byteman and JRD apis
			* not add jar/classes
			* dcevm/jrebel
	* global counter
--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# How - Trasnformer api
```
byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
Byte[] -> byte[] //… can not be more simple
```
	* asm ow2	
	* If the class was never used, it will not be reachable from there
	* Calling Class.forName on it will then do the job
...
	* So what actually JRD simply does, is **Map<classname,Map<classloader,body[]>** where overrides are stored, ad transform returns  hit front hat map if found
	* Similarly, if we require class, to send to client, we record it in this method
	* You have to return the “modified” bytecode everytime class redefiniton happens (which is pretty often)
	* Depends on VM, but on hotspot, one of the few places where it will not be picked up is during loop in method

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# How - chaining of agents/transformers
.
	* agents are modyfying and non modyfying
	* agents are processed in order of attach
	* modyfing (with Transformer) agents processed in order of reistered Transformer
	* original class definition (immutable) -> byte[] ->
		* Transformer1 -> byte[] ->
		* Transformer.. -> byte[] ->
		* TransformerN.. -> byte[] ->
		* final class definition usage
	*the full chain is always called. 
		* if you unregister, changes are gone

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# Demo IV

	* byteman
		* second agent!
		* silent non sucess
	* chaining of agents
	* code coverage instrumentation
	* multiple class defintions classloaders II
		* no change in gui
	* modules

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# Demo V

	* cli
	* adding classes
		* ServerImplNw
	* patch
		* not as simple
		* split?
		* full java files?
		* full binary classes?
	* Future - debugger attach
		* source-codeless IDE

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# More hardcore ways to overcome hotswap limitations
.
	* Class Redefinition - hotswap - limits
		* No new methods, no new fields, no renaming…
		* Generally you can only change content of methods bodies (including new variables)
		* DCEVM JDK based on Phd these is removing those limits with (huge) cost of performance
		* Take ages after JDK (jdk 17 not yet properly out, last udpate December 2022)
	* Jetbrains took it recently over
	* Jrebel JVMTI hacks
	* Tricks
		* In agent predefined map of objects to store fields/methods
		* Bytemen do it behind the doors
		* JRD offer sclumsy appi for it
		* Add a new class with reimplementation and call it where needed
		* inst.appendToSystem/BootClassLoader


--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# CPLC I
.
	* java compiler api is terrible
```
public interface ClassesProvider {
    Collection<IdentifiedBytecode> getClass(ClassIdentifier... names);
    List<String> getClassPathListing();
}
```
```
public interface ClasspathlessCompiler {
   Collection<IdentifiedBytecode> compileClass(ClassesProvider classesProvider,
            Optional<MessagesListener> messagesListener, IdentifiedSource... javaSourceFiles);
   ....
}
```
--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# CPLC II
.
	* init of classes
		* in case of jrd easy to get dependences from bytecode
		* for eg sources based compiler no op
	* the default implementations is classical class-path based javac
	* sources based plugins and many others
		* JEP 458: Launch Multi-File Source-Code Programs
		* (class-path based compiling classloader)
--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE---
# Enjoy!
....
	https://github.com/judovana/java-runtime-decompiler
	https://github.com/mkoncek/classpathless-compiler
	https://byteman.jboss.org/
	https://dcevm.github.io/
	https://docs.oracle.com/javase/8/docs/jdk/api/attach/spec/com/sun/tools/attach/VirtualMachine.html
	https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html
	https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/ClassFileTransformer.html
....
	https://github.com/tisnik/vim-weakpoint

