# Documentație Tehnică Proiect PPOO — Tema 4: Multimedia File Manager (Gestiunea Fișierelor Multimedia)

---

## 1. Descrierea problemei și obiectivele proiectului (`Tema 4`)

Proiectul **Multimedia File Manager (`Tema 4`)** reprezintă o aplicație de consolă dezvoltată în **Java 21** (fără dependențe externe la rulare, utilizând exclusiv pachetele standard `java.nio.file`, `java.io` și `java.util`), destinată monitorizării mai multor directoare de pe disc și gestiunii centralizate a fișierelor multimedia:
- **Fișiere audio**: `.mp3`, `.wav` (`MediaType.AUDIO`);
- **Fișiere imagine**: `.jpg`, `.png` (`MediaType.IMAGE`).

### Obiective principale
1. **Monitorizarea locațiilor de pe disc (`locations.txt`)**: Păstrarea unei liste de directoare monitorizate într-un fișier text în folderul aplicației, cu restaurarea automată la pornire și actualizarea automată la adăugarea/ștergerea unei locații sau la închiderea aplicației.
2. **Protecția fișierelor utilizatorului (`S3`)**: Eliminarea unui director din lista de monitorizare șterge **exclusiv referința** din memorie și din `locations.txt`, fără a șterge sau modifica vreodată fișierele sau directoarele fizice de pe disc.
3. **Gestiunea datelor în memorie (`G3`, `G4`)**: Stocarea și prelucrarea tuturor datelor active în colecții de obiecte proprii (`List<MonitoredDirectory>`, `Set<Path>`, `List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`) și în masive primitive unidimensionale și bidimensionale (`int[] countsByExtension`, `long[] bytesByExtension`, `long[][] directoryExtensionMatrix`).
4. **Persistență hibridă text și binară (`G1`, `G2`)**: Salvarea configurației în `locations.txt`, exportul rapoartelor statistice în `reports/multimedia-report.txt` și serializarea binară a catalogului și a masivelor statistice în `data/catalog.bin`.
5. **Validare riguroasă și meniu interactiv (`G5`–`G8`, `S4`–`S6`)**: Tratarea completă a situațiilor excepționale prin excepții standard și proprii, fără oprirea neașteptată a aplicației.

---

## 2. Matricea de trasabilitate a cerințelor generale (`G1`–`G9`) și specifice (`S1`–`S6`)

| ID Cerință | Descriere cerință (`general_req.txt` / `specif_req.txt`) | Pachete și clase responsabile | Mecanism de implementare |
| --- | --- | --- | --- |
| **G1** | Utilizarea fișierelor text și binare pentru salvarea/restaurarea datelor și generarea de statistici/rapoarte | `ro.ase.multimedia.storage` (`LocationRepository`, `ReportExporter`, `BinaryCatalogStorage`) | `locations.txt` (text UTF-8), `reports/multimedia-report.txt` (raport text UTF-8), `data/catalog.bin` (flux binar `DataOutputStream`/`DataInputStream`) |
| **G2** | Salvarea datelor la închidere și restaurarea la deschidere, cu actualizarea fișierului de intrare | `MultimediaCatalog`, `LocationRepository`, `BinaryCatalogStorage`, `Main` | `initialize()` restaurează căile din `locations.txt` și scanează discul; `saveAllOnExit()` salvează `locations.txt` și `data/catalog.bin` |
| **G3** | Minimum două masive 1D/2D de tipuri fundamentale și două colecții diferite de obiecte proprii | `StatisticsCalculator`, `MultimediaCatalog` | Masive 1D: `int[] countsByExtension`, `long[] bytesByExtension`; Masiv 2D: `long[][] directoryExtensionMatrix`; Colecții: `List<MonitoredDirectory>`, `Set<Path>`, `List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>` |
| **G4** | Efectuarea tuturor modificărilor în memorie în masive/colecții și salvarea la ieșire | `MultimediaCatalog` | Operațiile `addDirectory`, `removeDirectory`, `rescan` actualizează colecțiile și recalculează masivele primitive în memorie înainte de persistență |
| **G5** | Validarea datelor de la consolă și din fișiere prin excepții standard și proprii | `ro.ase.multimedia.exception` (`MultimediaException` și subclasele), `ConsoleMenu` | Ierarhie de 5 excepții proprii + `IllegalArgumentException`, `NumberFormatException`, `IOException` |
| **G6** | Flux funcțional logic de la pornire și încărcare până la închiderea aplicației | `Main`, `ConsoleMenu`, `MultimediaCatalog` | Pornire -> încărcare `locations.txt` -> clasificare stare directoare -> scanare recursivă -> calcul masive 1D/2D -> buclă meniu -> salvare la ieșire |
| **G7** | Meniu interactiv clar pentru apelarea funcționalităților | `ro.ase.multimedia.ui.ConsoleMenu` | Meniu numerotat (`1`–`6` și `0` pentru ieșire), sub-meniu de filtrare (`1`–`3`) și mesaje clare `[WARN]` / `[ERROR]` |
| **G8** | Proiect complet funcțional, fără erori de compilare sau execuție | Întregul proiect + `TestRunner` | Compilare cu `javac -Xlint:all -Werror`, verificare `javadoc -Xdoclint:all -Werror` și suită automată de teste (`T1`–`T37`) |
| **G9** | Documentație Word + JavaDoc conform indicațiilor de la curs și seminar | `docs/PROJECT_DOCUMENTATION.md`, `scripts/javadoc.sh`, `package-info.java` | JavaDoc complet pe toate cele 6 pachete și 17 clase/enum-uri + prezentul document tehnic pregătit pentru export `.docx` |
| **S1, S2** | Vizualizarea, adăugarea și ștergerea directoarelor monitorizate păstrate în `locations.txt` | `LocationRepository`, `MultimediaCatalog`, `ConsoleMenu` | Opțiunile `1`, `2`, `3` din meniu sincronizează automat colecția din memorie și fișierul `locations.txt` |
| **S3** | Ștergerea unei locații elimină doar referința, păstrând intacte fișierele de pe disc | `MultimediaCatalog.removeDirectory()` | Elimină intrarea din `List<MonitoredDirectory>` și `locations.txt`; nu apelează niciodată `Files.delete` pe directoarele utilizatorului |
| **S4** | Identificarea, afișarea și filtrarea fișierelor `.mp3`, `.wav`, `.jpg`, `.png` (nume, extensie, tip, dimensiune, locație) | `MediaType`, `MultimediaFile`, `MultimediaScanner`, `MultimediaCatalog` | Scanare recursivă `Files.walkFileTree`, afișare completă (Opțiunea `4`) și filtrare după tip, extensie sau cuvânt cheie (Opțiunea `5`) |
| **S5** | Meniu în consolă pentru gestiunea locațiilor, listare, filtrare/căutare, statistici și ieșire | `ConsoleMenu` | Opțiunile `1`–`6` și `0` acoperă integral cerințele funcționale |
| **S6** | Tratarea erorilor pentru căi invalide, probleme la `locations.txt` și directoare șterse/inaccesibile | `DirectoryStatus`, `LocationRepository`, `MultimediaScanner`, `ConsoleMenu` | Marcarea directoarelor ca `ACCESSIBLE`, `MISSING` sau `INACCESSIBLE` și raportarea avertismentelor fără oprirea execuției |

---

## 3. Arhitectura aplicației și documentația JavaDoc pe pachete și clase

Codul sursă este organizat în **6 pachete** sub rădăcina `ro.ase.multimedia`, respectând principiul responsabilității unice (`JAVA-5`) și separarea logicii de domeniu față de I/O (`JAVA-7`). Fiecare pachet include un fișier `package-info.java` documentat JavaDoc.

### 3.1. Pachetele aplicației (6 pachete)

| # | Pachet | Fișier `package-info.java` | Responsabilitate |
| --- | --- | --- | --- |
| 1 | `ro.ase.multimedia` | `src/main/java/ro/ase/multimedia/package-info.java` | Punctul de intrare în aplicație (`Main`) și conectarea straturilor `storage`, `service` și `ui` |
| 2 | `ro.ase.multimedia.model` | `src/main/java/ro/ase/multimedia/model/package-info.java` | Entități de domeniu imutabile și enumerări (`MediaType`, `DirectoryStatus`, `MonitoredDirectory`, `MultimediaFile`) |
| 3 | `ro.ase.multimedia.exception` | `src/main/java/ro/ase/multimedia/exception/package-info.java` | Ierarhia de excepții proprii (`MultimediaException` și cele 4 subclase specializate) |
| 4 | `ro.ase.multimedia.storage` | `src/main/java/ro/ase/multimedia/storage/package-info.java` | Adaptoare de persistență în fișiere text și binare (`LocationRepository`, `ReportExporter`, `BinaryCatalogStorage`) |
| 5 | `ro.ase.multimedia.service` | `src/main/java/ro/ase/multimedia/service/package-info.java` | Servicii de scanare recursivă, gestiune catalog în memorie și calcul statistic în masive 1D/2D (`MultimediaScanner`, `StatisticsCalculator`, `MultimediaCatalog`) |
| 6 | `ro.ase.multimedia.ui` | `src/main/java/ro/ase/multimedia/ui/package-info.java` | Interfața utilizator interactivă în consolă (`ConsoleMenu`) cu fluxuri `InputStream`/`PrintStream` injectabile |

### 3.2. Clasele și enumerările aplicației (17 clase și enum-uri de nivel superior)

| # | Clasă / Enum | Pachet | Rol și metode principale documentate JavaDoc |
| --- | --- | --- | --- |
| 1 | `Main` | `ro.ase.multimedia` | Punctul de intrare: `main(String[] args)` și `run(Path locationsFile, InputStream in, PrintStream out)` |
| 2 | `MediaType` | `ro.ase.multimedia.model` | Enum (`AUDIO`, `IMAGE`) cu lista ordonată de extensii suportate (`SUPPORTED_EXTENSIONS`: `.mp3`, `.wav`, `.jpg`, `.png`), `fromExtension(String)`, `fromFileName(String)`, `normalizeExtension(String)` |
| 3 | `DirectoryStatus` | `ro.ase.multimedia.model` | Enum (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`) care descrie starea pe disc a unui director monitorizat |
| 4 | `MonitoredDirectory` | `ro.ase.multimedia.model` | Clasă imutabilă pentru o locație monitorizată (`path`, `status`, `addedAt`), cu metoda `withStatus(DirectoryStatus)` |
| 5 | `MultimediaFile` | `ro.ase.multimedia.model` | Clasă imutabilă pentru un fișier multimedia descoperit (`name`, `extension`, `mediaType`, `sizeBytes`, `directoryPath`, `fullPath`) |
| 6 | `MultimediaException` | `ro.ase.multimedia.exception` | Clasă de bază (`extends RuntimeException`) pentru toate excepțiile specifice domeniului |
| 7 | `InvalidDirectoryException` | `ro.ase.multimedia.exception` | Aruncată când calea introdusă este nulă, vidă, sintactic invalidă, inexistentă sau nu este director |
| 8 | `DuplicateLocationException` | `ro.ase.multimedia.exception` | Aruncată la încercarea de adăugare a unui director deja prezent în catalog |
| 9 | `LocationNotFoundException` | `ro.ase.multimedia.exception` | Aruncată la încercarea de eliminare a unui director sau index inexistent în catalog |
| 10 | `StorageOperationException` | `ro.ase.multimedia.exception` | Aruncată la erori de citire/scriere pentru `locations.txt`, `multimedia-report.txt` sau `catalog.bin` |
| 11 | `LocationRepository` | `ro.ase.multimedia.storage` | Gestionează citirea (`load()` -> `LoadResult`) și salvarea atomică (`save(List<Path>)`) a fișierului `locations.txt` |
| 12 | `BinaryCatalogStorage` | `ro.ase.multimedia.storage` | Salvează (`save(...)`) și restaurează (`load()` -> `Optional<CatalogSnapshot>`) starea binară în `data/catalog.bin` |
| 13 | `ReportExporter` | `ro.ase.multimedia.storage` | Exportă raportul statistic text (`export(...)`) în `reports/multimedia-report.txt` |
| 14 | `MultimediaScanner` | `ro.ase.multimedia.service` | Parcurge recursiv directoarele (`scanDirectory(Path)` -> `ScanResult`) prin `Files.walkFileTree` fără a urmări symlink-uri |
| 15 | `StatisticsCalculator` | `ro.ase.multimedia.service` | Calculează masivele primitive 1D și 2D (`calculate(...)` -> `StatisticsSnapshot`) cu copii defensive |
| 16 | `MultimediaCatalog` | `ro.ase.multimedia.service` | Coordonează colecțiile din memorie, scanarea, filtrarea (`filterByMediaType`, `filterByExtension`, `searchByName`), statisticile și persistența |
| 17 | `ConsoleMenu` | `ro.ase.multimedia.ui` | Gestionează bucla interactivă a meniului (`run()`), citirea comenzilor, formatarea tabelelor și afișarea erorilor |

---

## 4. Structuri de date în memorie (`G3`, `G4`)

Conform cerinței generale `G3`, aplicația utilizează simultan masive primitive unidimensionale și bidimensionale și colecții distincte de obiecte proprii:

### 4.1. Masive primitive 1D și 2D (`StatisticsCalculator.StatisticsSnapshot`)
Ordinea extensiilor este fixată prin constanta `MediaType.SUPPORTED_EXTENSIONS`: index `0` = `.mp3`, index `1` = `.wav`, index `2` = `.jpg`, index `3` = `.png`.
1. **`int[] countsByExtension` (masiv 1D de tip `int`, lungime 4)**: reține numărul total de fișiere descoperite pentru fiecare extensie suportată (`countsByExtension[extIndex]`).
2. **`long[] bytesByExtension` (masiv 1D de tip `long`, lungime 4)**: reține dimensiunea cumulată în octeți (bytes) a fișierelor pentru fiecare extensie (`bytesByExtension[extIndex]`).
3. **`long[][] directoryExtensionMatrix` (masiv 2D / matrice de tip `long`, dimensiune `[numărDirectoare][4]`)**: elementul `directoryExtensionMatrix[dirIndex][extIndex]` stochează numărul de fișiere având extensia `extIndex` aflate în directorul monitorizat de pe poziția `dirIndex`.

Toate metodele getter din `StatisticsSnapshot` returnează clone defensive (`clone()` pe fiecare rând al matricei 2D), garantând imutabilitatea stării interne (`JAVA-2`).

### 4.2. Colecții de obiecte proprii (`MultimediaCatalog`)
1. **`List<MonitoredDirectory> monitoredDirectories` (`ArrayList`)**: păstrează ordinea deterministă de inserare a directoarelor monitorizate, utilizată la afișarea indexată (`[1]`, `[2]`, ...) și la indexarea liniilor din matricea 2D `directoryExtensionMatrix`.
2. **`Set<Path> monitoredPathsIndex` (`LinkedHashSet`)**: index secundar de căi normalizate care asigură verificarea duplicatelor în timp constant $O(1)$.
3. **`List<MultimediaFile> multimediaFiles` (`ArrayList`)**: colecția ordonată alfabetic după `fullPath` a tuturor fișierelor multimedia descoperite, deduplicată în cazul directoarelor părinte-copil suprapuse.
4. **`Map<MediaType, List<MultimediaFile>> filesByMediaType` (`EnumMap`)**: dicționar care grupează fișierele după `MediaType.AUDIO` și `MediaType.IMAGE` pentru filtrare rapidă.

---

## 5. Persistența datelor în fișiere text și binare (`G1`, `G2`, `S1`–`S3`)

Toate scrierile pe disc folosesc un fișier temporar în același director (`.tmp`) urmat de redenumire atomică (`Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)` cu fallback la `REPLACE_EXISTING`), prevenind coruperea fișierelor în caz de întrerupere.

### 5.1. Fișierul text de configurare `locations.txt` (`LocationRepository`)
- Codificare: UTF-8, câte o cale absolută normalizată pe linie.
- La pornire, dacă `locations.txt` nu există, este creat automat ca fișier vid.
- Liniile goale sau comentariile (`#`) sunt ignorate; liniile malformate generează avertismente non-fatale în `LoadResult.warnings()`.

### 5.2. Raportul statistic text `reports/multimedia-report.txt` (`ReportExporter`)
- Conține: antetul raportului, data generării (UTC), totalurile generale (număr directoare, număr fișiere, dimensiune totală în bytes), statisticile 1D pe fiecare extensie (`.mp3`, `.wav`, `.jpg`, `.png`), matricea 2D director-pe-extensie, lista directoarelor monitorizate și inventarul complet al fișierelor.

### 5.3. Fișierul binar de stare `data/catalog.bin` (`BinaryCatalogStorage`)
Serializat cu `DataOutputStream` și citit cu `DataInputStream`:
- **Antet Magic (`int`)**: `0x4D464D43` (ASCII `"MFMC"` — Multimedia File Manager Catalog);
- **Versiune format (`int`)**: `1`;
- **Secțiune Directoare**: număr directoare (`int`), urmat pentru fiecare director de `path` (`UTF`), `status` (`UTF`) și `addedAt` (`UTF`);
- **Secțiune Fișiere**: număr fișiere (`int`), urmat pentru fiecare fișier de `name`, `extension`, `mediaType`, `sizeBytes` (`long`), `directoryPath` și `fullPath`;
- **Secțiune Masive Primitive 1D/2D**: lungime extensii (`int`), elementele `int[] countsByExtension`, elementele `long[] bytesByExtension`, dimensiunile matricei 2D (`rows`, `cols`) și elementele `long[][] directoryExtensionMatrix`.

Orice fișier trunchiat sau cu antet invalid aruncă `StorageOperationException`.

---

## 6. Validarea datelor și ierarhia de excepții (`G5`, `S6`)

Aplicația validează atât datele citite de la tastatură, cât și datele încărcate din fișiere:
- **`MultimediaException`** (bază):
  - **`InvalidDirectoryException`**: cale nulă/vidă, caractere ilegale (`InvalidPathException`), cale inexistentă pe disc la adăugare (`!Files.exists(path)`) sau cale care indică un fișier obișnuit în loc de director (`!Files.isDirectory(path)`).
  - **`DuplicateLocationException`**: directorul (după `toAbsolutePath().normalize()`) este deja monitorizat.
  - **`LocationNotFoundException`**: calea sau indexul numeric introdus la ștergere nu există în catalog.
  - **`StorageOperationException`**: erori I/O la citirea/scrierea `locations.txt`, `multimedia-report.txt` sau `catalog.bin`, ori format binar corupt.
- **Tratarea directoarelor șterse extern (`S6`)**: dacă un director prezent în `locations.txt` a fost șters de pe disc între sesiuni, `MultimediaCatalog.initialize()` îl clasifică cu starea `DirectoryStatus.MISSING` (sau `INACCESSIBLE` dacă nu are drepturi de citire) și emite un avertisment `[WARN]`, fără a bloca pornirea aplicației.

---

## 7. Fluxul funcțional, meniul interactiv și scenariu de utilizare / testare (`G6`–`G8`, `S4`, `S5`)

La lansarea aplicației (`./scripts/run.sh`), `Main` inițializează `MultimediaCatalog`, încarcă `locations.txt`, scanează directoarele accesibile, calculează masivele primitive 1D/2D și afișează meniul principal din `ConsoleMenu`:

```text
=== Multimedia File Manager ===
1 - View monitored directories
2 - Add monitored directory
3 - Remove monitored directory (keeps disk files intact)
4 - Display all multimedia files
5 - Filter / search multimedia files
6 - View statistics and export report / binary snapshot
0 - Save state and exit
Select option:
```

### Descrierea opțiunilor meniului
- **Opțiunea `1` (Vizualizare directoare)**: afișează lista indexată de la `1` a directoarelor monitorizate, starea pe disc (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`), numărul de fișiere multimedia și momentul adăugării.
- **Opțiunea `2` (Adăugare director)**: citește calea, validează existența și tipul de director, adaugă locația în memorie, actualizează `locations.txt`, rescanează fișierele și recalculează masivele 1D și 2D.
- **Opțiunea `3` (Ștergere director)**: permite eliminarea fie după **indexul afișat** (`1..N`), fie după **calea completă**. Elimină doar referința din catalog și din `locations.txt`, lăsând fișierele de pe disc neatinse (`S3`).
- **Opțiunea `4` (Afișare fișiere multimedia)**: afișează toate fișierele `.mp3`, `.wav`, `.jpg`, `.png` cu nume, extensie, `MediaType`, dimensiune formatată (`B`/`KB`/`MB` și octeți exacți) și cale.
- **Opțiunea `5` (Filtrare și căutare)**: deschide sub-meniul:
  - `1` — Filtrare după tip media (`AUDIO` sau `IMAGE`);
  - `2` — Filtrare după extensie (`.mp3`, `.wav`, `.jpg`, `.png`, insensibil la majuscule);
  - `3` — Căutare după subșir în numele fișierului (insensibil la majuscule).
- **Opțiunea `6` (Statistici și export)**: afișează în consolă totalurile generale, vectorii 1D (`countsByExtension`, `bytesByExtension`) și matricea 2D (`directoryExtensionMatrix`), apoi exportă `reports/multimedia-report.txt` și `data/catalog.bin`.
- **Opțiunea `0` (Salvare și ieșire)**: salvează starea curentă în `locations.txt` și `data/catalog.bin` și închide aplicația.

### Verificare automată și manuală
- **Teste automate (`./scripts/test.sh`)**: rulează suita `ro.ase.multimedia.TestRunner` (`T1`–`T37`) în directoare temporare izolate, acoperind toate cerințele `G1`–`G9` și `S1`–`S6`.
- **Verificare manuală (`README.md`)**: secțiunea `Manual Console Verification` din `README.md` oferă comenzile pas cu pas pentru testarea tuturor opțiunilor `1`–`6` și `0`.

---

## 8. Generarea documentației JavaDoc (`./scripts/javadoc.sh`) și exportul în Microsoft Word (`.docx`)

### 8.1. Generarea documentației HTML JavaDoc
Din rădăcina depozitului Git, rularea scriptului:

```bash
./scripts/javadoc.sh
```

compilează proiectul și execută `javadoc -encoding UTF-8 -Xdoclint:all -Werror` peste toate cele 6 pachete și 17 clase/enum-uri din `project_ppoo/src/main/java`, generând documentația HTML completă în directorul `docs/javadoc/` (cu pagina principală `docs/javadoc/index.html`).

### 8.2. Exportul documentației tehnice în format Microsoft Word (`.docx`)
Prezentul fișier (`docs/PROJECT_DOCUMENTATION.md`) este formatat cu secțiuni numerotate și tabele compatibile cu **Microsoft Word** și **Pandoc** (`G9`). Pentru a obține documentul `.docx`:

1. **Conversie directă cu Pandoc (recomandat)**:
   ```bash
   pandoc docs/PROJECT_DOCUMENTATION.md -o Tema4_Documentatie.docx
   ```
2. **Deschidere directă în Microsoft Word**:
   - Deschideți Microsoft Word -> **File -> Open** și selectați `docs/PROJECT_DOCUMENTATION.md` (sau copiați conținutul formatat în Word) și salvați ca **Word Document (`*.docx`)**.
