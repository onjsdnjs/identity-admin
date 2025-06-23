@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set OPENAI_API_KEY=dummy-key
set PATH=%JAVA_HOME%\bin;%PATH%
gradlew.bat bootRun