rem @echo off
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
  )
) else (
  echo JAVA_HOME environment variable non-existent.
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
      )
  ) else (
    echo Registry entries for JAVA_HOME non-existent.
  )
  set "_version_key=" & set "_version=" & set "_jh_key=" & set "_BAD_SLASH_JAVA_HOME=" & set "_REG_JAVA_HOME="
)

if "%JDK_LOCATION%" == "" (
  echo Searching for JAVA_HOME through the filesystem. This might take a while.

  rem Get valid drive letters, remove trailing colon :
  set _drives=
  for /f "tokens=2 delims==" %%d in ('wmic logicaldisk where "drivetype=3" get name /format:value') do (
    set "_editable=%%d"
    set "_drives=!_drives! !_editable:~0,1!"
  )

  rem Search drive for java.exe, return last path that is not in a JRE
  set _result=
  for %%d in (!_drives!) do (
    for /f "tokens=* delims=" %%f in ('dir/b/s %%d:\java.exe 2^>nul') do (
      if exist %%f (
        for /f "tokens=*" %%r in ('echo %%f^|findstr /rv "jre"') do set _result=%%r
      )
    )
  )

  rem Navigate to JDK's root directory
  for %%a in ("!_result!") do set "_parent=%%~dpa"
  for %%a in ("!_parent!.") do set "_PATH_JAVA_HOME=%%~dpa"

  "!_PATH_JAVA_HOME!/bin/java.exe" -version >nul 2>&1
  if errorlevel 0 if not errorlevel 1 (
      set JDK_LOCATION=!_PATH_JAVA_HOME!
    ) else (
      echo "Your JDK found at [!_PATH_JAVA_HOME!] is not valid. Please fix this."
    )
  )
  set "_drives=" & set "_editable=" & set "_drives=" & set "_result=" & set "_parent=" & set "_PATH_JAVA_HOME="
)

if "%JDK_LOCATION%" == "" (
  echo Unable to find a JDK on your computer. Please fix this before trying to launch the app again.
  goto:eof
)

rem Remove trailing backslash if present
IF "%JDK_LOCATION:~-1%"=="\" SET "JDK_LOCATION=%JDK_LOCATION:~0,-1%"
IF "%PORTABLE_JRD_HOME:~-1%"=="\" SET "PORTABLE_JRD_HOME=%PORTABLE_JRD_HOME:~0,-1%"

rem Get and verify Java tools.jar
set TOOLS="%JDK_LOCATION%\lib\tools.jar"
if not exist %TOOLS% (
  echo %TOOLS% file not found. Perhaps you are using a wrong JDK or a JRE. Please fix this before trying to launch the app again.
  goto:eof
)

rem Dummy that gets edited in image generation
set PURPOSE=DEVELOPMENT

rem Change directory for library search
if "x%PURPOSE%" == "xDEVELOPMENT" (
  pushd %userprofile%\.m2\repository
) else (
  pushd %PORTABLE_JRD_HOME%\libs\deps
)

rem Recursively find libraries
for /f "delims=" %%i in ('dir *rsyntaxtextarea-*.jar /B /S') do set RSYNTAXTEXTAREA=%%i
for /f "delims=" %%i in ('dir *gson-*.jar /B /S') do set GSON=%%i
for /f "delims=" %%i in ('dir *byteman-install-*.jar /B /S') do set BYTEMAN=%%i
for /f "delims=" %%i in ('dir *runtime-decompiler-*.jar /B /S') do set JRD=%%i

popd

rem Maps an available drive letter if script is located on a network drive, does pretty much nothing otherwise - done to counteract CMD not supporting UNC paths
pushd %PORTABLE_JRD_HOME%

rem Create environment variable pointing to script's location
set "PROPERTY_PURPOSE=-Djrd.purpose=%PURPOSE%"
set "PROPERTY_LOCATION=-Djrd.location=%CD%"

rem Concatenate classpath and launch the app
set CLASSPATH=%TOOLS%;%RSYNTAXTEXTAREA%;%GSON%;%BYTEMAN%;%JRD%
"%JDK_LOCATION%\bin\java.exe" %PROPERTY_LOCATION% %PROPERTY_PURPOSE% -cp %CLASSPATH% org.jrd.backend.data.Main

popd