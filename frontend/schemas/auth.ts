import { z } from 'zod';

export const loginSchema = z.object({
  username: z
    .string({ required_error: 'Vui lòng nhập tên đăng nhập.' })
    .min(3, 'Tên đăng nhập phải từ 3 ký tự.')
    .max(100),
  password: z
    .string({ required_error: 'Vui lòng nhập mật khẩu.' })
    .min(1, 'Vui lòng nhập mật khẩu.')
    .max(200),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
