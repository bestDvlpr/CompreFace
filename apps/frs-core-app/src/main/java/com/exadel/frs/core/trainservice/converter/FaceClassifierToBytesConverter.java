package com.exadel.frs.core.trainservice.converter;

import com.exadel.frs.core.trainservice.component.classifiers.FaceClassifier;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

@WritingConverter
public class FaceClassifierToBytesConverter implements Converter<FaceClassifier, byte[]> {
    @SneakyThrows
    @Override
    public byte[] convert(FaceClassifier classifier) {
        try (val baos = new ByteArrayOutputStream();
             val oos = new ObjectOutputStream(baos)
        ) {
            oos.writeObject(classifier);

            return baos.toByteArray();
        }
    }
}
