@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%i in ("%SCRIPT_DIR%..") do set "ROOT_DIR=%%~fi"
set "JAVA_BIN=java"
set "JAR_PATH="

if defined JAVA_HOME (
  set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
)

if exist "%ROOT_DIR%\project-navigator-0.1.0-SNAPSHOT.jar" (
  set "JAR_PATH=%ROOT_DIR%\project-navigator-0.1.0-SNAPSHOT.jar"
)

if not defined JAR_PATH if exist "%ROOT_DIR%\target\project-navigator-0.1.0-SNAPSHOT.jar" (
  set "JAR_PATH=%ROOT_DIR%\target\project-navigator-0.1.0-SNAPSHOT.jar"
)

if not defined JAR_PATH (
  echo Jar not found.
  echo Expected one of:
  echo   %ROOT_DIR%\project-navigator-0.1.0-SNAPSHOT.jar
  echo   %ROOT_DIR%\target\project-navigator-0.1.0-SNAPSHOT.jar
  echo If you are in the repo, build it first with: mvn -DskipTests package
  exit /b 1
)

for /f "tokens=3 delims=.\" %%v in ('"%JAVA_BIN%" -version 2^>^&1 ^| findstr /i "version"') do (
  set JAVA_MAJOR=%%v
  goto :version_checked
)

:version_checked
if not defined JAVA_MAJOR (
  echo Java 17+ is required.
  echo Could not detect the current Java version.
  exit /b 1
)

if %JAVA_MAJOR% LSS 17 (
  echo Java 17+ is required.
  "%JAVA_BIN%" -version
  exit /b 1
)

"%JAVA_BIN%" -jar "%JAR_PATH%"
