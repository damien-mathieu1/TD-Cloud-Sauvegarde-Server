import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Server {
  public static void main(String[] args) {
    System.out.println("Serveur en attente de connexions...");

    try (ServerSocket serverSocket = new ServerSocket(8080)) {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connecté depuis " + clientSocket.getInetAddress());
        handleConnection(clientSocket);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void handleConnection(Socket clientSocket) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

      String data = reader.readLine();
      String[] params = data.split("\\|");


      String sourceDir = new File(params[0]).getAbsolutePath();
      String serverIP = params[1];
      String[] extensions = params[2].split(",");

      System.out.printf("Nouvelle sauvegarde demandée depuis %s vers le serveur %s pour les extensions %s%n", sourceDir, serverIP, Arrays.toString(extensions));

      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      String backupDate = dateFormat.format(new Date());
      String backupDir = "backup_" + backupDate;

      // Vérifier si une sauvegarde précédente a déjà eu lieu
      if (!Files.exists(Paths.get(backupDir))) {
        Files.createDirectory(Paths.get(backupDir));
      }

      Files.walk(Paths.get(sourceDir))
          .filter(path -> Files.isRegularFile(path) && hasExtension(path.getFileName().toString(), extensions))
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

      System.out.printf("Sauvegarde terminée. Les fichiers ont été sauvegardés dans le répertoire %s%n", backupDir);
      writer.write("Sauvegarde terminée. Les fichiers ont été sauvegardés dans le répertoire " + backupDir + "\n");
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static boolean hasExtension(String filename, String[] extensions) {
    for (String ext : extensions) {
      if (filename.toLowerCase().endsWith(ext.toLowerCase())) {
        return true;
      }
    }
    return false;
  }
}