package de.kartax.awslauncher.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.budgets.BudgetsClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sfn.SfnClient;

@Configuration
@Getter
public class AwsConfig {

    @Value("${aws.accessKeyId}")
    private String awsAccessKeyId;

    @Value("${aws.secretAccessKey}")
    private String awsSecretAccessKey;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.budgetAccountId}")
    private String budgetAccountId;

    @Value("${aws.budgetName}")
    private String budgetName;

    @Bean
    public Ec2Client ec2Client() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
        return Ec2Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public SfnClient sfnClient() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
        return SfnClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public BudgetsClient cloudWatchClient() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
        return BudgetsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(awsRegion))
                .build();
    }

}
