package ru.nikidzawa.datingapp.configs.qrCode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class QrCodeGenerator {
    public String generate (String path, String eventId) {
        UUID uuid = UUID.randomUUID();
        String token = uuid.toString();
        String url = "https://thatgirloasis.ru/checkMemberStatus/" + eventId + "/" + token;
        String filePath = "QRCode-" + path + ".png";
        int size = 250;
        String fileType = "png";

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size, hints);
            Path qrPath = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(matrix, fileType, qrPath);
        } catch (WriterException | IOException e) {
            System.out.println("Ошибка создания QR Code: " + e.getMessage());
        }
        return filePath;
    }
}
