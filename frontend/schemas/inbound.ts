import { z } from 'zod';

export const inboundCreateSchema = z.object({
  documentTypeId: z.string().uuid({ message: 'Chọn loại văn bản.' }),
  confidentialityLevelId: z.string().uuid({ message: 'Chọn mức mật.' }),
  priorityLevelId: z.string().uuid({ message: 'Chọn mức khẩn.' }),
  subject: z.string().min(5, 'Trích yếu phải từ 5 ký tự.').max(1000),
  summary: z.string().max(5000).optional(),
  bookId: z.string().uuid({ message: 'Chọn sổ đăng ký.' }),
  organizationId: z.string().uuid(),
  departmentId: z.string().uuid().optional().or(z.literal('')),
  receivedDate: z.string().optional(),
  receivedFromChannel: z
    .enum(['POST', 'EMAIL', 'SCAN', 'HAND_DELIVERED', 'OTHER'])
    .optional(),
  externalReferenceNumber: z.string().max(100).optional(),
  externalIssuer: z.string().max(500).optional(),
  externalIssuedDate: z.string().optional(),
  dueDate: z.string().optional(),
});

export type InboundCreateValues = z.infer<typeof inboundCreateSchema>;
