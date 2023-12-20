import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Server {
  public static void main(String[] args) {
    System.out.println("Serveur en attente de connexions...");
	
    try (ServerSocket serverSocket = new ServerSocket(8080)) {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connecté depuis " + clientSocket.getInetAddress());
        int valueSend ;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
          reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
          writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
          valueSend = Integer.parseInt(reader.readLine());
          System.out.println(valueSend);
          if(valueSend == 1){
            handleConnection("extensions.txt", "log.txt", reader, writer);
          }
          if( valueSend == 2){
            System.out.println("Telecharger une sauvgarde");
            sendFilesToUser(clientSocket);
          }
        }
        catch (IOException e) {
          e.printStackTrace(); // Gérer les exceptions d'entrée/sortie ici
        }

      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void handleConnection(String extensionsFile, String logFile, BufferedReader reader, BufferedWriter writer) throws IOException {

     // String data = reader.readLine();
      //String[] params = data.split("\\|");

      // récupère l'addresse ip du serveur
      String serverIP = reader.readLine();
      System.out.println("serverIP : " + serverIP);

      // récupère le chemin du dossier à sauver
      String sourceDir = reader.readLine();
      System.out.println("sourceDir : " + sourceDir);


      // envoi au client les informations dans extensions.txt
      // writer.write("txt, jpeg,pdf");
      // writer.flush();

      // Vider le fichier extensions.txt
      try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("extensions.txt", false)))) {
        // Le fichier est vidé ici
      } catch (IOException e) {
        e.printStackTrace();
      }

      // vide le fichier extensions
      try (PrintWriter fich = new PrintWriter(new BufferedWriter(new FileWriter("extensions.txt", true)))) {
        // récupérer les extensions à sauver et les mettre dans un tableau
        String extensionsDeUser = reader.readLine();
        System.out.println("extensions du client à sauvegarder : " + extensionsDeUser);
        String[] etensionsDeUserArray = extensionsDeUser.split(",");

        // Inscrire les extensions à sauver dans le fichier extensions.txt
        for (int i = 0; i < etensionsDeUserArray.length; i++) {
          String text = etensionsDeUserArray[i];
            // Inscrire les nouvelles extensions dans extensions.txt
            fich.println(text);
          if (i < etensionsDeUserArray.length - 1) {
            System.out.print(", ");
          }
        }
        System.out.println("Extensions ajoutées avec succès dans le fichier.");

      } catch (IOException e) {
        e.printStackTrace();
      }
      
      
      // Lire les extensions depuis le fichier de paramètres
      String[] extensions = readExtensionsFromFile(extensionsFile);

      System.out.printf("Nouvelle sauvegarde demandée depuis %s vers le serveur %s pour les extensions %s%n",
              sourceDir, serverIP, Arrays.toString(extensions));

      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      String backupDate = dateFormat.format(new Date());
      String backupDir = "backup_" + backupDate;

      // Create the backup directory if it doesn't exist
      try {
        Files.createDirectories(Paths.get(backupDir));
      } catch (IOException e) {
        e.printStackTrace();
      }

      // Vérifier si une sauvegarde précédente a déjà eu lieu
      List<String> previousBackupFiles = readPreviousBackupFiles(logFile);

      Files.walk(Paths.get(sourceDir))
              .filter(path -> Files.isRegularFile(path) && hasExtension(path.getFileName().toString(), extensions))
              .filter(path -> isNewOrModifiedFile(path, previousBackupFiles))
              .forEach(path -> {
                Path relativePath = Paths.get(sourceDir).relativize(path);
                String destPath = backupDir + File.separator + relativePath.toString();

                // Copier le fichier dans le dossier de sauvegarde avec la structure de dossiers
                try {
                  Files.createDirectories(Paths.get(destPath).getParent());
                  Files.copy(path, Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                  e.printStackTrace();
                }
              });

      // Mettre à jour le fichier journal avec les fichiers sauvegardés
      updateLogFile(logFile, backupDir);

      System.out.printf("Sauvegarde terminée. Les fichiers ont été sauvegardés dans le répertoire %s%n", backupDir);
      writer.write("Sauvegarde terminée. Les fichiers ont été sauvegardés dans le répertoire " + backupDir + "\n");
      writer.flush();

  }

  private static boolean hasExtension(String filename, String[] extensions) {
    // Obtenez l'extension du fichier
    int lastDotIndex = filename.lastIndexOf(".");
    if (lastDotIndex == -1) {
      // Le fichier n'a pas d'extension, donc ne le sauvegardez pas
      return false;
    }

    String fileExtension = filename.substring(lastDotIndex + 1);
    System.out.println("filename : " + filename);
    System.out.println("filename ext : " + fileExtension);

    // Vérifiez si l'extension est dans la liste spécifiée
    for (String ext : extensions) {
      if (fileExtension.equals(ext)) {
        return true;
      }
    }
    return false;
  }

  // Méthode pour lire les extensions depuis le fichier
  private static String[] readExtensionsFromFile(String filePath) {
    try {
      return Files.lines(Paths.get(filePath)).toArray(String[]::new);
    } catch (IOException e) {
      e.printStackTrace();
      return new String[0]; // En cas d'erreur, retourne un tableau vide
    }
  }

  // Méthode pour lire les fichiers sauvegardés précédemment depuis le fichier journal
  private static List<String> readPreviousBackupFiles(String logFile) {
    try {
      return Files.lines(Paths.get(logFile)).collect(Collectors.toList());
    } catch (IOException e) {
      // Si le fichier journal n'existe pas, retourne une liste vide
      return new ArrayList<>();
    }
  }

  // Méthode pour déterminer si un fichier est nouveau ou modifié depuis la dernière sauvegarde
  private static boolean isNewOrModifiedFile(Path filePath, List<String> previousBackupFiles) {
    try {
      String fileDetails = filePath.toString() + "|" + Files.getLastModifiedTime(filePath).toMillis();
      return !previousBackupFiles.contains(fileDetails);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  // Méthode pour mettre à jour le fichier journal avec les fichiers sauvegardés
  private static void updateLogFile(String logFile, String backupDir) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
      Files.walk(Paths.get(backupDir))
              .filter(Files::isRegularFile)
              .map(path -> path.toString() + "|" + path.toFile().lastModified())
              .forEach(fileDetails -> {
                try {
                  writer.write(fileDetails);
                  writer.newLine();
                } catch (IOException e) {
                  e.printStackTrace();
                }
              });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String readFileExtensions() throws IOException {
    // File path is passed as parameter
    File file = new File("extensions.txt");

    // Creating an object of BufferedReader class
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    // Déclaration d'un objet StringBuilder
    StringBuilder result = new StringBuilder();

    // Déclaration d'une variable pour stocker chaque ligne
    String st;

    // Condition holds true till there is character in a string
    while ((st = br.readLine()) != null) {
      // Concaténer la ligne avec une virgule
      result.append(st).append(",");
    }

    // Convertir le StringBuilder en une chaîne de caractères
    String resultString = result.toString();

    // Supprimer la virgule finale si la chaîne n'est pas vide
    if (!resultString.isEmpty()) {
      resultString = resultString.substring(0, resultString.length() - 1);
    }

    // Afficher la chaîne résultante
    System.out.println(resultString);
    return resultString;
  }

    private static void sendFilesToUser(Socket clientsocket) throws IOException {
        // to send data to the client
        PrintStream ps = new PrintStream(clientsocket.getOutputStream());

        // to read data coming from the client
        BufferedReader br = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));

        // to read data from the keyboard
        BufferedReader kb = new BufferedReader(new InputStreamReader(System.in));

        // Obtenez le chemin absolu du dossier courant
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("currentDirectory : " + currentDirectory);

        // Créez un objet File pour représenter le dossier courant
        File currentDir = new File(currentDirectory);

        // Obtenez la liste des fichiers et dossiers dans le dossier courant
        String[] folders = currentDir.list();
        for (String folder : folders) {
            if(folder.startsWith("backup")){
                System.out.println(folder.toString());
            }
        }
        // Liste des dossiers de sauvegarde disponibles
        List<String> backupFolders = new ArrayList<>();

        // Ajoutez les noms des dossiers de sauvegarde à la liste
        if (folders != null) {
            String listes = "Liste des dossiers dans le dossier courant :";
            // send to client
            ps.println(listes);
            for (String folder : folders) {
                File file = new File(currentDir, folder);
                if (file.isDirectory() && folder.startsWith("backup")) {
                   String str1 = file.getName();
                   backupFolders.add(file.getName());
                    // send to client
                    ps.println(str1);
                }
            }
            ps.println("Choisissez :");
            // le server recoit le dossier qu'il veut récuperer
            String dossierARecup = br.readLine();
            System.out.println("dossierARecup : " + dossierARecup);
            // le dossier n'existe pas
            if(!backupFolders.contains(dossierARecup)){
                ps.println("le dossier que vous avez choissis n'existe pas");
            }
            /*
            ps.println("Ou voulez vous mettre la sauvegarde : ");
            String cheminSauvegarde = kb.readLine();
             */

            // le dossier de backup existe
            ps.println("La récupération de la sauvegarde est en cours ... \n");


            // Vous devez fournir le chemin du dossier à sauvegarder ici
            String sourceDir = dossierARecup;

            try (ZipOutputStream zipOutputStream = new ZipOutputStream(ps)) {
                Path sourcePath = Paths.get(sourceDir);
                System.out.println("sourcePath : " + sourcePath);

                Files.walk(sourcePath)
                        .forEach(filePath -> {
                            try {
                                // Vérifier si le chemin du fichier est un répertoire
                                if (Files.isDirectory(filePath)) {
                                    // Ajouter une entrée ZIP pour le répertoire
                                    Path relativePath = sourcePath.relativize(filePath);
                                    String entryName = relativePath.toString() + "/";
                                    ZipEntry zipEntry = new ZipEntry(entryName);
                                    System.out.println("zipEntry (directory) : " + zipEntry);
                                    zipOutputStream.putNextEntry(zipEntry);
                                    zipOutputStream.closeEntry();
                                } else {
                                    // Ajouter une entrée ZIP pour le fichier
                                    Path relativePath = sourcePath.relativize(filePath);
                                    ZipEntry zipEntry = new ZipEntry(relativePath.toString());
                                    System.out.println("zipEntry (file) : " + zipEntry);
                                    zipOutputStream.putNextEntry(zipEntry);
                                    Files.copy(filePath, zipOutputStream);
                                    zipOutputStream.closeEntry();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }








        } else {
            System.out.println("Aucun dossier trouvé dans le dossier courant.");
        }

    }

}
