@echo off
cd /d "%~dp0.."
gradlew.bat build
gradlew.bat runClient