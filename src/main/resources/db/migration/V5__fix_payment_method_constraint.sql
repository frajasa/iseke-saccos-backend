-- Fix transactions_payment_method_check to include mobile payment provider types
ALTER TABLE public.transactions DROP CONSTRAINT IF EXISTS transactions_payment_method_check;
ALTER TABLE public.transactions ADD CONSTRAINT transactions_payment_method_check
  CHECK (payment_method::text = ANY (ARRAY['CASH','CHEQUE','BANK_TRANSFER','MOBILE_MONEY','CARD','MPESA','TIGOPESA','NMB_BANK']::text[]));

-- Expand failure_reason column to accommodate longer error messages
ALTER TABLE public.payment_requests ALTER COLUMN failure_reason TYPE VARCHAR(2000);
