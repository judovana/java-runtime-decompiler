@echo off

:: Find Java tools.jar
set TOOLS="%JAVA_HOME%\lib\tools.jar"

:: Save decompiler directory and cd to Maven Local Repository
set "wd=%cd%"
cd %userprofile%/.m2

:: Recusively find libraries
FOR /f "delims=" %%i IN ('dir *rsyntaxtextarea-*.jar /B /S') DO set RSYNTAXTEXTAREA=%%i
FOR /f "delims=" %%i IN ('dir *gson-*.jar /B /S') DO set GSON=%%i
FOR /f "delims=" %%i IN ('dir *byteman-install-*.jar /B /S') DO set BYTEMAN=%%i
FOR /f "delims=" %%i IN ('dir *runtime-decompiler-*.jar /B /S') DO set JRD=%%i

:: cd back to cwd
cd %wd%

:: Concatenate classpath and launch the decompiler
set CLASSPATH=%TOOLS%;%RSYNTAXTEXTAREA%;%GSON%;%BYTEMAN%;%JRD%;
java -cp %CLASSPATH% org.jrd.backend.data.Main

exit