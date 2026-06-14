@echo off
setlocal
set APP_HOME=%~dp0
set GRADLE_VERSION=8.2.1
set GRADLE_BIN=%APP_HOME%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin\gradle-%GRADLE_VERSION%\bin\gradle.bat

if not exist "%GRADLE_BIN%" (
  echo Gradle %GRADLE_VERSION% is not installed in .gradle\wrapper\dists.
  echo Run gradlew from Git Bash or install Gradle on PATH.
  exit /b 1
)

call "%GRADLE_BIN%" %*
