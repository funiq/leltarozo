Cartographia Leltározó
=========

Kisebb üzletek, raktárak alkalmi leltározásához készített grafikus felületű alkalmazás, első sorban nyomdaipari termékekre kihegyezve, de más termékekkel is használható. A leltározás során készülő naplófájlok [CSV](https://en.wikipedia.org/wiki/Comma-separated_values) (Comma Separater Values) formátumúak, így könnyedén behívhatóak Excelbe, vagy bármilyen táblázatkezelőbe.

Licenc
------------

Copyright © 2016; Báthory Péter <peter.bathory@cartographia.hu>, Cartographia Kft.
Ez a program szabad szoftver; terjeszthető illetve módosítható a Free Software Foundation által kiadott GNU General Public License dokumentumában leírtak; akár a licenc 2-es, akár (tetszőleges) későbbi változata szerint. Részletekért lásd a [LICENSE fájl](./LICENSE)

Használat
------------

A program grafikus felülete egy beviteli mezőből és egy a beolvasott termékeket listázó táblázatból áll. A beviteli mező univerzális, minden adat itt adható meg.

Egy vonalkód beolvasása (vagy kézi beírása) után a termék és adatai megjelennek a listában. A beviteli mezőbe számot írva módosítható a leltározott darabszám (alapérték 1), a kiadási év, vagy szöveget írva megjegyzés fűzhető hozzá. A program a beírt karakterek alapján dönti el, melyik műveletet kell alkalmazni. Újabb vonalkód beolvasásakor a legutóbbi sor kiíródik a naplófájlba, innentől nem módosítható.

### Egyéb tudnivalók
* Vonalkódokból a legalább 8, legfeljebb 14 számból állókat tudja kezelni. Ismeri az EAN, ISBN és GTIN szabványokat.
* A adatbázisfájlban egy vonalkód többször is szerepelhet (pl. speciális csomagolású termékek). Ilyen terméket beolvasva a program egy választóablakot jelenít meg (és figyelmeztető hangot ad)
* Minden ki és bemeneti fájl UTF8 kódolású
* Az adatbázisban nem szereplő termékek piros színnel jelennek meg a listában (és a program figyelmeztető hangot ad)
* A beviteli mezőben a termékek név szerint is kereshetőek. A találatok a egy legördülőlistában jelennek meg, egy találatra rákattintva az rögtön bekerül a táblázatba.
* A bejelentkező képernyőn található *Kimutatás készítésére* kattintva összefűzi a *log* könyvtár naplófájljait, valamint összesített kimutatást készít a leltározott termékekről (összeadja a darabszámokat és összehasonlítja a készlet szerintivel)

### Kimenet
A naplófájlok a .jar fájllal megegyező könyvtárban lévő *log* könyvtárban jönnek létre az alábbi séma szerint:
`dátum_kezelőNeve_helyszín.csv`
A naplófájl sorai az alábbi adatokat tartalmazzák, tabulátorral elválasztva:
`beolvasás időbélyegzője,	vonalkód,	leltározott darabszám,	megjegyzés,	kiadás dátuma,	helyszín,	kezelő neve,	cikkszám,	terméknév,	kiadó,	normalizált vonalkód`

Rendszerkövetelmények
------------

A program futtatásához Java 8 szükséges JavaFX támogatással. Windowson a JRE 8 alapértelmezetten tartalmazza a JavaFX könyvtárat. Linuxon külön kell telepíteni az `openjdk` csomagot.

A használathoz erősen ajánlott egy kézi vonalkódolvasó ami képes USB billentyűzetként üzemelni (a legtöbb készülék ezt támogatja).

Telepítés, futtatás
------------

1. Szerezd be a legfrissebb Leltározó.jar fájlt
2. Kattints duplán a .jar fájlra

Megjegyzés: Linuxon a .jar fájlt futtathatóvá kell tenni a `chmod u+x Leltározó.jar` paranccsal, vagy a .jar fájlra jobb gombbal kattintva a Tulajdonságok menüben (Jogosultságok / Fájl végrehajtásának engedélyezése programként)

Parancssorból:
```java -jar Leltározó.jar```

Konfigurálás
------------

### Adatbázis
A leltározást jelentősen megkönnyíti, ha a program ismeri a nyilvántartott készletet, a termékek nevével és egyéb adataival. Ehhez a .jar fájllal megegyező könyvtárban el kell helyezni egy CSV formátumú database.csv fájlt az alábbi adatokkal:
```Vonalkód; Termék neve; Kiadó; Készlet szerinti darabszám; Cikkszám```

A CSV lehet bármilyen formázású (vesszővel, pontosvesszővel, tabulátorral elválasztott), de az adatok a fenti sorrendben kell szerepeljenek. A vonalkód és a terméknév kötelező, a többi opcionális.

### Helyszínek

A leltározási helyszíneket a locations.txt fájlban lehet megadni, soronként egyet. Leltározásnál a kiválasztott helyszín minden naplófájlba bekerül.

Fejlesztés
------------
Forráskód letöltése, fordítás, futtatás:
```
git clone https://github.com/funiq/leltarozo.git
cd leltarozo
ant
```

Megjegyzés: A program elég sok, a saját leltározásunkhoz igazított megoldást tartalmaz. Ha szeretnél egy testre szabott leltározó programot, de nem értesz a programozáshoz, vedd fel a kapcsolatot fejlesztőinkkel: info@cartographia.hu

