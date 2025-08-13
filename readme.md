
# Fix my track

Утилита для фильтрации фейковых gps данных в файлах записи физической активности(fit, gpx)


## Установка и использование

Онлайн сервис развернут по адресу (вставить ссылку)

Для локального запуска скачайте последний собранный релиз. (вставить ссылку)

1) Для обработки fit файла:
```bash
  java -jar fixmytrack.jar inputfile.fit outputfile.fit
```
где inputfile.fit - путь к обрабатываемому файлу, 
а outputfile.fit - путь для сохранения обработанного трека.

2) Для обработки gpx файла:
```bash
  java -jar fixmytrack.jar inputfile.gpx outputfile.gpx
```
где inputfile.gpx - путь к обрабатываемому файлу, 
а outputfile.gpx - путь для сохранения обработанного трека.

3) запуск в режиме Вебсервера:
просто запустите файл без параметров
```bash
  java -jar fixmytrack.jar
```
и в браузере откройте страницу http://127.0.0.1/

## License

[MIT](https://choosealicense.com/licenses/mit/)