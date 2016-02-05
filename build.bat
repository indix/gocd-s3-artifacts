WHERE choco
IF %ERRORLEVEL% NEQ 0 %PS% -NoProfile -ExecutionPolicy unrestricted -Command "iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))" && SET PATH=%PATH%;%systemdrive%\ProgramData\chocolatey\bin
choco install sbt --acceptlicense -y
WHERE sbt
IF %ERRORLEVEL% NEQ 0 SET PATH=%PATH%;%systemdrive%\Program Files (x86)\sbt\bin
choco install "\\columbus\sw_share\DEPARTMENT\SD Software\Java\jdk8.8.0.66.nupkg" --acceptlicense -y

WHERE sbt
IF %ERRORLEVEL% EQU 0 GOTO sbtgood:
echo ERROR: cannot find sbt
exit /b 1

:sbtgood
WHERE java
IF %ERRORLEVEL% NEQ 0 SET PATH=%PATH%;%systemdrive%\Program Files\Java\jdk1.8.0_66\bin

WHERE java
IF %ERRORLEVEL% EQU 0 GOTO javagood:
echo ERROR: cannot find java
exit /b 1

:javagood
call sbt clean editsource:edit assembly

md Deploy
copy .\fetch\target\s3fetch-*.jar .\Deploy
copy .\material\target\s3material-*.jar .\Deploy
copy .\publish\target\s3publish-*.jar .\Deploy