# Runehold Village Builder

## Problème à résoudre

Comment transformer la progression normale d'un joueur OSRS en un village qu'il a
réellement envie d'ouvrir, d'organiser et d'améliorer, sans toucher à l'économie
OSRS et sans produire une copie visuelle de Clash of Clans ?

## Joueur cible et définition du succès

Le joueur cible aime les progressions longues d'OSRS et les jeux de construction
de base. Il ne cherche pas un tableau de statistiques supplémentaire : il veut
voir son temps de jeu devenir un lieu personnel.

Le concept fonctionne si le joueur revient spontanément dans Runehold pour :

1. dépenser le mana gagné pendant ses sessions OSRS ;
2. placer et réorganiser ses bâtiments ;
3. lancer sa prochaine construction ;
4. préparer une future défense contre d'autres villages.

## Directions évaluées

| Direction | Valeur | Faisabilité | Différenciation | Verdict |
| --- | --- | --- | --- | --- |
| Liste de bâtiments dans le panneau RuneLite | Faible | Très forte | Faible | L'actuel MVP prouve l'économie mais pas le plaisir de construire. |
| Village superposé au monde OSRS | Forte | Faible | Forte | Trop risqué pour les règles d'interface et trop intrusif pendant le jeu. |
| Fenêtre de village dédiée | Très forte | Forte | Très forte | Direction retenue : assez d'espace, interaction locale et aucune action OSRS. |
| Client web autonome | Moyenne | Moyenne | Moyenne | Brise l'expérience RuneLite et introduit trop tôt un service distant. |
| Raids multijoueurs dès maintenant | Forte | Faible | Forte | Le serveur, l'identité et l'anti-triche masqueraient le risque produit principal. |
| Partage de layouts sans combat | Moyenne | Forte | Moyenne | Bon prolongement après validation de l'éditeur local. |

## Direction recommandée

Runehold devient un vrai **village-builder local**. Le panneau RuneLite reste un
résumé compact, puis ouvre une fenêtre dédiée dominée par une parcelle isométrique
de 18 x 18 cases. Le joueur sélectionne un bâtiment dans une palette, voit son
empreinte fantôme, puis le place seulement si toutes les cases sont libres et dans
les limites. Il peut sélectionner, déplacer et améliorer les constructions.

Les mécanismes repris de Clash of Clans sont structurels : Hôtel de ville qui
cadence la progression, constructeur occupé pendant les travaux, temps de
construction réel, terrain librement organisé et barre d'actions contextuelle.
L'univers, les sprites, les noms, les matériaux, les icônes et le rendu restent
originaux et inspirés du langage visuel médiéval d'OSRS.

Le vrai pari n'est pas encore le combat. Il est plus simple et plus risqué :
**déplacer un bâtiment sur une grille Runehold doit déjà être satisfaisant.**

## Hypothèses à valider

- [ ] Une fenêtre dédiée paraît naturelle depuis RuneLite et ne coupe pas le joueur
  de sa session OSRS — à vérifier dans le client de développement.
- [ ] Une grille isométrique Swing avec placement fantôme reste fluide sur les
  machines modestes — à vérifier avec des tests de projection et une mesure du
  temps de rendu.
- [ ] Un seul constructeur et des durées courtes au début créent de l'anticipation
  sans frustrer — à vérifier avec la première construction de 30 secondes.
- [ ] Une seule monnaie, le mana gagné par l'XP, suffit à donner du sens à la boucle
  de construction — à observer avant d'ajouter d'autres ressources.
- [ ] Les joueurs préfèrent d'abord personnaliser leur village avant d'avoir besoin
  de raids — à confirmer pendant le test manuel de la tranche locale.

## Périmètre MVP de cette tranche

- fenêtre Runehold dédiée et redimensionnable ;
- parcelle isométrique bornée de 18 x 18 cases ;
- Hôtel de ville, Puits de mana, Caserne et Atelier, chacun avec son empreinte ;
- construction, sélection, déplacement et amélioration ;
- cases vertes/rouges pendant le placement ;
- zoom et déplacement de caméra ;
- un constructeur et un chantier persistant à la fois ;
- migration sans perte de l'état local actuel ;
- sprites et habillage Runehold originaux ;
- résumé du mana, du constructeur et du chantier dans le panneau RuneLite.

## Ce que cette tranche ne fait pas

- **Raids et matchmaking** — nécessitent un backend autoritaire et une autre
  spécification de sécurité.
- **Troupes et simulation de combat** — n'aident pas à valider le plaisir de bâtir.
- **Murs, pièges, décorations et obstacles destructibles** — gonflent le moteur de
  placement avant que sa boucle principale soit prouvée.
- **Plusieurs exemplaires d'un même bâtiment** — un exemplaire par type garde la
  migration et l'interface compréhensibles.
- **Rotation des bâtiments** — les sprites isométriques gardent une orientation
  fixe dans cette première tranche.
- **Accélération payante ou monnaie premium** — incompatible avec l'intention du
  plugin et inutile à la validation.
- **Copie d'assets ou d'écrans Clash of Clans** — Runehold réutilise des principes
  d'interaction, pas l'identité propriétaire d'un autre jeu.

## Questions reportées

- Faut-il ensuite privilégier les défis amicaux sans perte ou les raids à trophées ?
- Le deuxième constructeur doit-il être débloqué par l'Hôtel de ville, un succès
  OSRS ou une progression propre à Runehold ?
- Les obstacles futurs doivent-ils être purement décoratifs ou faire partie de
  l'économie du village ?
