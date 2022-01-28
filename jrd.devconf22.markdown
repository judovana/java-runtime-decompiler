# Inspect and modify java classes in running JVM
##    Devconf 2022


	Jiri Vanek
	jvanek@redhat.com
	OpenJDK QA engineer


Weak point: https://github.com/tisnik/vim-weakpoint

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# JRD
 * https://github.com/pmikova/java-runtime-decompiler
  * https://github.com/pmikova/java-runtime-decompiler/blob/devconf2022/jrd.devconf22.markdown
  * https://github.com/pmikova/java-runtime-decompiler/releases/tag/java-runtime-decompiler-6.1
  * https://github.com/pmikova/java-runtime-decompiler/releases/download/java-runtime-decompiler-6.1/runtime-decompiler-6.1-with-decompilers.tar.xz
  * https://github.com/pmikova/java-runtime-decompiler/releases/tag/7.0-snapshot.1

Hot patching, instrumentation effect, investigations, reaching "unreachable" code

(Cli/Gui) + agent

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# java agents
	* native (jvmti) x java
	* Can be injected into fresh JVM
		* java -cp ${PWD} -javaagent:${PWD}/agent.jar=arg:value...  my.main
		* eg code coverage
	* can be injected into running JVM
		* https://docs.oracle.com/javase/8/docs/jdk/api/attach/spec/com/sun/tools/attach/VirtualMachine.html
		* vm.loadAgent(agentJar, agentOptions);
		* eg IDEs
	* fun parts
		* https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html
		* https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/ClassFileTransformer.html

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# agents hierarchy
	* bytecode origin
		-  original bytecodes -> incapable agents -> saved bytecodes
		-> saved bytecodes -> capable agents -> running bytecodes
		=> every time definition of class is requested, the pipe is run again
		=> no pernamently stored transformed code!
	* agent can not be detached
		* can only stop working (eg remove transformer, close server socket)
		* critical bug in JRD
			* was decompiler only
	* JVM must have "motivation" to reload bytecode

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# hotswap
	* byte[] -> byte[]
		* see "java agents" slide
	* moreover only bodies of methods are allowed to change
		* unless you have DCEVM pached jdk
		* https://dcevm.github.io/
	* the modified parts needs a motivations to take effect
		* call of the changed method is usually enough also for jitted code

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# standalone bytecode replacement
	* byteman - https://byteman.jboss.org/
		* rules
			* simple: https://github.com/judovana/JrdBytemanExamples/blob/master/btmn/date.btm
			* worse: https://github.com/bytemanproject/byteman/tree/main/sample/scripts
		* stable, safe
	* JRD
		* no rules, but you must compile
			* decompilers suck (source path, JRD 7)
		* just java, you can do everything, you can break a lot

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# hands on!
	* pre reqs:
	* https://github.com/pmikova/java-runtime-decompiler/releases/tag/java-runtime-decompiler-6.1
		* https://github.com/pmikova/java-runtime-decompiler/releases/download/java-runtime-decompiler-6.1/runtime-decompiler-6.1-with-decompilers.tar.xz
		* https://github.com/pmikova/java-runtime-decompiler/releases/download/java-runtime-decompiler-6.1/runtime-decompiler-6.1-with-decompilers.zip
	* jdk11 x 8 compatibility getting worse
		=> 11
	* https://github.com/judovana/JrdBytemanExamples.git
		* https://github.com/judovana/JrdBytemanExamples/blob/master/btmn/
		* https://github.com/judovana/JrdBytemanExamples/releases/tag/0.1
			* https://github.com/judovana/JrdBytemanExamples/releases/download/0.1/cdist.tar.xz

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# hands on!
	* warm up

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# jrd itself
	* cli
	* overwrite, api and init dialogues
	* `.*jrd.*Renderer.*`
    * decompiler, asms...

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# jrd itself
    * bad java?
	* -source 8 -target 8

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# calc
	* java -Djdk.attach.allowAttachSelf=true -jar cdist/nodebugBuild/EthernalCrashes.jar  math
    * jrd x byteman
	* missing lines?

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# calc
	* java -Djdk.attach.allowAttachSelf=true -jar cdist/fulldebugBuild/EthernalCrashes.jar  math
	* fix mult -> mul
	* fix div by zero exception to N/A

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# calc
	* java -Djdk.attach.allowAttachSelf=true -jar cdist/fulldebugBuild/EthernalCrashes.jar  math
	* attach byteman
	* show rule

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# date
	* java -Djdk.attach.allowAttachSelf=true -jar cdist/fulldebugBuild/EthernalCrashes.jar date1

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# date
	* java -Djdk.attach.allowAttachSelf=true -jar cdist/fulldebugBuild/EthernalCrashes.jar date1
	* No change? Lack of motivation to reload definition?

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# date
	* java -Djdk.attach.allowAttachSelf=true -jar cdist/fulldebugBuild/EthernalCrashes.jar date1
	* show byteman in action
	* show obfuscator in action

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# memory leak?
	* java -Djdk.attach.allowAttachSelf=true -jar cdist/fulldebugBuild/EthernalCrashes.jar row

--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# memory leak?
	* java -Djdk.attach.allowAttachSelf=true -jar cdist/fulldebugBuild/EthernalCrashes.jar row
	* jmap -histo <pid>
