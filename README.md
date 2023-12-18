# Application de Sauvegarde de Fichiers 

## Introduction
Cette application a été développée pour simplifier le processus de sauvegarde de fichiers en permettant à l'utilisateur de spécifier les extensions de fichiers à sauvegarder dans un fichier `extensions.txt`.

## Prérequis
Avant d'utiliser l'application, assurez-vous d'avoir Java installé sur votre machine. Vous pouvez télécharger la dernière version de Java sur le site officiel d'Oracle : [Télécharger Java](https://www.oracle.com/java/technologies/javase-downloads.html)

## Configuration
1. **Extensions à Sauvegarder**
   - Ouvrez le fichier `extensions.txt` situé dans le répertoire de l'application.
   - Ajoutez les extensions de fichiers que vous souhaitez sauvegarder, une par ligne. Par exemple:
     ```
     .txt
     .doc
     .jpg
     ```

## Utilisation
1. **Exécution de l'Application**
   - Ouvrez une invite de commande (ou terminal) dans le répertoire de l'application.
   - Exécutez la commande suivante pour lancer l'application :
     ```
     javac Server.java && java Server

     javac Client.java && java Client
     ```
   - Une fois le client lancé il faut spécifier le chemin absolu du dossier à sauvgarder (pwd - pour savoir le chemin absolu du dossier)

2. **Sauvegarde des Fichiers**
   - L'application explorera récursivement le répertoire spécifié et sauvegardera les fichiers correspondant aux extensions spécifiées dans `extensions.txt`.
   - Les fichiers sauvegardés seront stockés dans un répertoire nommé `Backup_date` créé dans le répertoire de l'application.

## Avertissement
L'application ne modifie pas les fichiers d'origine. Elle crée une copie des fichiers correspondant aux extensions spécifiées dans le répertoire `Backup_date`.

Merci d'utiliser notre application de sauvegarde de fichiers  !
