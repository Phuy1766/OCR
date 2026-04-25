'use client';

import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface PaginationProps {
  page: number;        // 0-indexed
  totalElements: number;
  size: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export function Pagination({
  page,
  totalElements,
  size,
  onPageChange,
  className,
}: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  const start = page * size + 1;
  const end = Math.min(start + size - 1, totalElements);

  if (totalElements === 0) return null;

  return (
    <div className={cn('flex items-center justify-between gap-2 text-sm', className)}>
      <div className="text-muted-foreground">
        {totalElements > 0 ? (
          <>
            {start}-{end} / {totalElements}
          </>
        ) : (
          'Không có dữ liệu'
        )}
      </div>
      <div className="flex items-center gap-1">
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => onPageChange(Math.max(0, page - 1))}
          disabled={page === 0}
          aria-label="Trang trước"
        >
          <ChevronLeft className="h-3.5 w-3.5" />
        </Button>
        <span className="px-2 text-xs text-muted-foreground">
          Trang {page + 1} / {totalPages}
        </span>
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
          disabled={page >= totalPages - 1}
          aria-label="Trang sau"
        >
          <ChevronRight className="h-3.5 w-3.5" />
        </Button>
      </div>
    </div>
  );
}
