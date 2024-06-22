package com.agilysys.StayTenantPurger.Util;

import org.bson.BsonBinaryReader;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BsonFileReader {
    DocumentCodec codec = new DocumentCodec();
    public List<Document> convertToDocuments(String path){
        List<Document> documentList=new ArrayList<>();
        try (InputStream inputStream = new FileInputStream(path)) {
            byte[] bsonBytes = inputStream.readAllBytes();
            ByteBuffer byteBuffer = ByteBuffer.wrap(bsonBytes);

            while (byteBuffer.hasRemaining()) {
                try (BsonBinaryReader reader = new BsonBinaryReader(byteBuffer)) {
                    if (reader.getCurrentBsonType() == null) {
                        reader.readBsonType();  // Initialize the reader to start reading
                    }
                    if (reader.getCurrentBsonType() == BsonType.END_OF_DOCUMENT) {
                        break;  // Break the loop if we reach the end of the document
                    }

                    Document document = codec.decode(reader, DecoderContext.builder().build());
                    documentList.add(document);
                } catch (Exception e) {
                    System.out.println(e);
                    break;  // Exit the loop if any exception occurs
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
       return documentList;
    }
    }

