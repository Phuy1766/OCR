'use client';

import { useRef, useState } from 'react';
import { File as FileIcon, Upload, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';

interface FileDropzoneProps {
  files: File[];
  onChange: (files: File[]) => void;
  maxSizeBytes?: number;
  acceptMime?: string[];
}

const DEFAULT_MIME = [
  'application/pdf',
  'image/jpeg',
  'image/png',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
];

const DEFAULT_MAX = 50 * 1024 * 1024;

export function FileDropzone({
  files,
  onChange,
  maxSizeBytes = DEFAULT_MAX,
  acceptMime = DEFAULT_MIME,
}: FileDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleSelect = (selected: FileList | null) => {
    if (!selected) return;
    setError(null);
    const newFiles: File[] = [];
    for (const f of Array.from(selected)) {
      if (f.size > maxSizeBytes) {
        setError(`File "${f.name}" vượt quá 50MB.`);
        return;
      }
      if (!acceptMime.includes(f.type)) {
        setError(`Định dạng "${f.type || f.name}" không được phép.`);
        return;
      }
      newFiles.push(f);
    }
    onChange([...files, ...newFiles]);
  };

  return (
    <div className="space-y-2">
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={(e) => {
          e.preventDefault();
          setIsDragging(false);
          handleSelect(e.dataTransfer.files);
        }}
        onClick={() => inputRef.current?.click()}
        className={cn(
          'flex cursor-pointer flex-col items-center gap-2 rounded-md border-2 border-dashed p-6 text-sm transition',
          isDragging ? 'border-primary bg-accent' : 'border-input hover:bg-muted/40',
        )}
      >
        <Upload className="h-6 w-6 text-muted-foreground" />
        <div className="text-center">
          <div className="font-medium">Kéo thả file hoặc bấm để chọn</div>
          <div className="text-xs text-muted-foreground">
            PDF / JPG / PNG / DOC(X) / XLS(X) — tối đa 50MB mỗi file
          </div>
        </div>
        <input
          ref={inputRef}
          type="file"
          multiple
          accept={acceptMime.join(',')}
          className="hidden"
          onChange={(e) => handleSelect(e.target.files)}
        />
      </div>

      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-2 text-xs text-destructive">
          {error}
        </div>
      )}

      {files.length > 0 && (
        <ul className="space-y-1.5">
          {files.map((f, idx) => (
            <li
              key={`${f.name}-${idx}`}
              className="flex items-center gap-2 rounded-md border bg-background p-2 text-sm"
            >
              <FileIcon className="h-4 w-4 text-muted-foreground" />
              <span className="flex-1 truncate">{f.name}</span>
              <span className="text-xs text-muted-foreground">
                {(f.size / 1024).toFixed(0)} KB
              </span>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="h-6 w-6"
                onClick={(e) => {
                  e.stopPropagation();
                  onChange(files.filter((_, i) => i !== idx));
                }}
              >
                <X className="h-3.5 w-3.5" />
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
