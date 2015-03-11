del iNotesMonitor.sqlite3
@echo ""
@echo Deleted existing SQLITE database
@pause

"C:\Program Files\Utilities\sqlite3.exe" -init "..\src\main\resources\scripts\schema.sql" iNotesMonitor.sqlite3  ""
@echo ""
@echo Regenerated SQLITE database from schema.sql
@pause

rmdir /s/q com
@echo ""
@echo Deleted old generated code
@pause

java -cp "../../ThirdParty/SQLite/sqlite-jdbc-3.8.7.jar;../../ThirdParty/JOOQ-3.4.2/jOOQ-lib/*;./jOOQ-CamelCaseGeneratorStrategy.jar;." org.jooq.util.GenerationTool /library.xml
@echo ""
@echo Generated fresh code
@pause