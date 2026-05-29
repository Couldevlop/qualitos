package com.openlab.qualitos.blockchain.config;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * Construit la connexion Hyperledger Fabric (mTLS) à partir du matériel MSP fourni
 * par l'environnement (générés par le {@code test-network} de fabric-samples).
 * Voir {@code infra/blockchain/README.md} pour les variables attendues.
 */
@Configuration
public class FabricGatewayConfig {

    @Value("${fabric.peer-endpoint}") String peerEndpoint;
    @Value("${fabric.peer-host-alias:}") String peerHostAlias;
    @Value("${fabric.tls-cert}") String tlsCertPath;
    @Value("${fabric.msp-id}") String mspId;
    @Value("${fabric.cert}") String certPath;
    @Value("${fabric.key}") String keyPath;
    @Value("${fabric.channel}") String channelName;
    @Value("${fabric.chaincode}") String chaincodeName;

    /** Canal gRPC TLS vers le peer (le SDK n'ouvre pas le socket lui-même). */
    @Bean
    ManagedChannel fabricGrpcChannel() throws IOException {
        SslContext tls = GrpcSslContexts.forClient()
                .trustManager(Path.of(tlsCertPath).toFile())
                .build();
        NettyChannelBuilder builder = NettyChannelBuilder.forTarget(peerEndpoint).sslContext(tls);
        if (peerHostAlias != null && !peerHostAlias.isBlank()) {
            builder.overrideAuthority(peerHostAlias);
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    Gateway fabricGateway(ManagedChannel fabricGrpcChannel)
            throws IOException, CertificateException, InvalidKeyException {
        X509Certificate cert = Identities.readX509Certificate(Files.readString(Path.of(certPath)));
        Identity identity = new X509Identity(mspId, cert);
        PrivateKey privateKey = Identities.readPrivateKey(Files.readString(Path.of(keyPath)));
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        return Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(fabricGrpcChannel)
                .evaluateOptions(o -> o.withDeadlineAfter(30, TimeUnit.SECONDS))
                .endorseOptions(o -> o.withDeadlineAfter(30, TimeUnit.SECONDS))
                .submitOptions(o -> o.withDeadlineAfter(60, TimeUnit.SECONDS))
                .commitStatusOptions(o -> o.withDeadlineAfter(1, TimeUnit.MINUTES))
                .connect();
    }

    @Bean
    Contract anchorContract(Gateway fabricGateway) {
        Network network = fabricGateway.getNetwork(channelName);
        return network.getContract(chaincodeName);
    }
}
