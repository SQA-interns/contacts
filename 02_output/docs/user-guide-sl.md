# Navodila za uporabo: Skupine kontaktov

> Za: uporabnike (berejo) · Dostavljeno s produktom (NFR-15, DoD-P04) · Jezik: slovenščina · Vir: uporabniški vmesnik in tabela pravic specifikacije · Agent: piše

Skupine kontaktov je spletna aplikacija za hrambo kontaktov (oseb in organizacij) v **skupinah**. Skupina je namenjena enemu projektu ali podjetju. Kontakte skupine vidijo samo njeni člani. Vsako spremembo kontakta aplikacija zapiše v zgodovino.

## 1. Prvi koraki

1. **Registracija.** Na strani »Prijava« izberite »Registracija«, vpišite prikazno ime (drugi člani ga vidijo v zgodovini, do 100 znakov), e-pošto in geslo (vsaj 8 znakov), nato »Ustvari račun«. Prijavite se lahko tudi z Googlom (»Prijava z Googlom«).
2. **Potrditev e-pošte.** Na vaš naslov pride povezava. Dokler je ne odprete, aplikacija ne pokaže nobene skupine. Če povezave niste dobili, uporabite »Pošlji znova«.
3. **Pozabljeno geslo.** Na prijavi izberite »Pozabljeno geslo?« in vpišite e-pošto računa.
4. **Jezik.** Aplikacija je privzeto slovenska. Jezik zamenjate (slovenščina, English) z izbiro »Jezik«; izbira ostane.
5. **Odjava** in **Moj račun** najdete v uporabniškem meniju zgoraj.

## 2. Skupine in vloge

Po prijavi vidite »Moje skupine« z vlogo v vsaki. Novo skupino ustvarite z »Ustvari skupino«: vpišete le ime (do 100 znakov; dve vaši skupini ne smeta imeti istega imena, velike in male črke se ne razlikujejo). Po ustvaritvi se skupina odpre kot prazen seznam kontaktov.

Vloga v skupini določa, kaj smete:

| Dejanje | Lastnik | Urednik | Ogledovalec |
|---|---|---|---|
| Ogled kontaktov, iskanje, filtri, razvrščanje | da | da | da |
| Ogled članov skupine | da | da | da |
| Dodajanje in urejanje kontaktov, značk in atributov | da | da | ne |
| Preimenovanje in brisanje značk skupine | da | da | ne |
| Premik kontakta v koš, obnova iz koša | da | da | ne |
| Izvoz v Excel | da | da | ne |
| Zgodovina kontakta, dejavnost skupine, različice kontakta | da | da | ne |
| Iskanje in združevanje podvojenih kontaktov | da | da | ne |
| Trajni izbris kontakta iz koša | da | ne | ne |
| Povabila, vloge in odstranjevanje članov | da | ne | ne |
| Izbris skupine | da | ne | ne |

Skupina mora imeti vedno vsaj enega lastnika.

## 3. Kontakti

### Dodajanje

V skupini izberite »Dodaj kontakt«. Polja kontakta so: ime, priimek, e-pošta, telefon, podjetje, delovno mesto, ulica in hišna številka, poštna številka, kraj, država in opombe. Pravila:

- izpolnjeno mora biti vsaj eno od polj ime, priimek, e-pošta, podjetje;
- e-pošta mora imeti veljavno obliko (shrani se z malimi črkami);
- telefon sme imeti samo števke, presledke in znake `+ - ( ) . /`, največ 30 znakov;
- besedilna polja imajo do 255 znakov, opombe do 5.000;
- država mora biti s seznama držav.

Če kaj ni v redu, se ob polju pokaže sporočilo, nič se ne shrani in vpisano ostane v obrazcu. Presledki na začetku in koncu se odstranijo. Dva kontakta smeta imeti isto e-pošto.

### Značke in atributi

- **Značke** so oznake, po katerih kontakte filtrirate (na primer `udelezenec2025`). Ime ima 1 do 50 znakov: črke, števke, `-` in `_`, brez presledkov. Veliko in malo črko enači (`Gost` in `gost` je ista značka). Novo značko dodate na kontaktu z »Dodaj značko«.
- **Atributi** so poljubni pari ključ in vrednost (na primer `kompetenca` in `spletnoProgramiranje`). Ključ ima 1 do 50 znakov, vrednost ni prazna in ima do 255 znakov. Kontakt ima lahko največ 50 atributov, ključa ne smete podvojiti (velike in male črke se ne razlikujejo).
- Značke skupine preimenujete ali izbrišete v zavihku »Značke«. Preimenovanje spremeni značko na vseh kontaktih; z brisanjem se odstrani z vseh, kontakti ostanejo.

### Urejanje in hkratne spremembe

Spremembe shranite s »Shrani«; vsako shranjevanje poveča **različico** kontakta. Če je kontakt medtem spremenil nekdo drug, se spremembe ne shranijo: aplikacija sporoči, da je kontakt spremenil drug uporabnik, in pokaže trenutno stanje. Preglejte ga in spremembo ponovite.

### Zgodovina in različice

Zavihek »Zgodovina« kontakta kaže, kdo je kaj, kdaj in kako spremenil (staro in novo vrednost). Zavihek »Različice« kaže vsebino kontakta po vsakem shranjevanju. Različico lahko primerjate s trenutno (»Primerjaj s trenutno«) ali jo obnovite z »Obnovi to različico«; obnova se shrani kot nova različica, prejšnje ostanejo.

## 4. Seznam kontaktov

- Seznam kaže **25 kontaktov na stran** in skupno število. Privzeto je urejen po priimku; kontakti brez priimka so na koncu.
- **Razvrščanje:** izberite naslov stolpca (priimek, ime, e-pošta, podjetje, kraj, država); ponovna izbira obrne vrstni red. Velike in male črke ter strešice (š, č, ž) se pri razvrščanju ne štejejo.
- **Iskanje:** polje »Iskanje« najde dele besed v imenu, priimku, e-pošti, podjetju, kraju, značkah in vrednostih atributov, ne glede na velike črke in strešice (»skerlj« najde »Škerlj«).
- **Filtri** (»Filtri«): značke (ima vse ali katero koli od izbranih), »Brez značk«, država, podjetje, atribut (ključ ima vrednost ali obstaja). Iskanje in filtri delujejo skupaj. »Počisti filtre« vrne polni seznam.

## 5. Izvoz v Excel

1. Izberite kontakte: potrdilno polje ob vrstici, »Izberi vse na tej strani« ali »Izberi vseh N zadetkov« (vsi zadetki trenutnih filtrov). Izbira ostane, ko listate po straneh, in se počisti, ko spremenite iskanje ali filtre.
2. Izberite »Izvozi v Excel«. Datoteka je imenovana `<ime skupine>_<datum>.xlsx`, ima en list, glavo, vrstico na kontakt, enajst polj kontakta, stolpec »Značke« in stolpec za vsak ključ atributa izbranih kontaktov.
3. Naenkrat je mogoče izvoziti do **10.000 kontaktov**; pri večji izbiri zožite filter. Vsak izvoz se zapiše v dejavnost skupine.

Če za izvoz uporabljate kontakte z značko `neObvescaj`, jo izključite s filtrom »Brez značk«: aplikacija sama ne pošilja sporočil, značka je znamenje za druga orodja, naj kontakta ne obveščajo.

## 6. Koš in obnova

»Izbriši« na kontaktu ga premakne v **koš**. Tam ostane **30 dni**, nato se trajno izbriše. V zavihku »Koš« ga lahko obnovite (»Obnovi«). Lastnik ga lahko tam izbriše tudi takoj (»Izbriši za vedno«); tega ni mogoče razveljaviti, v zgodovini ostane le zapis o izbrisu. Kontakti v košu se ne pokažejo na seznamu in ne v iskanju.

## 7. Člani in povabila (lastnik)

- **Povabilo:** v zavihku »Povabila« vpišite e-pošto in vlogo ter izberite »Ustvari povabilo«. Aplikacija **ne pošlje sporočila**: povabljenca obvestite sami. Ko se oseba prijavi z istim, potrjenim e-poštnim naslovom, se skupina pojavi na njenem seznamu z dano vlogo. Povabilo velja 14 dni, odprtih povabil je lahko največ 20; prekličete ga z »Prekliči povabilo«.
- **Člani:** v zavihku »Člani« lahko spremenite vlogo člana ali ga odstranite (»Odstrani«). Odstranjeni član izgubi dostop; kontakti in zgodovina, ki jih je ustvaril, ostanejo.

## 8. Dejavnost skupine (lastnik in urednik)

Zavihek »Dejavnost« kaže vse zapise zgodovine skupine, najnovejši najprej, po 25 na stran. Filtrirate lahko po uporabniku, dejanju in obdobju (»Od«, »Do«; oba dneva štejeta, čas je ljubljanski). Če je začetni datum po končnem, se pokaže opozorilo. Filtri delujejo skupaj.

## 9. Podvojeni kontakti (lastnik in urednik)

Zavihek »Podvojeni kontakti« poišče kontakte, ki bi lahko bili ista oseba: z enako e-pošto (»gotovo«) ali z enakim imenom in priimkom ter enakim podjetjem ali telefonom (»verjetno«). Primerjava ne razlikuje velikih črk in strešic. Nič se ne spremeni, dokler jih ne združite.

- **Ni podvojeno:** množico označite, da se ne prikazuje več.
- **Združi:** izberite vsaj dva kontakta in »Združi izbrane«. Za vsako polje izberete vrednost, ki jo obdržite. Obdrži se najstarejši kontakt, ki dobi vse značke; ostali gredo v koš in jih lahko obnovite. Združitev se zapiše v zgodovino.

## 10. Izbris skupine in računa

- **Skupina** (lastnik): v možnostih skupine izberite »Izbriši skupino« in vpišite njeno ime. Skupina gre za 30 dni v **koš skupin**, članom je takoj skrita; v tem času jo lastnik obnovi (»Koš skupin«), nato se trajno izbriše z vsemi kontakti.
- **Račun:** v »Moj račun« izberite »Izbriši račun«. Izbrišejo se račun in vsa članstva; kontakti, ki ste jih ustvarili, ostanejo v skupinah, v zgodovini ostane vaše ime. Če ste edini lastnik skupine, računa ne morete izbrisati: lastništvo predajte drugemu članu ali skupino izbrišite.

## 11. Pogosta vprašanja

**Skupine ne vidim, čeprav so me povabili.** Prijavite se z istim e-poštnim naslovom, na katerega je bilo povabilo, in potrdite e-pošto. Povabilo je morda poteklo (14 dni) ali preklicano.

**Ne morem urejati.** Verjetno ste ogledovalec; lastnik vam lahko spremeni vlogo.

**Shranjevanje je zavrnjeno z obvestilom, da je kontakt spremenil drug uporabnik.** Glejte razdelek 3: preglejte trenutno stanje in spremembo ponovite.

**Izbrisal sem kontakt.** Odprite zavihek »Koš« in kontakt obnovite, če ni minilo 30 dni.

**Kdo je spremenil podatek?** Odprite zgodovino kontakta ali dejavnost skupine.
