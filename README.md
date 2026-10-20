# 🥋 TaegeuKizz - QCM Passage de Grade 1er Dan Taekwondo (FFTDA)

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Status](https://img.shields.io/badge/Status-Open%20Source-brightgreen.svg)](#)

**TaegeuKizz** est une application Android **100 % gratuite, hors-ligne et open source** conçue pour accompagner les pratiquants de Taekwondo dans la préparation de l'épreuve théorique (Module B) de l'examen du **1er Dan FFTDA** (Fédération Française de Taekwondo et Disciplines Associées).

---

## 🎯 Objectifs de l'application

L'examen du 1er Dan comporte une épreuve théorique sous forme de questionnaire à choix multiples (QCM) évaluant la culture marciale, le lexique coréen et la réglementation sportive. 

**TaegeuKizz** offre une solution simple, rapide et accessible partout (sans connexion Internet requise) pour s'entraîner efficacement :
- **Entraînement en conditions réelles** : Tirage aléatoire de **15 questions** parmi une banque complète de 120 questions.
- **Répartition équilibrée** : Couverture bilatérale conforme aux exigences du passage de grade (Bloc Arbitrage / Réglementation & Bloc Culture / Terminus).
- **Évaluation immédiate** : Calcul automatique de la note globale sur **30 points** (seuil d'admissibilité à 15/30).
- **Correction détaillée et explications** : Récapitulatif interactif à la fin de chaque session indiquant les réponses correctes/incorrectes ainsi qu'une explication pédagogique pour chaque erreur.

---

## 📂 Contenu et Structure du Projet

L'application est développée en **Java natif** pour Android (API 24 minimum) et repose sur une architecture légère sans dépendances lourdes :

```text
TaegeuKizz/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── assets/
│   │       │   └── questions.json      # Base de 120 questions, options et explications
│   │       ├── java/com/taekwondo/qcm1erdan/
│   │       │   ├── MainActivity.java    # Écran d'accueil et lancement
│   │       │   ├── QuizActivity.java    # Moteur de jeu et gestion du chronomètre / flux
│   │       │   ├── ResultActivity.java  # Écran de récapitulatif défilant et scores
│   │       │   ├── Question.java        # Modèle de données de question
│   │       │   ├── QuestionBank.java    # Parser JSON et pioche aléatoire
│   │       │   └── QuestionResult.java  # Modèle de restitution pour la correction
│   │       └── res/
│   │           ├── layout/              # Fichiers XML d'interface utilisateur
│   │           └── drawable/            # Visuels, icônes et ressources vectorielles
└── README.md
```

---

## 🚀 Installation & Utilisation

### 1. Pour les utilisateurs (Installer l'APK)
1. Téléchargez la dernière version de l'APK depuis la section [Releases](../../releases) du dépôt.
2. Autorisez l'installation d'applications issues de sources inconnues sur votre appareil Android.
3. Installez et lancez l'application (aucune inscription ni connexion réseau requise).

### 2. Pour les développeurs (Build local)
1. Clonez ce dépôt sur votre machine :
   ```bash
   git clone https://github.com/morvanlemescam/taegeukizz.git
   ```
2. Ouvrez le projet dans **Android Studio**.
3. Synchronisez le projet avec Gradle (`Sync Project with Gradle Files`).
4. Lancez l'application sur un émulateur ou un appareil physique connecté.

---

## 🤝 Contribution & Open Source

Ce projet est **gratuit et open source**. Les contributions de la communauté (pratiquants, enseignants, arbitres, développeurs) sont les bienvenues !

Vous pouvez contribuer de plusieurs manières :
* **Enrichir la base de questions** : Ajoutez ou corrigez des questions directement dans le fichier `app/src/main/assets/questions.json`.
* **Rapporter un bug** : Ouvrez une [Issue](../../issues) pour signaler un problème d'affichage ou une erreur technique.
* **Proposer une amélioration** : Soumettez une *Pull Request* avec vos modifications.

---

## 📄 Licence

Ce projet est sous licence **MIT**. Vous êtes libre de le réutiliser, de le modifier et de le distribuer. Voir le fichier `LICENSE` pour plus de détails.
