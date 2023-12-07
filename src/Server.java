import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class Server {
  public static void main(String[] args) {
    System.out.println("Serveur en attente de connexions...");

    try {
      // Chargement du keystore contenant le certificat du serveur
      char[] keystorePassword = "password".toCharArray();
      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(new FileInputStream("serverKeystore.jks"), keystorePassword);

      // Initialisation du contexte SSL
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
      keyManagerFactory.init(keyStore, keystorePassword);

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

      // Création d'un serveur SSLSocket
      SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
      SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(8080);

      while (true) {
        SSLSocket clientSocket = (SSLSocket) sslServerSocket.accept();
        System.out.println("Client connecté depuis " + clientSocket.getInetAddress());
        handleConnection(clientSocket);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

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

      if (params.length != 3) {
        System.out.println("Format de données invalide.");
        return;
      }

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

      // Générer une clé secrète pour le chiffrement AES
      Key secretKey = generateSecretKey();

      // Sauvegarder la clé dans un fichier
      saveKeyToFile(secretKey, backupDir);

      // Chiffrer les fichiers sauvegardés avec la clé secrète
      Files.walk(Paths.get(sourceDir))
          .filter(path -> Files.isRegularFile(path) && hasExtension(path.getFileName().toString(), extensions))
          .forEach(path -> {
            Path relativePath = Paths.get(sourceDir).relativize(path);
            String destPath = backupDir + File.separator + relativePath.toString();

            try {
              Files.createDirectories(Paths.get(destPath).getParent());

              // Chiffrer le fichier avec AES
              encryptFile(path.toFile(), new File(destPath), secretKey);
            } catch (IOException e) {
              e.printStackTrace();
            }
          });

      System.out.printf("Sauvegarde terminée. Les fichiers ont été sauvegardés et chiffrés dans le répertoire %s%n", backupDir);
      writer.write("Sauvegarde terminée. Les fichiers ont été sauvegardés et chiffrés dans le répertoire " + backupDir + "\n");
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

  private static Key generateSecretKey() {
    try {
      // Générer une clé AES
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(128);
      return keyGenerator.generateKey();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private static void saveKeyToFile(Key key, String backupDir) {
    try {
      // Construisez le chemin du fichier de clé en utilisant Paths
      Path keyFilePath = Paths.get(backupDir, "..", "secretKey.ser");

      // Assurez-vous que le dossier parent existe
      Files.createDirectories(keyFilePath.getParent());

      try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(keyFilePath.toFile()))) {
        outputStream.writeObject(key);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void encryptFile(File inputFile, File outputFile, Key secretKey) {
    try {
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);

      try (FileInputStream inputStream = new FileInputStream(inputFile);
          FileOutputStream outputStream = new FileOutputStream(outputFile);
          CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          cipherOutputStream.write(buffer, 0, bytesRead);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
