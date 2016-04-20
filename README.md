# cassandra-ssl-client-to-node-example

This example shows how to connect to a Cassandra cluster using SSL encryption (client to node encryption)

To make it work:
1. Follow [instructions](http://thelastpickle.com/blog/2015/09/30/hardening-cassandra-step-by-step-part-1-server-to-server.html) to enable internode encryption. If you want, you can skip internode part, however you will need the `ca-cert` file;
2. Generate the trust store with `keytool -keystore client-truststore.jks -alias CARoot -importcert -file ca-cert -keypass mypass -storepass truststorepass -noprompt`
3. Put `client-truststore.jks` under `resources` folder
4. Run ClientToNode class

Inside `main` method trust store file and password are set with code, using `getCluster(String host)` method you can pass parameter with command line using `-Djavax.net.ssl.trustStore=/Users/Giampaolo/dev/cassandra-ssl-client-to-node-example/target/classes/client-truststore.jks -Djavax.net.ssl.trustStorePassword=truststorepass`

To invoke from command line type
1. `mvn package appassembler:assemble`
2. `sh target/appassembler/bin/clientToNode 127.0.0.1`

SSL parameters are in `pom.xml` at `extraJvmArguments` tag.

To debug SSL run with `-Djavax.net.debug=ssl` 