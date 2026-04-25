package vn.edu.congvan.inbound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import vn.edu.congvan.common.exception.BusinessException;

class FileValidatorTest {

    private final FileValidator validator = new FileValidator();

    @Test
    void acceptsValidPdf() {
        byte[] pdf = combine("%PDF-1.4\n".getBytes(), filler(2000));
        validator.validate("doc.pdf", "application/pdf", pdf);
    }

    @Test
    void acceptsValidPng() {
        byte[] png = combine(
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, filler(1000));
        validator.validate("img.png", "image/png", png);
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> validator.validate("e.pdf", "application/pdf", new byte[0]))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FILE_EMPTY");
    }

    @Test
    void rejectsOversizeFile() {
        byte[] big = new byte[(int) FileValidator.MAX_SIZE_BYTES + 1];
        // Đặt magic header cho fair test
        big[0] = '%';
        big[1] = 'P';
        big[2] = 'D';
        big[3] = 'F';
        assertThatThrownBy(() -> validator.validate("big.pdf", "application/pdf", big))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    void rejectsDisallowedMime() {
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        "evil.exe", "application/x-msdownload", filler(100)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FILE_MIME_NOT_ALLOWED");
    }

    @Test
    void rejectsFakePdfWithPngContent() {
        byte[] pngContent = combine(
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, filler(500));
        assertThatThrownBy(() -> validator.validate("fake.pdf", "application/pdf", pngContent))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FILE_MAGIC_MISMATCH");
    }

    @Test
    void acceptsDocxAsZipMagic() {
        byte[] docx = combine(new byte[] {0x50, 0x4B, 0x03, 0x04}, filler(500));
        validator.validate(
                "doc.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docx);
    }

    @Test
    void magicBytesUtilityWorksForJpeg() {
        byte[] jpeg = combine(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, filler(100));
        assertThat(FileValidator.matchesMagicBytes("image/jpeg", jpeg)).isTrue();
        assertThat(FileValidator.matchesMagicBytes("image/png", jpeg)).isFalse();
    }

    private static byte[] filler(int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) b[i] = (byte) (i & 0x7F);
        return b;
    }

    private static byte[] combine(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
