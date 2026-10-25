@echo off
setlocal
cd /d "%~dp0.."
if not exist verification\classes mkdir verification\classes
javac -encoding UTF-8 -cp "dist\TCPServer.jar;dist\TCPClient.jar" -d verification\classes verification\ProjectCheck.java
if errorlevel 1 exit /b 1
java -cp "verification\classes;dist\TCPServer.jar;dist\TCPClient.jar" ProjectCheck
