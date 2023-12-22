import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

import static java.lang.System.exit;

public class Client {
    public static void main(String[] args) {
        try {
            SecretKey secretKey = null;
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            char[] keyStorePassword = "yourKeyStorePassword".toCharArray();
            Path keyStorePath = Paths.get("keystore.jceks");

            // If the KeyStore doesn't exist, create it and generate a new secret key
            if (!Files.exists(keyStorePath)) {
                keyStore.load(null, null);

                // Generate a new secret key
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128);
                secretKey = keyGenerator.generateKey();

                // Store the secret key in the KeyStore
                KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
                KeyStore.ProtectionParameter entryPassword = new KeyStore.PasswordProtection(
                        "yourKeyPassword".toCharArray());
                keyStore.setEntry("mySecretKey", secretKeyEntry, entryPassword);

                // Save the KeyStore to a file
                try (OutputStream keyStoreOutputStream = Files.newOutputStream(keyStorePath)) {
                    keyStore.store(keyStoreOutputStream, keyStorePassword);
                }
            } else {
                // Load the KeyStore from a file
                try (InputStream keyStoreInputStream = Files.newInputStream(keyStorePath)) {
                    keyStore.load(keyStoreInputStream, keyStorePassword);
                }

                // Retrieve the secret key from the KeyStore
                KeyStore.ProtectionParameter entryPassword = new KeyStore.PasswordProtection(
                        "yourKeyPassword".toCharArray());
                KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry("mySecretKey",
                        entryPassword);
                secretKey = secretKeyEntry.getSecretKey();
                System.out.println("Secret key retrieved: " + secretKey);
            }

            Scanner scanner = new Scanner(System.in);
            System.out.print("Entrez l'adresse IP du serveur: ");
            String serverIP = scanner.nextLine();
            Cipher cipher = Cipher.getInstance("AES");
            System.out.println("cipher padding : " + cipher.getBlockSize());

            System.out.println("Si vous souhaitez envoyé une sauvegarde sur le server entrez 1 ");
            System.out.println("Si vous souhaitez reprendre une sauvegarde du server entrez 2 ");
            System.out.println("Réponse du client : ");
            int value = scanner.nextInt();
            scanner.nextLine();
            Socket socket;
            DataOutputStream dos;
            BufferedReader br;
            BufferedReader kb;
            try {
                socket = new Socket(serverIP, 8080);

                // to send data to the server
                dos = new DataOutputStream(socket.getOutputStream());

                // to read data coming from the server
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // to read data from the keyboard
                kb = new BufferedReader(new InputStreamReader(System.in));

            } catch (Exception e) {
                System.out.println("Une erreur est survenue lors de la connexion au serveur.");
                scanner.close();
                throw new RuntimeException(e);
            }

            if (value == 1) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                faireUneSauvegarde(dos, br, socket, cipher);
                exit(0);
            } else if (value == 2) {
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                reprendreUneSauvegarde(dos, br, socket, kb, cipher);
                exit(0);
            } else {
                System.out.println("Veuillez Entrez 1 ou 2 uniquement, merci de relancer le programme.");
                exit(0);
            }
            scanner.close();
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException | IOException
                | java.security.cert.CertificateException | NoSuchPaddingException | InvalidKeyException e) {
            e.printStackTrace();

        }
    }

    private static void faireUneSauvegarde(DataOutputStream dos, BufferedReader br, Socket socket, Cipher cipher) {
        Scanner scanner = new Scanner(System.in);
        try {
            // envoi la valeur donc 1 puisque il veut mettre une nouvelle sauvegarde :
            dos.write((1 + "\n").getBytes());

            System.out.println(
                    "Extensions deja compris dans extensions.txt , réécrivez les si vous souhaité les gardez encore : ");
            String str;
            System.out.println("----Début----");

            while (!(str = br.readLine()).equals("fin")) {
                System.out.println(str);
            }
            System.out.println("----" + str + "----"); // ou mettre System.out.println("----fin----");
            System.out.print("Entrez les extensions à sauvegarder au format (txt,jpeg,jpg,...) : ");
            String extensionsDeUser = scanner.nextLine();

            // envoi les extensions à sauvegarder au server
            dos.writeBytes(extensionsDeUser + "\n");

            System.out.print("Entrez le chemin du dossier à sauvegarder: ");
            String sourceDir = scanner.nextLine();

            System.out.println("Demande de sauvegarde envoyée au serveur.");
            OutputStream outputStream = socket.getOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

            System.out.println("sourceDir : " + sourceDir + " Paths.get(sourceDir) : " + Paths.get(sourceDir));
            Files.walk(Paths.get(sourceDir)) // on parcours le dossier
                    .filter(path -> !Files.isDirectory(path)) // on filtre pour ne pas prendre les dossiers
                    .forEach(path -> {
                        try {
                            String entryName = path.toString().substring(sourceDir.length() + 1);
                            System.out.println("entryName : " + entryName);
                            ZipEntry zipEntry = new ZipEntry(entryName);
                            zipOutputStream.putNextEntry(zipEntry);
                            if (!Files.isDirectory(path)) {
                                CipherOutputStream cipherOutputStream = new CipherOutputStream(zipOutputStream, cipher);
                                Files.copy(path, cipherOutputStream);
                                cipherOutputStream.flush();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            zipOutputStream.close();
            System.out.println("Sauvegarde terminée.");
        } catch (IOException e) {

        }
        scanner.close();
    }

    private static void reprendreUneSauvegarde(DataOutputStream dos, BufferedReader br, Socket socket,
            BufferedReader kb, Cipher cipher) {
        Scanner scanner = new Scanner(System.in);
        try {
            String str;
            // envoie de 2 au server donc
            dos.writeBytes("2" + "\n");

            // repeat as long as exit
            // is not typed at client
            // tant que le server n'envoi pas "Choisissez" on continue de lire
            // l'informations du server
            while (!((str = br.readLine()).equals("Choisissez :"))) {
                // receive from the server
                System.out.println(str);
            }
            System.out.println(str);
            // envoi au server le dossier que il veut récupérer
            String dossierARecup = kb.readLine();
            dos.writeBytes(dossierARecup + "\n");

            if ((str = br.readLine()).equals("le dossier que vous avez choissis n'existe pas")) {
                System.out.println(str);
                return;
            }
            System.out.println(
                    "Ou voulez vous mettre la sauvegarde ? (indiquer le chemin complet) avec en final le nom du nouveau dossier : ");
            String destinationDir = scanner.nextLine();
            InputStream inputStream = socket.getInputStream();
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = zipEntry.getName();
                System.out.println("entryName : " + entryName);

                if (entryName != null && !entryName.isEmpty()) {
                    Path filePath = Paths.get(destinationDir, entryName);

                    // Si l'entrée est un répertoire, assurez-vous de le créer
                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        // Si c'est un fichier, créez le fichier et copiez les données
                        Files.createDirectories(filePath.getParent());
                        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                            CipherInputStream cipherInputStream = new CipherInputStream(zipInputStream, cipher);
                            byte[] buffer = new byte[cipher.getBlockSize()];
                            int bytesRead;
                            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            cipherInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            System.out.printf("Dossier bien importé");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
