import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Server {
  public static void main(String[] args) {
    System.out.println("Serveur en attente de connexions...");
    Socket clientSocket = null;
    try (ServerSocket serverSocket = new ServerSocket(8080)) {
      while (true) {
        clientSocket = serverSocket.accept();
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
            handleConnection(clientSocket,"extensions.txt", reader, writer);
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

  private static void handleConnection(Socket clientSocket ,String extensionsFile, BufferedReader reader, BufferedWriter writer) throws IOException {
      String[] etensionsDeUserArray = new String[0];
     // String data = reader.readLine();
      //String[] params = data.split("\\|");

      // récupère l'addresse ip du serveur
      String serverIP = reader.readLine();
      System.out.println("serverIP : " + serverIP);

      // récupère le chemin du dossier à sauver
      String sourceDir = reader.readLine();
      System.out.println("sourceDir : " + sourceDir);

      // to send data to the client
      PrintStream ps = new PrintStream(clientSocket.getOutputStream());

      // to read data coming from the client
      BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      // to read data from the keyboard
      BufferedReader kb = new BufferedReader(new InputStreamReader(System.in));

      // envoi au client les informations dans extensions.txt
      // Lire toutes les lignes du fichier dans une liste
      List<String> lines = Files.readAllLines(Paths.get("extensions.txt"));

      // Afficher chaque ligne
      for (String line : lines) {
          ps.println(line);
          System.out.println(line);
      }
      ps.println("fin");
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
        etensionsDeUserArray = extensionsDeUser.split(",");

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
      String destinationDir = backupDir;
      // Create the backup directory if it doesn't exist
      try {
        Files.createDirectories(Paths.get(backupDir));
      } catch (IOException e) {
        e.printStackTrace();
      }

      InputStream inputStream = clientSocket.getInputStream();
      ZipInputStream zipInputStream = new ZipInputStream(inputStream);
      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
          String entryName = zipEntry.getName();
          String zipExtension = "";

          if (entryName != null && !entryName.isEmpty()) {
              int lastDotIndex = entryName.lastIndexOf(".");
              if (lastDotIndex != -1) {
                  zipExtension = entryName.substring(lastDotIndex + 1);
              }
              System.out.println("zipExtension : " + zipExtension);

              Path filePath = Paths.get(destinationDir, entryName);

              // Vérifier si l'extension du fichier est autorisée
              for (String s : etensionsDeUserArray) {
                  if(s.equals((zipExtension.toLowerCase()))){
                      if (zipEntry.isDirectory()) {
                          Files.createDirectories(filePath);
                      } else {
                          // Si c'est un fichier, créez le fichier et copiez les données
                          Files.createDirectories(filePath.getParent());
                          try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                              byte[] buffer = new byte[1024];
                              int bytesRead;
                              while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                                  outputStream.write(buffer, 0, bytesRead);
                              }
                          }
                      }
                  }
              }
          }
          zipInputStream.closeEntry();
      }
      System.out.printf("Sauvegarde terminée. Les fichiers ont été sauvegardés dans le répertoire %s%n", backupDir);
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
