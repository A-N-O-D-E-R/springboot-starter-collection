package com.anode.biojava;

import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.io.CifFileReader;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class B2StructureProvider {

    private final S3Client s3;
    private final String bucket;

    public B2StructureProvider(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public Structure getStructure(String pdbId) throws IOException {
        String key = "structures/" + pdbId.toLowerCase() + ".cif";
        try {
            byte[] data = s3.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()).readAllBytes();
            CifFileReader reader = new CifFileReader();
            return reader.getStructure(new ByteArrayInputStream(data));
        } catch (NoSuchKeyException e) {
            return null;
        } catch (Exception e) {
            throw new IOException("Failed to fetch from B2", e);
        }
    }

    public void putStructure(String pdbId, Path localFile) throws IOException {
        String key = "structures/" + pdbId.toLowerCase() + ".cif";
        s3.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build(), RequestBody.fromFile(localFile));
    }

    public void putStructure(String pdbId, InputStream data) throws IOException {
        String key = "structures/" + pdbId.toLowerCase() + ".cif";
        s3.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build(), RequestBody.fromInputStream(data, data.available()));
    }
}
