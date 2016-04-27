# cassandra-ssl-client-to-node-example

This example shows how to connect to a Cassandra cluster using SSL encryption (client to node encryption). This is ideally
a continuation of the [post from The Last Picke blog](http://thelastpickle.com/blog/2015/09/30/hardening-cassandra-step-by-step-part-1-server-to-server.html)
that explains how to implement node-to-node encryption in a Cassandra cluster. There instructions do not cover the client _authentication_ that requires a slight different configuration.
I'll will use the same passwords, filenames, identifiers and conventions of that post in the following.
In particular I will use the [Cassandra Cluster Manager (CCM)](https://github.com/pcmanus/ccm.git) to provide with a working cluster.

## To make it work:

1. Follow the cited blog post to enable internode encryption. If you want, you can skip the internode part, however you will need the [`ca-cert`](http://thelastpickle.com/blog/2015/09/30/hardening-cassandra-step-by-step-part-1-server-to-server.html#byo-certificate-authority) file and provide a keystore file for you node as specified.
2. Generate a trust store named `client-truststore.jks` including the CA root certificate running `keytool -keystore client-truststore.jks -alias CARoot -importcert -file ca-cert -keypass mypass -storepass truststorepass -noprompt`
3. Put `client-truststore.jks` under `resources` folder of this project.
   This way client is shares your Certification Authority with any node in the cluster. This will allow encrypted communication after the handshake.
4. Change the [client_encryption_options|https://github.com/apache/cassandra/blob/trunk/conf/cassandra.yaml#L897-L911] section of `cassandra-yaml` for every node of the cluster as follows to enable client encryption (but not authentication):

   ```yaml
   client_encryption_options:
   enabled: true
   keystore: /Users/Giampaolo/.ccm/sslverify/$NODE/conf/server-keystore.jks
   keystore_password: awesomekeypass
   optional: false
   ```
   
4. Run `ClientToNode` class `main` method to verify that the setup is correct.

## Code details

Inside `main` method, trust store file and password are set programmatically, using `getCluster(String trustStoreLocation, String trustStorePassword, String host)` method

```java
    cluster = getCluster("/client-truststore.jks", "truststorepass", host);
    session = cluster.newSession();
```

where the method is defined as follows:

```java
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
```

However you can simplify code if you pass trustStore and related password through command line using `-Djavax.net.ssl.trustStore=/Users/Giampaolo/dev/cassandra-ssl-client-to-node-example/target/classes/client-truststore.jks -Djavax.net.ssl.trustStorePassword=truststorepass`
In this case, you neee to build the cluster only with the SSL option enabled:
```java
 Cluster cluster = Cluster.builder()
      .addContactPoint(host)
      .withSSL()
      .build();
``


### To invoke from command line type
If you want to invoke `ClientToNode` class without the help of an IDE, you can run the following commands in the root folder of the project:

1. `mvn package appassembler:assemble`
2. `sh target/appassembler/bin/clientToNode 127.0.0.1`

SSL parameters are specified in `pom.xml` at `extraJvmArguments` tag.

### If something goes wrong
To enable debug SSL run with `-Djavax.net.debug=ssl`. Don't forget to apply changes to all nodes and restart the cluster.


## Using cqlsh
If you allow client encryption, `cqlsh` won't work anymore as is. You need to follow these steps to connect to cluster.

1. Run
   ```keytool -importkeystore \
              -srckeystore node1-server-keystore.jks  \
              -destkeystore node1.p12 \
              -deststoretype PKCS12 \
              -alias node1 \
              -srcstorepass awesomepass \
              -srckeypass awesomepass \
              -deststorepass clientpassword \
              -destkeypass clientpassword`
   ```

   to convert the server certificate to the [PKCS12|(https://en.wikipedia.org/wiki/PKCS_12)] format.
2. Convert the `PKCS#12` to the [PEM|(https://en.wikipedia.org/wiki/Privacy-enhanced_Electronic_Mail)] format with this command `openssl pkcs12 -in node1.p12 -out node1.pem -nodes`. The option `-nodes` is not the word "nodes" so it's unrelated to Cassandra nodes, but rather is "no DES" which means that OpenSSL will not encrypt the private key in a `PKCS#12` file.
3. Move the file `node1.pem` to the folder `~/.cassandra`. This folder holds the command history for `cqlsh`, `cli` and `nodetool` session. It contains also the `.cqlshrs` file that specifies different settings for the cqlsh sessions.
   The `.cassandra` directory holds command history for your `cqlsh`, `cli`, and `nodetool` sessions. It is also the default location for the `.cqlshrc` file, which allows you to persist various [settings|(http://docs.datastax.com/en/cql/3.1/cql/cql_reference/cqlshrc.html)] for your `cqlsh` sessions.
4  Edit or create the `~/.cassandra/cqlshrc` file adding the following sections if not present:

   ```bash
   [authentication]
   username =
   password =

   [connection]
   hostname = localhost
   port = 9042
   factory = cqlshlib.ssl.ssl_transport_factory

   [ssl]
   certfile = /Users/Giampaolo/.cassandra/node1.pem
   validate = true ## Optional, true by default.
   ```

5. connect with `ccm node1 cqlsh --ssl`. `cqlsh` allows you to pass the file path also as the `SSL_CERTFILE` variable or modifying the `cqlshrc` file like this

In alternative, you can specify certificate for every node in the `[certfiles]` section of

   ```bash
   [authentication]
   username =
   password =

   [connection]
   hostname = localhost
   port = 9042
   factory = cqlshlib.ssl.ssl_transport_factory

   [ssl]
   validate = true ## Optional, true by default.

   [certfiles]
   127.0.0.1 = /Users/Giampaolo/.cassandra/node1.pem
   127.0.0.2 = /Users/Giampaolo/.cassandra/node1.pem
   127.0.0.3 = /Users/Giampaolo/.cassandra/node1.pem
   ```




## References
[What is a Pem file and how does it differ from other OpenSSL Generated Key File Formats|(http://serverfault.com/a/9717/329639)]
[Creating and using the cqlshrc file|(http://docs.datastax.com/en/cql/3.1/cql/cql_reference/cqlshrc.html)]
[Using cqlsh with SSL encryption|(http://docs.datastax.com/en/cassandra/2.1/cassandra/security/secureCqlshSSL_t.html)]
[“~/.cassandra” folder - what is it used for?|http://stackoverflow.com/q/30869921/1360888)]

