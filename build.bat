WHERE choco
IF %ERRORLEVEL% NEQ 0 %PS% -NoProfile -ExecutionPolicy unrestricted -Command "iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))" && SET PATH=%PATH%;%systemdrive%\ProgramData\chocolatey\bin
choco install sbt --acceptlicense -y
WHERE sbt
IF %ERRORLEVEL% NEQ 0 SET PATH=%PATH%;%systemdrive%\Program Files (x86)\sbt\bin
choco install "\\columbus\sw_share\DEPARTMENT\SD Software\Java\jdk8.8.0.66.nupkg" --acceptlicense -y --force
