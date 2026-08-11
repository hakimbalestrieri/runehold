# Spécification : Runehold Village Builder

## Statut

**Brouillon à valider avant planification et implémentation.**

## Hypothèses proposées à la validation

1. Le village s'ouvre dans une fenêtre Swing dédiée, non modale et liée à la
   fenêtre RuneLite ; le panneau latéral de 225 px reste un résumé et un lanceur.
2. La parcelle fonctionnelle mesure 18 x 18 cases et les sprites gardent une
   orientation isométrique fixe.
3. Cette tranche garde les quatre bâtiments existants et un seul exemplaire de
   chaque type.
4. Le joueur dispose d'un seul constructeur et ne peut lancer qu'un chantier à la
   fois.
5. Le mana reste l'unique ressource et sa formule actuelle ne change pas.
6. Les niveaux, le mana et les données d'XP existants sont migrés sans perte vers
   le schéma persistant v2.
7. Le multijoueur, les raids, les troupes et les défenses actives restent hors de
   cette tranche.

## Objectif

Transformer le MVP actuel, fondé sur une liste de bâtiments, en une première
expérience de construction de base réellement jouable. Le joueur doit pouvoir
ouvrir Runehold, voir un terrain délimité, choisir où construire, déplacer son
village et lancer des améliorations temporisées avec le mana gagné par son XP OSRS.

Le joueur cible aime à la fois la progression longue d'OSRS et l'organisation de
base de Clash of Clans. Le succès n'est pas mesuré par la fidélité d'une copie : il
est atteint lorsque le village suit les mêmes principes de lisibilité et de
satisfaction spatiale tout en possédant une identité Runehold originale.

### Histoires utilisateur

- En tant que joueur, j'ouvre mon village depuis le panneau RuneLite sans agir sur
  le monde OSRS.
- En tant que joueur, je vois clairement la limite de ma parcelle et les cases
  occupées.
- En tant que joueur, je prévisualise l'empreinte d'un bâtiment avant de le poser.
- En tant que joueur, je ne peux jamais valider un placement en collision ou hors
  terrain.
- En tant que joueur, je sélectionne un bâtiment et choisis Informations,
  Déplacer ou Améliorer.
- En tant que joueur, je vois si mon constructeur est libre et quand son chantier
  se termine.
- En tant que joueur existant, je conserve mon mana, mon XP et mes niveaux après
  la migration.

## Règles fonctionnelles

### Parcelle et coordonnées

- La parcelle contient 18 colonnes et 18 lignes indexées de 0 à 17.
- Une position désigne la case nord-ouest de l'empreinte logique du bâtiment.
- L'Hôtel de ville occupe 4 x 4 cases.
- Le Puits de mana occupe 2 x 2 cases.
- La Caserne et l'Atelier occupent chacun 3 x 3 cases.
- Un placement est valide si chaque case de l'empreinte est dans la parcelle et
  n'appartient à aucune autre construction.
- Les bâtiments ne tournent pas dans cette tranche.
- Une commande invalide ne modifie ni le mana, ni la position, ni le chantier.

### Village initial

- Un nouveau village possède son Hôtel de ville niveau 1 à la position (7, 7).
- Les autres bâtiments ne sont présents qu'après le lancement de leur construction.
- Le terrain fonctionnel ne contient pas encore d'obstacle destructible.
- Les rochers, souches et arbres éventuels de la bordure sont purement visuels et
  ne recouvrent jamais une case constructible.

### Mode construction et déplacement

- La palette inférieure présente les bâtiments constructibles ou déjà construits.
- Choisir un bâtiment absent entre en mode construction.
- Déplacer un bâtiment existant entre en mode déplacement sans coût et sans durée.
- Le bâtiment fantôme suit la case pointée ; son empreinte est verte si elle est
  valide, rouge sinon.
- Un clic principal ou la touche Entrée confirme un placement valide.
- Échap ou le bouton Annuler quitte le mode sans changement.
- Les flèches permettent de déplacer le fantôme case par case pour ne pas rendre
  le placement dépendant du glisser-déposer.
- Un bâtiment en chantier ne peut pas être déplacé dans cette tranche.

### Sélection contextuelle

- Un clic sur une construction la sélectionne et dessine une bordure distincte.
- La barre contextuelle expose trois actions explicites : Informations, Déplacer,
  Améliorer.
- Informations affiche le niveau, l'empreinte, le prochain coût, la durée et le
  prérequis d'Hôtel de ville.
- Améliorer affiche une confirmation avant de dépenser le mana.
- Les actions indisponibles restent visibles avec leur raison textuelle.

### Constructeur et chantiers

- Le village possède un constructeur dans cette tranche.
- Une construction ou amélioration réussie dépense immédiatement le coût existant
  et crée un chantier contenant le type, le niveau cible, l'instant de début et
  l'instant de fin.
- Le niveau cible n'est appliqué qu'à la fin du chantier.
- Pour une nouvelle construction, l'emplacement est réservé dès le lancement et
  affiche l'échafaudage jusqu'au niveau 1.
- Le chargement, l'ouverture de la fenêtre et un minuteur Swing d'une seconde
  vérifient la fin du chantier avec une horloge injectable.
- Si l'instant de fin est passé pendant que RuneLite était fermé, le chantier se
  termine au prochain chargement et l'état est sauvegardé.
- Il n'y a ni accélération, ni annulation, ni remboursement dans cette tranche ;
  la boîte de confirmation prévient les erreurs.

### Durées proposées

Les coûts et prérequis restent ceux du catalogue actuel. Les durées sont indexées
par niveau cible :

| Bâtiment | L1 | L2 | L3 | L4 | L5 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Hôtel de ville | déjà construit | 5 min | 20 min | 2 h | 8 h |
| Puits de mana | 30 s | 3 min | 15 min | 1 h | 4 h |
| Caserne | 2 min | 10 min | 1 h | 4 h | — |
| Atelier | 5 min | 30 min | 3 h | — | — |

La première construction de 30 secondes sert de golden path testable sans outil
d'accélération caché.

### Caméra et rendu

- Le canvas est une sous-classe Swing de `JComponent` et n'utilise pas de moteur
  externe.
- La projection logique suit une transformation isométrique déterministe avec une
  transformation inverse testée pour le pointage.
- La molette et les boutons `+` / `-` règlent le zoom entre 0,75 et 1,75.
- Le glisser sur une zone vide déplace la caméra ; un bouton recentre la parcelle.
- Le rendu est déclenché par les changements d'état, les entrées et le minuteur de
  chantier, pas par une boucle permanente à 60 images par seconde.
- Les couches sont dessinées dans cet ordre : fond, terrain, grille utile, cases de
  placement, bâtiments triés par profondeur, chantier, sélection, HUD.

### Interface RuneLite

- Le `PluginPanel` actuel devient un résumé compact du mana, du constructeur et du
  chantier actif.
- Un bouton explicite `Open village` ouvre ou ramène au premier plan la fenêtre
  dédiée.
- La fenêtre est non modale, redimensionnable, d'une taille initiale de 1100 x 700
  et d'une taille minimale de 900 x 600.
- Fermer la fenêtre la masque ou la détruit sans arrêter RuneLite ni le plugin.
- L'arrêt du plugin ferme la fenêtre, arrête son minuteur et retire tous les
  écouteurs.
- Toute mutation Swing s'exécute sur l'Event Dispatch Thread.

### Direction artistique et assets

- Le terrain, la grille et les états vert/rouge sont dessinés par code.
- Les textes de l'interface utilisent les polices OSRS fournies à l'exécution par
  `FontManager`. Les contrôles du HUD peuvent utiliser les sprites OSRS chargés
  depuis le cache local par `ItemManager` ou `SpriteManager`.
- Aucun fichier extrait du cache Jagex (PNG, police, modèle ou son) n'est copié
  dans le dépôt ou redistribué dans le JAR.
- Les bâtiments, l'échafaudage et le constructeur utilisent des PNG originaux
  optimisés et chargés avec `ImageUtil.loadImageResource`.
- Les sprites suivent la référence
  `docs/design/runehold-village-concept-v2.png` mais ne copient aucun asset tiers.
- Les bâtiments possèdent une silhouette propre à Runehold. Les niveaux supérieurs
  peuvent utiliser des ornements procéduraux dans cette tranche ; une série de
  sprites complète par niveau est reportée si elle met en danger le moteur de
  placement.
- Aucun asset, logo, emblème, personnage ou écran de Clash of Clans n'entre dans
  le dépôt. Les seuls visuels OSRS sont demandés au client RuneLite à l'exécution
  et documentés dans `THIRD_PARTY_NOTICES.md`.

### Persistance et migration

- `CURRENT_SCHEMA_VERSION` passe de 1 à 2 après validation de cette spécification.
- Le schéma v2 ajoute les positions et le chantier actif tout en conservant les
  champs de mana, XP, date et niveaux.
- Le décodeur continue d'accepter le schéma v1 et le migre en mémoire.
- La migration place les bâtiments déjà construits aux coordonnées déterministes :
  Hôtel de ville (7, 7), Puits de mana (3, 9), Caserne (11, 4), Atelier (11, 11).
- Un bâtiment de niveau 0 n'obtient aucune position pendant la migration.
- La migration ne crée aucun chantier et donne un constructeur libre.
- Les niveaux, le mana, les restes d'XP, la progression quotidienne et la date sont
  conservés exactement.
- Les coordonnées, empreintes et chantiers invalides sont rejetés à la frontière de
  persistance. Un JSON irrécupérable revient à un village neuf sans exception non
  gérée, comme aujourd'hui.
- Le JSON reste stocké dans `ConfigManager` au niveau du profil RuneScape ; aucune
  donnée n'est envoyée sur le réseau.

## Stack technique

- Java 11.
- Swing/AWT et APIs fournies par RuneLite.
- Gradle selon le template officiel RuneLite.
- Gson injecté par RuneLite pour le schéma v2.
- JUnit 4.12 pour les tests.
- Aucune nouvelle dépendance runtime.

## Commandes

À exécuter depuis la racine du dépôt sous Windows :

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat run
```

`run` ouvre uniquement le client de développement. La connexion et les actions
dans RuneScape restent exclusivement manuelles et réalisées par l'utilisateur.

## Structure proposée

```text
src/main/java/com/runehold/
  domain/
    BuildingCatalog.java             coûts, prérequis, durées et empreintes
    VillageState.java                mana, niveaux, layout et chantier
    layout/
      GridPoint.java                 coordonnée immutable
      Footprint.java                 largeur et hauteur
      BuildingPlacement.java         type et position
      VillageLayout.java             occupation et commandes atomiques
      PlacementResult.java           succès ou motif de refus
    construction/
      BuildJob.java                  chantier temporel immutable
      ConstructionService.java       démarrage et achèvement
  persistence/
    RuneholdStateCodec.java          validation v1/v2 et migration
    PersistedRuneholdState.java      DTO JSON borné
  ui/
    RuneholdPanel.java               résumé latéral et lanceur
    village/
      VillageWindow.java             cycle de vie de la fenêtre
      VillageCanvas.java             rendu et entrées
      IsometricProjection.java       conversions grille/écran
      VillageInteractionModel.java   sélection et mode placement
      VillageViewModel.java          snapshot immutable pour l'EDT
      SpriteAtlas.java               chargement des PNG empaquetés
src/main/resources/runehold/village/
  town_hall.png
  mana_well.png
  barracks.png
  workshop.png
  construction.png
  builder.png
src/test/java/com/runehold/
  domain/layout/
  domain/construction/
  persistence/
  ui/village/
docs/design/
docs/specs/
tasks/
```

Les noms peuvent être simplifiés pendant la planification si une classe n'apporte
pas de frontière utile. Les règles de placement restent dans le domaine pur et ne
sont jamais dupliquées dans le canvas.

## Style de code

Conserver les conventions RuneLite : tabulations, accolades à la ligne, objets de
domaine immuables quand possible et commandes explicites.

```java
public PlacementResult move(BuildingType type, GridPoint destination)
{
	PlacementResult preview = previewMove(type, destination);
	if (!preview.isSuccess())
	{
		return preview;
	}

	placements.put(type, new BuildingPlacement(type, destination));
	return preview;
}
```

## Stratégie de tests

- TDD pour chaque règle de placement et de chantier.
- Tests unitaires de limites, collisions, auto-collision pendant un déplacement,
  placement atomique et position initiale.
- Tests de projection grille/écran pour plusieurs niveaux de zoom et points de
  frontière.
- Tests avec `Clock.fixed` avant, à et après la fin d'un chantier.
- Tests de constructeur occupé, mana insuffisant, prérequis, sauvegarde et
  achèvement au redémarrage.
- Tests de migration v1 vers v2 avec conservation exacte du mana, de l'XP et des
  niveaux.
- Tests du view model pour les textes, actions indisponibles et états de couleur
  accompagnés d'un libellé.
- `gradlew test` puis `gradlew build` doivent passer à chaque tranche.
- Le rendu final, la souris, le clavier, le redimensionnement et le cycle de vie de
  la fenêtre nécessitent une validation manuelle dans le client RuneLite.

## Limites

### Toujours

- Garder toutes les interactions dans l'interface du plugin.
- Valider les commandes dans le domaine avant toute mutation ou dépense.
- Utiliser une horloge injectable pour la logique temporelle.
- Préserver les données v1 lors de la migration.
- Optimiser les PNG et les charger comme ressources empaquetées.
- Nettoyer fenêtre, minuteur et écouteurs à l'arrêt.
- Ajouter un test en échec avant chaque nouveau comportement.

### Demander avant

- Changer la formule de mana, les coûts ou les prérequis existants.
- Modifier les durées proposées après validation.
- Ajouter une dépendance runtime.
- Introduire réseau, comptes, télémétrie ou synchronisation distante.
- Ajouter un deuxième constructeur, une autre ressource ou plusieurs bâtiments du
  même type.
- Réinitialiser, supprimer ou migrer avec perte un village existant.

### Jamais

- Copier ou extraire les assets, modèles, sons, textes ou écrans d'un autre jeu.
- Injecter des entrées souris/clavier ou envoyer une action au serveur OSRS.
- Modifier les menus Construction d'OSRS ou les zones cliquables du client.
- Interagir avec le monde depuis une caméra détachée ; la caméra Runehold ne
  contrôle que le canvas local.
- Exposer les informations d'un joueur par HTTP ou collecter celles d'autres
  joueurs.
- Rendre le mana convertible en GP, objets OSRS ou argent réel.
- Utiliser réflexion, code dynamique, processus externe ou sérialisation Java.

## Critères de succès

- [ ] Le panneau RuneLite ouvre une seule fenêtre Runehold non modale et la ramène
  au premier plan si elle existe déjà.
- [ ] Une nouvelle partie affiche une parcelle 18 x 18 et l'Hôtel de ville en (7, 7).
- [ ] Les quatre empreintes sont rendues à la bonne profondeur isométrique.
- [ ] Le fantôme et les cases indiquent visuellement et textuellement un placement
  valide ou invalide.
- [ ] Les placements en collision ou hors terrain échouent sans mutation.
- [ ] Le déplacement est gratuit, atomique, annulable et accessible au clavier.
- [ ] Une construction dépense le mana, réserve l'emplacement et occupe l'unique
  constructeur jusqu'à l'instant de fin.
- [ ] Un chantier se termine correctement pendant l'exécution et après un
  redémarrage simulé.
- [ ] La migration d'un état v1 conserve exactement les valeurs existantes et
  attribue des positions déterministes sans collision.
- [ ] Le zoom reste borné entre 0,75 et 1,75 et le recentrage retrouve la parcelle.
- [ ] Tous les sprites empaquetés sont originaux et optimisés ; chaque asset OSRS
  est fourni à l'exécution par RuneLite et inventorié.
- [ ] Aucun appel réseau ou action OSRS n'est ajouté.
- [ ] Les tests et le build Gradle passent.
- [ ] L'utilisateur valide le golden path dans le client RuneLite de développement.

## Hors périmètre

- Attaques, matchmaking, défenses simulées, trophées et replays.
- Serveur, authentification, clans et classements.
- Troupes, camps, formation et intelligence de combat.
- Murs, pièges, routes éditables, décorations et obstacles destructibles.
- Rotations, layouts sauvegardés multiples et mode photo.
- Monnaie premium, accélération, achats et publicité.
- Variantes artistiques complètes pour chaque niveau si elles retardent le moteur
  spatial ; elles feront l'objet d'une tranche artistique séparée.

## Sources officielles de référence

- RuneLite `PluginPanel` (largeur fixe de 225 px) :
  https://raw.githubusercontent.com/runelite/runelite/master/runelite-client/src/main/java/net/runelite/client/ui/PluginPanel.java
- Règles du template et du Plugin Hub :
  https://raw.githubusercontent.com/runelite/example-plugin/master/AGENTS.md
- Directives Jagex pour les clients tiers :
  https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1
- Principes officiels du Builder Base (placement, ressources et batailles) :
  https://support.supercell.com/clash-of-clans/en/articles/builder-base-the-basics-2.html
- Rôle officiel des constructeurs :
  https://ingame.support.supercell.com/clash-of-clans/en/articles/builders-4.html
