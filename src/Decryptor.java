import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import java.io.*;
import java.nio.file.*;
import java.security.Key;
import java.util.Scanner;

public class Decryptor {
  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);

    // Chemin du répertoire contenant les fichiers chiffrés
    System.out.print("Chemin du répertoire contenant les fichiers chiffrés : ");
    String encryptedDir = scanner.nextLine();

    // Chemin du fichier contenant la clé
    System.out.print("Chemin du fichier contenant la clé : ");
    String keyFile = scanner.nextLine();

    // Chemin du répertoire où les fichiers déchiffrés seront enregistrés
    System.out.print("Nom du répertoire de destination pour les fichiers déchiffrés : ");
    String destinationDirPath = scanner.nextLine();

    // Confirmer le déchiffrement
    System.out.print("Confirmez-vous le déchiffrement ? (O/N) : ");
    String confirmation = scanner.nextLine();
    if (!confirmation.equalsIgnoreCase("O")) {
      System.out.println("Déchiffrement annulé.");
      return;
    }

    // Charger la clé à partir du fichier
    Key secretKey = loadKeyFromFile(keyFile);

    // Déchiffrer les fichiers
    decryptFiles(encryptedDir, destinationDirPath, secretKey);
  }

  private static Key loadKeyFromFile(String filename) {
    try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(filename))) {
      return (Key) inputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static void decryptFiles(String encryptedDir, String destinationDirPath, Key secretKey) {
    try {
      Files.walk(Paths.get(encryptedDir))
          .filter(path -> Files.isRegularFile(path))
          .forEach(path -> {
            String fileName = path.getFileName().toString();
            int underscoreIndex = fileName.indexOf("_");

            String backupName;
            if (underscoreIndex != -1) {
              backupName = fileName.substring(0, underscoreIndex); // Extraire le nom de la sauvegarde
            } else {
              backupName = fileName; // Utiliser le nom de fichier comme nom de la sauvegarde
            }

            String decryptedDirPath = destinationDirPath + File.separator + "dossierDechiffrer" + File.separator + backupName;

            File decryptedDir = new File(decryptedDirPath);
            if (!decryptedDir.exists()) {
              decryptedDir.mkdirs(); // Créer le dossier s'il n'existe pas
            }

            String destPath = decryptedDirPath + File.separator + fileName;
            decryptFile(path.toFile(), new File(destPath), secretKey);
          });

      System.out.println("Déchiffrement terminé. Les fichiers ont été enregistrés dans le répertoire " + destinationDirPath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void decryptFile(File inputFile, File outputFile, Key secretKey) {
    try {
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // Ajout du mode et du padding
      cipher.init(Cipher.DECRYPT_MODE, secretKey);

      try (FileInputStream inputStream = new FileInputStream(inputFile);
          CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
          FileOutputStream outputStream = new FileOutputStream(outputFile)) {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
