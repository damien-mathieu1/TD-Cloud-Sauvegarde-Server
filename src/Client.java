import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.lang.System.exit;

public class Client {
  public static void main(String[] args) {
      // ajoutér ici


    Scanner scanner = new Scanner(System.in);
    System.out.print("Entrez l'adresse IP du serveur: ");
    String serverIP = scanner.nextLine();

    System.out.println("Si vous souhaitez envoyé une sauvegarde sur le server entrez 1 ");
    System.out.println("Si vous souhaitez reprendre une sauvegarde du server entrez 2 ");
    System.out.println("Réponse du client : ");
    int value = scanner.nextInt();
    scanner.nextLine();
    Socket socket;
    DataOutputStream dos;
    BufferedReader br;
    BufferedReader kb;
    try{
        socket = new Socket(serverIP, 8080);

        // to send data to the server
        dos = new DataOutputStream(socket.getOutputStream());

        // to read data coming from the server
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // to read data from the keyboard
        kb = new BufferedReader(new InputStreamReader(System.in));

    }
    catch (Exception e){
        throw new RuntimeException(e);
    }

    if (value == 1) {
        faireUneSauvegarde(dos,br, socket);
        exit(0);
    }
    else if (value == 2) {
        reprendreUneSauvegarde(dos,br,socket,kb);
        exit(0);
    }
    else{
        System.out.println("Veuillez Entrez 1 ou 2 uniquement, merci de relancer le programme.");
        exit(0);
    }
  }

  private static void faireUneSauvegarde(DataOutputStream dos, BufferedReader br, Socket socket){
      Scanner scanner = new Scanner(System.in);
      try {
          // envoi la valeur donc 1 puisque il veut mettre une nouvelle sauvegarde :
          dos.write((1 + "\n").getBytes());

          System.out.println("Extensions deja compris dans extensions.txt , réécrivez les si vous souhaité les gardez encore : ");
          String str;
          System.out.println("----Début----");

          while( !(str = br.readLine()).equals("fin") ){
              System.out.println(str);
          }
          System.out.println("----"+str+"----"); // ou mettre System.out.println("----fin----");
          System.out.print("Entrez les extensions à sauvegarder au format (txt,jpeg,jpg,...) : ");
          String extensionsDeUser = scanner.nextLine();

          // envoi les extensions à sauvegarder au server
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
                              // Si c'est un répertoire, ajoutez simplement une entrée au ZIP
                              zipOutputStream.putNextEntry(new ZipEntry(relativePath.toString() + "/"));
                              zipOutputStream.closeEntry();
                          } else {
                              // Si c'est un fichier, copiez-le dans le ZIP
                              zipOutputStream.putNextEntry(new ZipEntry(relativePath.toString()));
                              Files.copy(filePath, zipOutputStream);
                              zipOutputStream.closeEntry();
                          }
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  });
          System.out.println("La sauvegarde c'est bien finis");
      }catch (IOException e) {
          e.printStackTrace();
      }
  }

  private static void reprendreUneSauvegarde(DataOutputStream dos, BufferedReader br, Socket socket,BufferedReader kb){
      Scanner scanner = new Scanner(System.in);
      try {
          String str;
          // envoie de 2 au server donc
          dos.writeBytes("2" + "\n");

          // repeat as long as exit
          // is not typed at client
          // tant que le server n'envoi pas "Choisissez" on continue de lire l'informations du server
          while(!((str = br.readLine()).equals("Choisissez :"))){
              // receive from the server
              System.out.println(str);
          }
          System.out.println(str);
          // envoi au server le dossier que il veut récupérer
          String dossierARecup = kb.readLine();
          dos.writeBytes(dossierARecup+ "\n");

          if((str = br.readLine()).equals("le dossier que vous avez choissis n'existe pas")){
              System.out.println(str);
              return;
          }
          System.out.println("Ou voulez vous mettre la sauvegarde ? (indiquer le chemin complet) avec en final le nom du nouveau dossier : ");
          String destinationDir = scanner.nextLine();
          InputStream inputStream = socket.getInputStream();
          ZipInputStream zipInputStream = new ZipInputStream(inputStream);
          ZipEntry zipEntry;
          System.out.println("zipInputStream.getNextEntry() : " + zipInputStream.getNextEntry());
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
                          byte[] buffer = new byte[1024];
                          int bytesRead;
                          while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                              outputStream.write(buffer, 0, bytesRead);
                          }
                      }
                  }
              }
              zipInputStream.closeEntry();
          }
          System.out.printf("Dossier bien importé");
      } catch (IOException e) {
          e.printStackTrace();
      }
  }
}
