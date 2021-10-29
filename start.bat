@echo off
echo.%* | find /I "-verbose">nul && ( @echo on & set "VERBOSE=TRUE") || ( @echo off & set "VERBOSE=FALSE")
setLocal EnableDelayedExpansion

rem Resolve this batch file's location
set "PORTABLE_JRD_HOME=%~dp0"

rem Resolve JDK using environment variable, registry query or path search. Each step includes verification of the directory using java -version.
rem Algorithm courtesy of https://github.com/AdoptOpenJDK/IcedTea-Web/blob/master/launchers/shell-launcher/launchers.bat.in
set "JDK_LOCATION="
if not "%JAVA_HOME%" == "" (
  "%JAVA_HOME%/bin/java.exe" -version >nul 2>&1
  if errorlevel 0 if not errorlevel 1 (
    set "JDK_LOCATION=%JAVA_HOME%"
  ) else (
    echo Your JDK at [%JAVA_HOME%] read from the JAVA_HOME environment variable is not valid. Please fix this.
    goto:eof
  )
) else (
  if "%VERBOSE%" == "TRUE" ( echo JAVA_HOME environment variable non-existent. )
)

if "%JDK_LOCATION%" == "" (
  for /f "tokens=*" %%a in ('%windir%\System32\reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit" 2^>nul') do set "_version_key=%%a"
  set "_version=!_version_key:~58!"
  for /f "tokens=*" %%a in ('%windir%\System32\reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit\!_version!" /v JavaHome 2^>nul') do set "_jh_key=%%a"
  set "_BAD_SLASH_JAVA_HOME=!_jh_key:~22!"
  set "_REG_JAVA_HOME=!_BAD_SLASH_JAVA_HOME:\=/!"

  if EXIST "!_REG_JAVA_HOME!/bin/java.exe" (
    "!_REG_JAVA_HOME!/bin/java.exe" -version >nul 2>&1
      if errorlevel 0 if not errorlevel 1 (
        set JDK_LOCATION=!_REG_JAVA_HOME!
      ) else (
        echo Your JDK [!_REG_JAVA_HOME!] read from Registry HKLM\SOFTWARE\JavaSoft\Java Development Kit is not valid. Please fix this.
        goto:eof
      )
  ) else (
    if "%VERBOSE%" == "TRUE" ( echo Registry entries for JAVA_HOME non-existent. )
  )
  set "_version_key=" & set "_version=" & set "_jh_key=" & set "_BAD_SLASH_JAVA_HOME=" & set "_REG_JAVA_HOME="
)

if "%JDK_LOCATION%" == "" (
  set "_PATH_JAVA_HOME="
  for %%a in ("%PATH:;=";"%") do (
    if exist "%%~a/javac.exe" (
      for %%a in ("%%~a!") do set "_parent=%%~dpa"
      for %%a in ("!_parent!.") do set "_PATH_JAVA_HOME=%%~dpa"
    )
  )
  "!_PATH_JAVA_HOME!/bin/java.exe" -version >nul 2>&1
    if errorlevel 0 if not errorlevel 1 (
      set JDK_LOCATION=!_PATH_JAVA_HOME!
    ) else (
      echo Your JDK [!_PATH_JAVA_HOME!] read from the PATH environment variable is not valid. Please fix this.
      goto:eof
    )
    set "_parent=" & set "_PATH_JAVA_HOME="
)

if "%JDK_LOCATION%" == "" (
  echo Unable to find a JDK on your computer. Please fix this before trying to launch the app again.
  goto:eof
)

rem Remove trailing backslash if present
if "%JDK_LOCATION:~-1%"=="\" set "JDK_LOCATION=%JDK_LOCATION:~0,-1%"
if "%PORTABLE_JRD_HOME:~-1%"=="\" set "PORTABLE_JRD_HOME=%PORTABLE_JRD_HOME:~0,-1%"

rem Get and verify Java tools.jar
set TOOLS="%JDK_LOCATION%\lib\tools.jar"
if not exist %TOOLS% (
  if "%VERBOSE%" == "TRUE" ( echo %TOOLS% file not found, however it isn't necessary for JDK 11+ )
)

rem Dummy that gets edited in image generation
set PURPOSE=DEVELOPMENT

rem Recursively find libraries
call :findLib "com\fifesoft\rsyntaxtextarea","*rsyntaxtextarea-*.jar",RSYNTAXTEXTAREA
call :findLib "com\google\code\gson\gson","*gson-*.jar",GSON
call :findLib "org\jboss\byteman\byteman-install","*byteman-install-*.jar",BYTEMAN
call :findLib "java-runtime-decompiler\runtime-decompiler","*runtime-decompiler-*.jar",JRD
call :findLib "io\github\mkoncek\classpathless-compiler","*classpathless-compiler-*.jar",CPLC
call :findLib "io\github\mkoncek\classpathless-compiler-api","*classpathless-compiler-api-*.jar",CPLC_API
call :findLib "io\github\mkoncek\classpathless-compiler-util","*classpathless-compiler-util-*.jar",CPLC_UTIL
call :findLib "org\ow2\asm\asm-tree","asm-tree-*.jar",ASM_TREE
call :findLib "org\ow2\asm\asm","asm-*.jar",ASM_JAR

rem Maps an available drive letter via pushd if script is located on a network drive - done to counteract CMD not supporting UNC paths
if "%PORTABLE_JRD_HOME:~0,2%"=="//" (
  pushd %PORTABLE_JRD_HOME%
  set "PROPERTY_LOCATION=-Djrd.location=%CD%"
) else (
  set "PROPERTY_LOCATION=-Djrd.location=%PORTABLE_JRD_HOME%"
)

rem Create environment variable pointing to script's location
set "PROPERTY_PURPOSE=-Djrd.purpose=%PURPOSE%"

rem Concatenate classpath and launch the app
set CLASSPATH=%TOOLS%;%RSYNTAXTEXTAREA%;%GSON%;%BYTEMAN%;%JRD%;%CPLC%;%CPLC_API%;%CPLC_UTIL%;%ASM_TREE%;%ASM_JAR%
"%JDK_LOCATION%\bin\java.exe" -Djdk.attach.allowAttachSelf=true %PROPERTY_LOCATION% %PROPERTY_PURPOSE% -cp %CLASSPATH% org.jrd.backend.data.Main %*

if "%PORTABLE_JRD_HOME:~0,2%"=="//" (
  popd
)

rem Exit before function section
exit /B 0

rem Function for finding mvn repo files (2nd arg) of a certain group (1st arg) & returning it through the 3rd arg
:findLib
if "x%PURPOSE%" == "xDEVELOPMENT" (
  pushd %userprofile%\.m2\repository\%~1
) else (
  pushd %PORTABLE_JRD_HOME%\libs\deps
)

for /f "delims=" %%i in ('dir %~2 /B /S') do set %~3=%%i

popd
exit /b 0
