# Defects Detection of Heating Systems on Thermograms

Репозиторий содержит программу, которая выделяет дефекты (зоны разрушения теплоизоляционного слоя) на термограммах, сделанных БПЛА, а также производит подсчёт их площади. 

Чтобы произвести расчёты, необходимо сначала запустить ряд скриптов, а затем выполнить непосредственно расчёты. Скрипты извлекают из термограмм различные EXIF-параметры и 
необработанные температурные данные. Скрипты самодостаточны, их можно использовать независимо от программы.

## Using the Sample Code

Все скрипты необходимо запускать из директории, содержащей файл `config.txt`. Кроме этого, перед запуском `thermograms_raw_temperatures.bat` нужно убедиться, что упомянутая выше 
директория содержит скрипт `raw.bat` и папку `SUBDIR_RAW_TEMPS`.
Файл `config.txt` содержит различные параметры, в том числе упомянутые ниже `DIR_THERMOGRAMS`, `SUBDIR_OUTPUT`, `SUBDIR_DEFECTS`, `SUBDIR_RAW_TEMPS`, `SUBDIR_REAL_TEMPS`, 
`POSTFIX_RAW_TEMPS`.
```
Usage: global_params.bat

Извлекает общие для всех термограмм параметры: Focal Length, Planck R1, Planck R2, Planck O, Planck B, 
Planck F, emissivity, Reflected Apparent Temperature, Raw Thermal Image Height, Raw Thermal Image Width 
- из данных EXIF первой термограммы из папки DIR_THERMOGRAMS в файл .\SUBDIR_OUTPUT\global_params.txt в 
формате JSON.
```

```
Usage: thermograms_info.bat

Извлекает параметры термограмм из папки DIR_THERMOGRAMS: Relative Altitude, Gimbal Yaw Degree,
GPS Latitude, GPS Longitude - из данных EXIF в файл .\SUBDIR_OUTPUT\thermograms_info.txt в виде массива JSON.
```

```
Usage: thermograms_raw_temperatures.bat

Извлекает из всех термограмм из папки DIR_THERMOGRAMS необработанные температурные данные в файлы в папке
.\SUBDIR_RAW_TEMPS.
```

```
Usage: raw.bat %1 %2

Извлекает из указанной термограммы необработанные температурные данные в файл 
<thermogram_name>POSTFIX_RAW_TEMPS.pgm в указанной папке.

  %1         термограмма
  %2         папка, содержащая файл с необработанными температурными данными термограммы
```

## Запуск из командной строки
* Клонировать репозиторий:
```
> git clone https://github.com/ShikhovRuslan/defects_detection_on_thermograms.git
```
* В рабочем каталоге должны находиться скрипты (с расширением `.bat` для Windows и `.sh` для Linux), файлы `config.txt` и `help.txt`.

* Запустить файл geo.jar из вышеуказанного каталога с первыми трёмя параметрами и, дождавшись окончания работы скриптов, последовательно запустить с оставшимися двумя 
параметрами.
```
Usage: java -jar geo.jar [-gp | -ti | -trt | -csv | -d]

  -gp        Запуск global_params.bat.
  -ti        Запуск thermograms_info.bat.
  -trt       Запуск thermograms_raw_temperatures.bat.
  -csv       Для каждой термограммы из папки DIR_THERMOGRAMS конвертирует файл с необработанными температурными данными 
             из папки .\SUBDIR_RAW_TEMPS в файл с температурами в формате CSV в папке .\SUBDIR_REAL_TEMPS.
  -d         Для каждой термограммы из файла .\SUBDIR_OUTPUT\thermograms_info.txt создаёт изображение с выделенными 
             дефектами в папке .\SUBDIR_DEFECTS.
```

* Картинки с выделенными дефектами сохраняются в папке `.\SUBDIR_DEFECTS`, а расчёты - в папке `.\SUBDIR_OUTPUT`.

## Результаты
