# Inspect and modify java classes in running JVM
##    Devconf 2022


	Jiri Vanek
	jvanek@redhat.com
	OpenJDK QA engineer


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
		=> no pernamently stored tranformed code!
	* agent can not bee detached
		* can only stop working (eg remove transformer, close server socket)
		* critical bug in JRD
	* JVM must have "motivation" to relaod bytecode
--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# hotswap
	* byte[] -> byte[]
	* moreover only bodies of methods are allowed to change
		* unless you have DCEVM pached jdk
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
			* decopilers suck (source path, JRD 7)
		* just java, you can do everthing, you can break a lot
--PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE----PAGE--
# hands on!
	* https://github.com/pmikova/java-runtime-decompiler/releases/tag/java-runtime-decompiler-6.1
		* https://github.com/pmikova/java-runtime-decompiler/releases/download/java-runtime-decompiler-6.1/runtime-decompiler-6.1-with-decompilers.tar.xz
		* https://github.com/pmikova/java-runtime-decompiler/releases/download/java-runtime-decompiler-6.1/runtime-decompiler-6.1-with-decompilers.zip
	* jdk11 x 8 comaptibility getting worse
		=> 11
	* https://github.com/judovana/JrdBytemanExamples.git
		* https://github.com/judovana/JrdBytemanExamples/blob/master/btmn/
		* https://github.com/judovana/JrdBytemanExamples/releases/tag/0.1
			* https://github.com/judovana/JrdBytemanExamples/releases/download/0.1/cdist.tar.xz
