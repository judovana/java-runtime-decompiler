rem Turn off debugging output (essentially 'set +x')
@echo off

rem If any argument case-insensitively matches "-verbose" toggle debugging output and assign the VERBOSE variable
echo.%* | find /I "-verbose">nul && ( @echo on & set "VERBOSE=TRUE") || ( @echo off & set "VERBOSE=FALSE")

rem DelayedExpansion allows for !variables! to be expanded when reached, rather than in advance
setLocal EnableDelayedExpansion

rem Resolve this batch file's location: *d*rive + *p*ath of the 0th argument, the script itself
set "PORTABLE_JRD_HOME=%~dp0"

rem Resolve JDK using environment variable, registry query or path search. Each step includes verification of the directory using java -version.
rem Algorithm courtesy of https://github.com/AdoptOpenJDK/IcedTea-Web/blob/master/launchers/shell-launcher/launchers.bat.in
set "JDK_LOCATION="

rem If the env var JAVA_HOME is not empty, check it points to a functioning executable
if not "%JAVA_HOME%" == "" (
  "%JAVA_HOME%/bin/java.exe" -version >nul 2>&1
  if errorlevel 0 if not errorlevel 1 (
    set "JDK_LOCATION=%JAVA_HOME%"
  ) else (
    echo Your JDK at [%JAVA_HOME%] read from the JAVA_HOME environment variable is not valid. Please fix this.

    rem Terminate script
    goto:eof
  )
) else (
  if "%VERBOSE%" == "TRUE" ( echo JAVA_HOME environment variable non-existent. )
)

rem Attempt to find java.exe from registry keys
if "%JDK_LOCATION%" == "" (
  rem First, find the version
  for /f "tokens=*" %%a in ('%windir%\System32\reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit" 2^>nul') do set "_version_key=%%a"

  rem Remove the first 57 chars, the length of "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit"
  set "_version=!_version_key:~58!"

  rem Second, query for a specific registry key value of JavaHome
  for /f "tokens=*" %%a in ('%windir%\System32\reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit\!_version!" /v JavaHome 2^>nul') do set "_jh_key=%%a"

  rem Remove the first 21 chars, the length of ???
  set "_BAD_SLASH_JAVA_HOME=!_jh_key:~22!"

  rem Replace backslashes with forward slashes
  set "_REG_JAVA_HOME=!_BAD_SLASH_JAVA_HOME:\=/!"

  rem Check that the found java.exe path points to a functioning executable
  if EXIST "!_REG_JAVA_HOME!/bin/java.exe" (
    rem Test exit_success of calling "java -version, ignoring outputs"
    "!_REG_JAVA_HOME!/bin/java.exe" -version >nul 2>&1
    if errorlevel 0 if not errorlevel 1 (
      set JDK_LOCATION=!_REG_JAVA_HOME!
    ) else (
      echo Your JDK [!_REG_JAVA_HOME!] read from Registry HKLM\SOFTWARE\JavaSoft\Java Development Kit is not valid. Please fix this.

      rem Terminate script
      goto:eof
    )
  ) else (
    if "%VERBOSE%" == "TRUE" ( echo Registry entries for JAVA_HOME non-existent. )
  )

  rem Unset variables for some reason
  set "_version_key=" & set "_version=" & set "_jh_key=" & set "_BAD_SLASH_JAVA_HOME=" & set "_REG_JAVA_HOME="
)

rem Attempt to find java.exe from the PATH environment variable
if "%JDK_LOCATION%" == "" (
  set "_PATH_JAVA_HOME="

  rem Split PATH on ";" (Windows' path separator), iterate over the chunks
  for %%a in ("%PATH:;=";"%") do (
    if exist "%%~a/javac.exe" (
      rem Somehow get parent directory (from ."/bin/" to "./")? Might be a typo/oversight with both calls getting the drive+path of %%a, might not
      for %%a in ("%%~a!") do set "_parent=%%~dpa"
      for %%a in ("!_parent!.") do set "_PATH_JAVA_HOME=%%~dpa"
    )
  )

  rem Check that the found java.exe path points to a functioning executable
  "!_PATH_JAVA_HOME!/bin/java.exe" -version >nul 2>&1
    if errorlevel 0 if not errorlevel 1 (
      set JDK_LOCATION=!_PATH_JAVA_HOME!
    ) else (
      echo Your JDK [!_PATH_JAVA_HOME!] read from the PATH environment variable is not valid. Please fix this.
      goto:eof
    )
    set "_parent=" & set "_PATH_JAVA_HOME="
)

rem If no JDK has been found in any of the ways, terminate the script
if "%JDK_LOCATION%" == "" (
  echo Unable to find a JDK on your computer. Please fix this before trying to launch the app again.
  goto:eof
)

rem Remove trailing backslashes if present as the last characters
if "%JDK_LOCATION:~-1%"=="\" set "JDK_LOCATION=%JDK_LOCATION:~0,-1%"
if "%PORTABLE_JRD_HOME:~-1%"=="\" set "PORTABLE_JRD_HOME=%PORTABLE_JRD_HOME:~0,-1%"

rem Get and verify Java tools.jar
set TOOLS="%JDK_LOCATION%\lib\tools.jar"
if not exist %TOOLS% (
  if "%VERBOSE%" == "TRUE" ( echo %TOOLS% file not found, however it isn't necessary for JDK 11+ )
)

rem Dummy that gets replaced with "=PORTABLE" in image generation
set PURPOSE=DEVELOPMENT

rem Recursively find libraries (if this is a portable image, the first argument is basically not used in the function - every .jar is in "/deps")
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
"%JDK_LOCATION%\bin\java.exe" --add-exports jdk.jdeps/com.sun.tools.javap=ALL-UNNAMED -Djdk.attach.allowAttachSelf=true %PROPERTY_LOCATION% %PROPERTY_PURPOSE% -cp %CLASSPATH% org.jrd.backend.data.Main %*

rem popd from network drive related pushd
if "%PORTABLE_JRD_HOME:~0,2%"=="//" (
  popd
)

rem Exit before function section
exit /B 0

rem Function for finding mvn repo files (2nd arg) of a certain group (1st arg) & returning it through the 3rd arg
:findLib

rem Pushd into library location
if "x%PURPOSE%" == "xDEVELOPMENT" (
  pushd %userprofile%\.m2\repository\%~1
) else (
  pushd %PORTABLE_JRD_HOME%\libs\deps
)

rem Assign full path of sought-after .jar file to the return argument
for /f "delims=" %%i in ('dir %~2 /B /S') do set %~3=%%i

rem Don't forget to popd from library location!
popd

exit /b 0
