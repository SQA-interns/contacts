Večnajemniški sistem za vodenje kontaktnih podatkov

Načrtovati, nadgraditi, implementirajte in namestiti bo potrebno sistem, ki bo uporabnikom omogočal vodenje kontaktov na nekem projektu ali v nekem podjetju (samo naziv je dovolj). Skupino kontaktov lahko uporabniki kreirajo sami, kontakti naj bodo vidni le znotraj te skupine; poskrbeti bo torej treba za izolacijo podatkov med različnimi uporabniki in njihovimi projekti.

Podatki o uporabnikih naj bodo do določene mere predefinirani (ime, priimek, naslov, država, email, podjetje ipd.), omogočeno pa naj bo tudi poljubno definiranje kontaktov v smislu kluč-vrednost (kompetenca=spletnoProgramiranje ipd.). Pričakuje se označevanje kontaktov s poljubnimi značkami (neObvescaj, udelezenec2025), ki bodo služile kot kategorije kontaktov. Voditi bo potrebno tudi vse spremembe kontaktov (revizijska sled) in jih tudi primerno verzionirati. Pričakuje se tudi določene napredne funkcionalnosti, kot npr. iskanje podvojenih kontaktov, njihovo združevanje ipd. Ter seveda pregled s filtriranjem ipd.

Dostop do zalednega sistema naj bo mogoč preko ustrezno zavarovanega REST vmesnika.

Uporabniški vmesnik naj nudi prijazen pregled, iskanje, filtriranje ter izvoz označenih kontaktov v format MS Excel. Konkurenčna prednost izdelka prek konkurenčnimi CRM sistemi bo torej ravno enostavnost uporabe.

·      Backend: SpringBoot
·      Frontend: Resct
·      Podatki: Postgresql
·      Integracija: Excel
·      Avtentikacija: Firebase
·      Avtorizacija: lastna

Primer že izdelane rešitve: https://github.com/anzo26/SCM