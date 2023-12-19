import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
  public static void main(String[] args) {
    System.out.println("Serveur en attente de connexions...");
	
    try (ServerSocket serverSocket = new ServerSocket(8080)) {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connecté depuis " + clientSocket.getInetAddress());
        handleConnection(clientSocket, "extensions.txt", "log.txt"); // Modifiez cette ligne
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void handleConnection(Socket clientSocket, String extensionsFile, String logFile) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
	
	
      String data = reader.readLine();
      String[] params = data.split("\\|");
	
      // récupère le chemin du dossier à sauver
      String sourceDir = new File(params[0]).getAbsolutePath();
      // récupère l'addresse ip du serveur
      String serverIP = params[1];
      // récupère les extensions à sauver et les mets dans une Array
      String extensionsDeUser = params[2];
      System.out.println("extensionsDeUser " + extensionsDeUser);
      String[] etensionsDeUserArray = extensionsDeUser.split(",");
      // Affichez le contenu du tableau
        System.out.print("Tableau d'extensions : [");
        for (int i = 0; i < etensionsDeUserArray.length; i++) {
            System.out.print(etensionsDeUserArray[i]);
            if (i < etensionsDeUserArray.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("]");
      // inscris les extensions à sauver dans le fichier extensions.txt
        Path fileName = Path.of("extensions.txt");
      System.out.println("fileName " + fileName);
      for (int i = 0; i < etensionsDeUserArray.length; i++) {
            System.out.print(etensionsDeUserArray[i]);
            String text = etensionsDeUserArray[i];
            Files.writeString(fileName, text);
            if (i < etensionsDeUserArray.length - 1) {
                System.out.print(", ");
            }
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
    } catch (IOException e) {
      e.printStackTrace();
    }
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
}
