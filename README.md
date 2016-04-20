# cassandra-ssl-client-to-node-example

This example shows how to connect to a Cassandra cluster using SSL encryption (client to node encryption)

## To make it work:

1. Follow [instructions](http://thelastpickle.com/blog/2015/09/30/hardening-cassandra-step-by-step-part-1-server-to-server.html) to enable internode encryption. If you want, you can skip internode part, however you will need the `ca-cert` file and provide a keystore file for you node
2. Generate the trust store with `keytool -keystore client-truststore.jks -alias CARoot -importcert -file ca-cert -keypass mypass -storepass truststorepass -noprompt`
3. Put `client-truststore.jks` under `resources` folder
4. Change `cassandra-yaml` as follows:

   ```yaml
   client_encryption_options:
   enabled: true
   keystore: /Users/Giampaolo/.ccm/sslverify/node2/conf/server-keystore.jks
   keystore_password: awesomekeypass
   optional: false
   ```
   
4. Run ClientToNode class

Inside `main` method trust store file and password are set with code, using `getCluster(String host)` method you can pass parameter with command line using `-Djavax.net.ssl.trustStore=/Users/Giampaolo/dev/cassandra-ssl-client-to-node-example/target/classes/client-truststore.jks -Djavax.net.ssl.trustStorePassword=truststorepass`

## To invoke from command line type

1. `mvn package appassembler:assemble`
2. `sh target/appassembler/bin/clientToNode 127.0.0.1`

SSL parameters are in `pom.xml` at `extraJvmArguments` tag.

## If something goes wrong
To enable debug SSL run with `-Djavax.net.debug=ssl`. Don't forget to apply changes to all nodes and restart the cluster.

## Note
All passwords, filenames, certificate identifiers come from the example linked at the very beginning.


### Using cqlsh
If you allow client encryption, cqlsh won't work anymore as is. You need to follow these steps.

1. run `keytool -importkeystore -srckeystore node1-server-keystore.jks  -destkeystore node1.p12 -deststoretype PKCS12`
2. run `openssl pkcs12 -in node1.p12 -out node1.pem -nodes`
  * move `node1.pem` to `.cassandra`
3. edit the file `.cassandra/cqlshrc`, insert these lines

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

4. connect with `ccm node1 cqlsh --ssl`


