import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.Scanner;

public class Client {
  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);

    System.out.print("Entrez le chemin du dossier à sauvegarder: ");
    String sourceDir = scanner.nextLine();

    System.out.print("Entrez l'adresse IP du serveur: ");
    String serverIP = scanner.nextLine();

    System.out.print("Entrez les extensions de fichiers à sauvegarder (séparées par des virgules): ");
    String extensions = scanner.nextLine();

    try {
      // Initialisation du contexte SSL
      SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
      SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(serverIP, 8080);

      // Obtention des flux d'entrée/sortie
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      // Envoi des données au serveur
      String data = sourceDir + "|" + serverIP + "|" + extensions;
      writer.write(data + "\n");
      writer.flush();

      System.out.println("Demande de sauvegarde envoyée au serveur.");
      System.out.println("Réponse du serveur :");
      String response = reader.readLine();
      System.out.println(response);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
