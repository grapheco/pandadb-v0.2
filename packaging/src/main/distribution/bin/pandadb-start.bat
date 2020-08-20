@echo off
set base=%PANDADB_HOME%
set class_path=%base%/lib/packaging-0.2.0-shaded.jar
set MainClass="org.neo4j.server.CommunityEntryPoint"

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

start java -jar %class_path% -server --home-dir=%PANDADB_HOME% --config-dir=%PANDADB_HOME%/conf
Pause
exit