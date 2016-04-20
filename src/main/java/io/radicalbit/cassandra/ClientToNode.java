package io.radicalbit.cassandra;

import com.datastax.driver.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

public class ClientToNode {

  public static Logger logger = LoggerFactory.getLogger(ClientToNode.class);

  public static void main(String[] args) {

    ClientToNode l = new ClientToNode();
    if (args.length != 1) {
      System.out.println("Specify host name/IP");
      System.exit(-1);
    }

    l.execute(args[0]);
  }

  private void execute(String host) {
    Cluster cluster;
    Session session;

    logger.info("Login into {}", host);

    cluster = getCluster("/client-truststore.jks", "truststorepass", host);

    // Alternate way to use a truststore passing location and password from command line
    // cluste = getCluster();


    // Connect to the cluster and keyspace "ssl"
    session = cluster.newSession();

    session.execute(" CREATE KEYSPACE IF NOT EXISTS ssl WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};");


    session.execute("use ssl;");
    session.execute("CREATE TABLE IF NOT EXISTS songs (title text, lyrics text, PRIMARY KEY (title))");

    // Insert one record into the users table
    session.execute("INSERT INTO songs (title, lyrics) VALUES ('Don''t believe the hype', 'Caught you lookin for the same thing It''s a new thing check out this I bring')");

    // Use select to get the user we just entered
    ResultSet results = session.execute("SELECT * FROM songs Limit 200;");
    for (Row row : results) {
      logger.info("*** data extracted ***");
      logger.info("{} - {}", row.getString("title"), row.getString("lyrics"));
    }

    session.close();

    // Clean up the connection by closing it
    cluster.close();
  }


  // This method is an example of loading a truststore from a resource, decoding it with its password.
  private Cluster getCluster(String trustStoreLocation, String trustStorePassword, String host) {
    Cluster cluster;
    SSLContext sslcontext = null;

    try {

      InputStream is = ClientToNode.class.getResourceAsStream(trustStoreLocation);
      KeyStore keystore = KeyStore.getInstance("jks");
      char[] pwd = trustStorePassword.toCharArray();
      keystore.load(is, pwd);

      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(keystore);
      TrustManager[] tm = tmf.getTrustManagers();

      sslcontext = SSLContext.getInstance("TLS");
      sslcontext.init(null, tm, null);
    } catch (KeyStoreException kse) {
      logger.error(kse.getMessage(), kse);
    } catch (CertificateException e) {
      logger.error(e.getMessage(), e);
    } catch (NoSuchAlgorithmException e) {
      logger.error(e.getMessage(), e);
    } catch (KeyManagementException e) {
      logger.error(e.getMessage(), e);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }


    JdkSSLOptions sslOptions = JdkSSLOptions.builder()
      .withSSLContext(sslcontext)
      .build();

    cluster = Cluster.builder()
      .addContactPoint(host)
      .withSSL(sslOptions)
      .build();
    return cluster;
  }

  private Cluster getCluster(String host) {
    // -Djavax.net.ssl.trustStore=/Users/Giampaolo/dev/cassandra-ssl-client-to-node-example/target/classes/client-truststore.jks -Djavax.net.ssl.trustStorePassword=truststorepass
    Cluster cluster = Cluster.builder()
      .addContactPoint(host)
      .withSSL()
      .build();

    return cluster;
  }
}