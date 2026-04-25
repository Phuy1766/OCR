-- =========================================================================
-- V8 — Seed 29 loại văn bản hành chính theo Phụ lục III NĐ 30/2020/NĐ-CP.
-- Thứ tự display_order tuân theo thứ tự trong văn bản gốc.
-- Viết tắt tuân theo Phụ lục I NĐ 30/2020 (dùng trong số hiệu VB).
-- =========================================================================

INSERT INTO document_types (code, abbreviation, name, description, display_order) VALUES
    ('NGHI_QUYET',       'NQ',   'Nghị quyết',          'Văn bản quyết định các vấn đề lớn do tập thể ban hành.', 10),
    ('QUYET_DINH',       'QĐ',   'Quyết định',          'Văn bản quyết định về cán bộ, tổ chức, công tác chuyên môn.', 20),
    ('CHI_THI',          'CT',   'Chỉ thị',             'Văn bản chỉ đạo, điều hành của cấp trên.', 30),
    ('QUY_CHE',          'QC',   'Quy chế',             'Quy định nguyên tắc, thủ tục hoạt động của đơn vị.', 40),
    ('QUY_DINH',         'QĐ',   'Quy định',            'Quy định chi tiết về một vấn đề cụ thể.', 50),
    ('THONG_CAO',        'TC',   'Thông cáo',           'Thông tin chính thức công khai ra công chúng.', 60),
    ('THONG_BAO',        'TB',   'Thông báo',           'Thông tin thông thường gửi đến đối tượng cụ thể.', 70),
    ('HUONG_DAN',        'HD',   'Hướng dẫn',           'Hướng dẫn thực hiện văn bản cấp trên.', 80),
    ('CHUONG_TRINH',     'CTr',  'Chương trình',        'Kế hoạch tổng thể các hoạt động theo mục tiêu.', 90),
    ('KE_HOACH',         'KH',   'Kế hoạch',            'Kế hoạch thực hiện nhiệm vụ theo thời gian.', 100),
    ('PHUONG_AN',        'PA',   'Phương án',           'Các giải pháp triển khai cho một vấn đề cụ thể.', 110),
    ('DE_AN',            'ĐA',   'Đề án',               'Đề xuất một vấn đề/dự án để phê duyệt.', 120),
    ('DU_AN',            'DA',   'Dự án',               'Các công trình, đầu tư có mục tiêu rõ ràng.', 130),
    ('BAO_CAO',          'BC',   'Báo cáo',             'Văn bản tổng hợp tình hình, kết quả công tác.', 140),
    ('BIEN_BAN',         'BB',   'Biên bản',            'Ghi nhận diễn biến sự việc, cuộc họp.', 150),
    ('TO_TRINH',         'TTr',  'Tờ trình',            'Đề xuất cấp có thẩm quyền phê duyệt.', 160),
    ('HOP_DONG',         'HĐ',   'Hợp đồng',            'Thỏa thuận giữa các bên có tính ràng buộc pháp lý.', 170),
    ('CONG_DIEN',        'CĐ',   'Công điện',           'Văn bản chỉ đạo khẩn cấp, thường qua điện tín.', 180),
    ('BAN_GHI_NHO',      'BGN',  'Bản ghi nhớ',         'Ghi lại nội dung thỏa thuận không ràng buộc pháp lý.', 190),
    ('BAN_THOA_THUAN',   'BTT',  'Bản thỏa thuận',      'Thỏa thuận giữa các bên về hợp tác.', 200),
    ('GIAY_UY_QUYEN',    'GUQ',  'Giấy ủy quyền',       'Ủy quyền cho người khác thực hiện công việc.', 210),
    ('GIAY_MOI',         'GM',   'Giấy mời',            'Mời tham dự sự kiện, cuộc họp.', 220),
    ('GIAY_GIOI_THIEU',  'GGT',  'Giấy giới thiệu',     'Giới thiệu người đại diện công tác.', 230),
    ('GIAY_NGHI_PHEP',   'GNP',  'Giấy nghỉ phép',      'Xin nghỉ phép công tác.', 240),
    ('PHIEU_GUI',        'PG',   'Phiếu gửi',           'Phiếu gửi công văn/tài liệu.', 250),
    ('PHIEU_CHUYEN',     'PC',   'Phiếu chuyển',        'Phiếu chuyển văn bản đến đơn vị xử lý.', 260),
    ('PHIEU_BAO',        'PB',   'Phiếu báo',           'Phiếu thông báo nội dung cần xử lý.', 270),
    ('THU_CONG',         'TCg',  'Thư công',            'Thư trao đổi công việc giữa các cơ quan.', 280),
    ('CONG_VAN',         'CV',   'Công văn',            'Công văn thường — loại VB phổ biến nhất.', 290);
