@echo off
cd /d "%~dp0"
cd ..
title Memory Game SERVER
echo Starting Server...
java -jar Server.jar
pause