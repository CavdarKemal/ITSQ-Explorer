@ECHO OFF
REM TemplateGUI Startskript
REM Benoetigt Java 17 oder hoeher

REM Bestimme Java-Executable
IF DEFINED JAVA_HOME (
    SET JAVA_CMD="%JAVA_HOME%\bin\java.exe"
) ELSE (
    SET JAVA_CMD=java
    ECHO WARNUNG: JAVA_HOME ist nicht gesetzt. Verwende Java aus PATH.
)

REM Debug-Optionen (auskommentieren fuer Produktion)
REM SET DEBUG_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005

SET JVM_ARGS=-Dfile.encoding=UTF8 -Xms512m -Xmx2g -XX:+HeapDumpOnOutOfMemoryError -classpath ".;lib/*"
SET MAIN_CLASS=de.cavdar.gui.Main

ECHO Starting %MAIN_CLASS%...
ECHO Using Java: %JAVA_CMD%

%JAVA_CMD% %DEBUG_OPTS% %JVM_ARGS% %MAIN_CLASS% %CONFIG_FILE%

IF ERRORLEVEL 1 (
  ECHO.
  ECHO Fehler beim Starten! Druecken Sie eine Taste...
  PAUSE >nul
)
