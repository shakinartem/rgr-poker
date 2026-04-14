@echo off
if not exist out mkdir out
javac -encoding UTF-8 -d out src\poker\common\*.java src\poker\server\*.java src\poker\client\*.java
if errorlevel 1 exit /b 1
java -cp out poker.client.PokerClient
