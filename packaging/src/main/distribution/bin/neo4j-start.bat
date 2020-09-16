@echo off
echo batchfile=%0
echo full=%~f0
setlocal
  for %%d in (%~dp0.) do set Directory=%%~fd
  echo Directory=%Directory%
  for %%d in (%~dp0..) do set ParentDirectory=%%~fd
  echo ParentDirectory=%ParentDirectory%
  set class_path=%ParentDirectory%\lib\pandadb-server-all-in-one-0.2.jar
  set MainClass="org.neo4j.server.CommunityEntryPoint"
  set config=%ParentDirectory%\conf
  set log_path=%ParentDirectory%\logs\neo4j.log
  echo Starting Pandadb...

  for /f "tokens=1,2 delims==" %%a in (%config%) do (
    if "%%a"=="dbms.connector.bolt.listen_address" set bolt=%%b
  )
  echo Bolt enabled on %bolt%
  echo Started

  for /f "tokens=1,2 delims==" %%a in (%config%) do (
    if "%%a"=="dbms.connector.http.listen_address" set http=%%b
  )
  echo Remote interface available at %http%
  echo There may be a short delay until the server is ready.
  echo See %log_path% for current status.

  start java -jar %class_path% -server --home-dir=%ParentDirectory% --config-dir=%config%
endlocal
Pause
exit