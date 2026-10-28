@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0gradlew.bat" %*
