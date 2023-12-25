import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.lang.System.exit;

public class Client {

    // Clé XOR pour le chiffrement et le déchiffrement
    private static final byte XOR_KEY = 0x0F;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Entrez l'adresse IP du serveur: ");
        String serverIP = scanner.nextLine();

        System.out.println("Si vous souhaitez envoyer une sauvegarde sur le serveur, entrez 1 ");
        System.out.println("Si vous souhaitez reprendre une sauvegarde du serveur, entrez 2 ");
        System.out.println("Réponse du client : ");
        int value = scanner.nextInt();
        scanner.nextLine(); // Pour consommer le retour à la ligne

        Socket socket;
        DataOutputStream dos;
        BufferedReader br;
        BufferedReader kb;

        try {
            socket = new Socket(serverIP, 8080);
            dos = new DataOutputStream(socket.getOutputStream());
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            kb = new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (value == 1) {
            faireUneSauvegarde(dos, br, socket);
            exit(0);
        } else if (value == 2) {
            reprendreUneSauvegarde(dos, br, socket, kb);
            exit(0);
        } else {
            System.out.println("Veuillez entrer 1 ou 2 uniquement. Merci de relancer le programme.");
            exit(0);
        }
    }

    private static void faireUneSauvegarde(DataOutputStream dos, BufferedReader br, Socket socket) {
        Scanner scanner = new Scanner(System.in);
        try {
            dos.write((1 + "\n").getBytes());

            System.out.println("Extensions déjà comprises dans extensions.txt, réécrivez-les si vous souhaitez les garder encore : ");
            String str;
            System.out.println("----Début----");

            while (!(str = br.readLine()).equals("fin")) {
                System.out.println(str);
            }
            System.out.println("----" + str + "----");
            System.out.print("Entrez les extensions à sauvegarder au format (txt,jpeg,jpg,...) : ");
            String extensionsDeUser = scanner.nextLine();
            dos.writeBytes(extensionsDeUser + "\n");

            System.out.print("Entrez le chemin du dossier à sauvegarder: ");
            String sourceDir = scanner.nextLine();

            System.out.println("Demande de sauvegarde envoyée au serveur.");
            OutputStream outputStream = socket.getOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

            Files.walk(Paths.get(sourceDir))
                    .forEach(filePath -> {
                        try {
                            Path relativePath = Paths.get(sourceDir).relativize(filePath);

                            if (Files.isDirectory(filePath)) {
                                zipOutputStream.putNextEntry(new ZipEntry(relativePath.toString() + "/"));
                                zipOutputStream.closeEntry();
                            } else {
                                zipOutputStream.putNextEntry(new ZipEntry(relativePath.toString()));
                                encryptAndCopyFile(filePath, zipOutputStream);
                                zipOutputStream.closeEntry();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            System.out.println("La sauvegarde s'est bien terminée.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void reprendreUneSauvegarde(DataOutputStream dos, BufferedReader br, Socket socket, BufferedReader kb) {
        Scanner scanner = new Scanner(System.in);
        try {
            String str;
            dos.writeBytes("2" + "\n");

            while (!((str = br.readLine()).equals("Choisissez :"))) {
                System.out.println(str);
            }
            System.out.println(str);

            String dossierARecup = kb.readLine();
            dos.writeBytes(dossierARecup + "\n");

            if ((str = br.readLine()).equals("le dossier que vous avez choisi n'existe pas")) {
                System.out.println(str);
                return;
            }

            System.out.println("Où voulez-vous mettre la sauvegarde ? (Indiquez le chemin complet avec en fin le nom du nouveau dossier) : ");
            String destinationDir = scanner.nextLine();
            InputStream inputStream = socket.getInputStream();
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = zipEntry.getName();
                System.out.println("entryName : " + entryName);

                if (entryName != null && !entryName.isEmpty()) {
                    Path filePath = Paths.get(destinationDir, entryName);

                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                            decryptAndCopyFile(zipInputStream, outputStream);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }

            System.out.println("Dossier bien importé.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void encryptAndCopyFile(Path sourcePath, ZipOutputStream zipOutputStream) throws IOException {
        try (InputStream inputStream = Files.newInputStream(sourcePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // XOR encryption
                for (int i = 0; i < bytesRead; i++) {
                    buffer[i] = (byte) (buffer[i] ^ XOR_KEY);
                }
                zipOutputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private static void decryptAndCopyFile(ZipInputStream zipInputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
            // XOR decryption
            for (int i = 0; i < bytesRead; i++) {
                buffer[i] = (byte) (buffer[i] ^ XOR_KEY);
            }
            outputStream.write(buffer, 0, bytesRead);
        }
    }
}
