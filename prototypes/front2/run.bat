@echo off
chcp 65001 >nul
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
    echo Java JDK 17 or newer is required. javac was not found.
    pause
    exit /b 1
)
if not exist build mkdir build
javac -encoding UTF-8 -d build src\com\oopteama\chess\ui\*.java src\com\oopteama\chess\demo\*.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
java -cp build com.oopteama.chess.demo.Front2Demo
if errorlevel 1 pause
