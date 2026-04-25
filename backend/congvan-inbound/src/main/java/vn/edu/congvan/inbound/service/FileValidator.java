package vn.edu.congvan.inbound.service;

import java.util.Set;
import org.springframework.stereotype.Service;
import vn.edu.congvan.common.exception.BusinessException;

/**
 * Validate file upload theo defense-in-depth:
 * <ol>
 *   <li>Size ≤ 50MB
 *   <li>MIME type trong whitelist
 *   <li>Magic bytes khớp với MIME (chống fake extension)
 * </ol>
 *
 * <p>Whitelist phase 3: PDF, JPEG, PNG, DOC/DOCX, XLS/XLSX.
 */
@Service
public class FileValidator {

    public static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;

    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of(
                    "application/pdf",
                    "image/jpeg",
                    "image/png",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    /**
     * Validate file. Ném {@link BusinessException} nếu vi phạm.
     *
     * @param filename tên file (chứa extension — chỉ để debug log)
     * @param mimeType MIME type do client khai báo
     * @param content  nội dung file
     */
    public void validate(String filename, String mimeType, byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException("FILE_EMPTY", "File rỗng: " + filename);
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new BusinessException(
                    "FILE_TOO_LARGE",
                    "File quá lớn (" + content.length + " > 50MB): " + filename);
        }
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new BusinessException(
                    "FILE_MIME_NOT_ALLOWED",
                    "Định dạng file không được phép: " + mimeType + " (file: " + filename + ")");
        }
        if (!matchesMagicBytes(mimeType.toLowerCase(), content)) {
            throw new BusinessException(
                    "FILE_MAGIC_MISMATCH",
                    "Nội dung file không khớp với loại " + mimeType + " (file: " + filename + ")");
        }
    }

    /** So sánh magic bytes đầu file với MIME khai báo. */
    static boolean matchesMagicBytes(String mime, byte[] content) {
        if (content.length < 4) return false;
        return switch (mime) {
            case "application/pdf" -> startsWith(content, "%PDF".getBytes());
            case "image/jpeg" -> startsWith(content, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "image/png" -> startsWith(
                    content, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            // Office: DOCX/XLSX là ZIP (PK\x03\x04), DOC/XLS là OLE compound (D0CF11E0A1B11AE1)
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> startsWith(
                    content, new byte[] {0x50, 0x4B, 0x03, 0x04});
            case "application/msword", "application/vnd.ms-excel" -> startsWith(
                    content,
                    new byte[] {
                        (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                        (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
                    });
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
