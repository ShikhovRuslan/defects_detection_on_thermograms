@rem Извлекает из всех термограмм из папки DIR_THERMOGRAMS необработанные температурные данные в файлы в папке
@rem .\SUBDIR_RAW_TEMPS.

@echo off

for /f "tokens=2 delims==" %%i in ('findstr "^DIR_THERMOGRAMS" config.txt') do set DIR_THERMOGRAMS=%%i
set DIR_THERMOGRAMS="%DIR_THERMOGRAMS:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^SUBDIR_RAW_TEMPS" config.txt') do set SUBDIR_RAW_TEMPS=%%i
set SUBDIR_RAW_TEMPS=%SUBDIR_RAW_TEMPS:~1%


for %%i in (%DIR_THERMOGRAMS%\*) do raw.bat "%%i" ".\%SUBDIR_RAW_TEMPS%"