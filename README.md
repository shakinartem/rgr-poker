# Texas Hold'em RGR on Java

Учебный проект: клиент-серверная игра в техасский холдем на `Java`.

В составе:
- `GameServer` - TCP сервер на сокетах
- `PokerClient` - графический клиент на `Swing`
- обмен сообщениями в `JSON`
- поддержка 2-4 игроков
- стадии `PRE_FLOP`, `FLOP`, `TURN`, `RIVER`, `SHOWDOWN`
- действия `Fold`, `Check`, `Call`, `Raise`, `All-in`
- таймаут хода и автоматический `Check/Fold`
- подсчёт комбинаций и раздача основного/побочного банка
- минимальный графический стол с PNG-картами-заглушками

## Структура

- `src/poker/common` - общие модели, JSON и оценка комбинаций
- `src/poker/server` - сервер, стол, игроки, игровой движок
- `src/poker/client` - сетевой клиент и `Swing` UI
- `assets/cards` - PNG-заглушки для карт

## Масти

- `H` - черви = `TOMATO`
- `D` - бубны = `CARROT`
- `S` - пики = `CUCUMBER`
- `C` - трефы = `EGGPLANT`

## Сборка

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName)
```

## Запуск сервера

```powershell
java -cp out poker.server.GameServer 5000
```

## Запуск клиента

```powershell
java -cp out poker.client.PokerClient
```

Можно открыть 2-4 клиента и подключиться к одному серверу.

## Правила имени игрока

- длина от 3 до 16 символов
- разрешены `A-Z`, `a-z`, `0-9`, `_`
- regex: `^[a-zA-Z0-9_]{3,16}$`

## Примечания

- стартовый стек игрока: `1000`
- блайнды: `10/20`
- лимит ожидания хода: `90` секунд
- UI сделан без внешних библиотек, чтобы проект собирался локально
