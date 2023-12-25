# Application de Sauvegarde de Fichiers 

## Introduction
Cette application a été développée pour simplifier le processus de sauvegarde de fichiers en permettant à l'utilisateur de spécifier les extensions de fichiers à sauvegarder en plus de celles de bases qui sont : "txt, pdf, jpg, jpeg, png, docx, xlsx, mp3, mp4, html". 
Les fichiers sont chiffrés coté client avec la méthode XOR avant d'être envoyé au serveur. 
Le client déchiffre également les données lorsqu'il les récupère depuis le serveur.

Cette partie est la partie serveur de l'application, pour la partie Client veuillez suivre ce lien [Client](https://github.com/SamyOffer/TD-Cloud-Sauvegarde-Client)

## Prérequis
Avant d'utiliser l'application, assurez-vous d'avoir Java installé sur votre machine. Vous pouvez télécharger la dernière version de Java sur le site officiel d'Oracle : [Télécharger Java](https://www.oracle.com/java/technologies/javase-downloads.html)

## Configuration
Il n'y a aucune configuration à faire, il suffit d'exécuter le serveur comme indiqué ci-dessous. 

## Utilisation
1. **Exécution de l'Application**
   - Ouvrez une invite de commande (ou terminal) dans le répertoire de l'application.
   - Exécutez la commande suivante pour lancer l'application :
     ```
     cd src
     ```
     ```
     javac Server.java
     ```
     ```
     java Server
     ```
   - Pour exécuter le client veuillez suivre ce lien [Client](https://github.com/SamyOffer/TD-Cloud-Sauvegarde-Client)

2. **Sauvegarde des Fichiers**
   - L'application explorera récursivement le répertoire spécifié et sauvegardera les fichiers correspondant aux extensions spécifiées dans `extensions.txt`.
   - Les fichiers sauvegardés seront stockés dans un répertoire nommé `Backup_date` créé dans le répertoire de l'application.

## Avertissement
L'application ne modifie pas les fichiers d'origine. Elle crée une copie des fichiers chiffrés correspondant aux extensions spécifiées dans le répertoire `Backup_date`.

Merci d'utiliser notre application de sauvegarde de fichiers  !
