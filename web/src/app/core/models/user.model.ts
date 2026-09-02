import { z } from 'zod';

// Schemas
export const userSchema = z.object({
  id: z.string().optional(),
  username: z.string(),
  name: z.string(),
  email: z.string().email(),
});

export const authRequestSchema = z.object({
  username: z.string(),
  password: z.string(),
});

export const signUpRequestSchema = z.object({
  user: z.object({
    email: z.string().email(),
  }),
  password: z.string(),
  passwordRepeat: z.string(),
});

export const forgotPasswordRequestSchema = z.object({
  email: z.string().email(),
});

export const resetPasswordRequestSchema = z.object({
  password: z.string(),
  passwordRepeat: z.string(),
  token: z.string().optional(),
  user: z.string().optional(),
});

export const authSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  user: userSchema,
});

// Types
export type User = z.infer<typeof userSchema>;
export type AuthRequest = z.infer<typeof authRequestSchema>;
export type SignUpRequest = z.infer<typeof signUpRequestSchema>;
export type ForgotPasswordRequest = z.infer<typeof forgotPasswordRequestSchema>;
export type ResetPasswordRequest = z.infer<typeof resetPasswordRequestSchema>;
export type Auth = z.infer<typeof authSchema>;
