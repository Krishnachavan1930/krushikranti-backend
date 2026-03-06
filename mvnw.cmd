@echo off
setlocal

set MAVEN_WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar
set MAVEN_WRAPPER_PROPERTIES=%~dp0.mvn\wrapper\maven-wrapper.properties

for /f "usebackq tokens=1,2 delims==" %%a in ("%MAVEN_WRAPPER_PROPERTIES%") do (
    if "%%a"=="distributionUrl" set DISTRIBUTION_URL=%%b
)

if exist "%MAVEN_WRAPPER_JAR%" (
    goto runWrapper
)

echo Downloading Maven Wrapper JAR...
powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%MAVEN_WRAPPER_JAR%'"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to download Maven Wrapper JAR.
    exit /b 1
)

:runWrapper
java -jar "%MAVEN_WRAPPER_JAR%" %*

endlocal
